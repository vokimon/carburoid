package net.canvoki.carburoid

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.location.Location
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.distances.DistanceFromCurrentPosition
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.GasStationResponse
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.network.GasStationApiFactory
import net.canvoki.carburoid.ui.GasStationAdapter
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.product.ProductSpinner


class MainActivity : AppCompatActivity() {

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var productSpinner: ProductSpinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var progressText: TextView
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var gasStationAdapter: GasStationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val productSpinner = findViewById<ProductSpinner>(R.id.product_spinner)
        productSpinner.setOnProductSelectedListener { selectedProduct ->
            loadGasStations()
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinner = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.text_empty)
        progressText = findViewById(R.id.text_progress)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        gasStationAdapter = GasStationAdapter(this, emptyList(), ::onItemClicked)
        recyclerView.adapter = gasStationAdapter

        showEmpty("No stations")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                repository.launchFetch() // Triggers background fetch if needed
            }
        }

        lifecycleScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.UpdateStarted -> {
                        // Show loading, disable refresh, etc.
                        //showLoading()
                        swipeRefreshLayout.isRefreshing = true
                        log("EVENT UpdateStarted")
                    }
                    is RepositoryEvent.UpdateReady -> {
                        swipeRefreshLayout.isRefreshing = false
                        showProgress("Processing Data...")
                        log("EVENT UpdateReady")
                        loadGasStations()
                    }
                    is RepositoryEvent.UpdateFailed -> {
                        swipeRefreshLayout.isRefreshing = false
                        log("EVENT UpdateFailed")
                        showToast(event.error)
                    }
                }
            }
        }

        lifecycleScope.launch {
            FilterSettings.changes.collect {
                log("EVENT Filter updated")
                loadGasStations()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun setFallbackLocation() {
        val madrid = Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        CurrentDistancePolicy.setMethod(DistanceFromAddress(madrid))
    }

    private fun showToast(message: String) {
        log(message)
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun showEmpty(message: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                recyclerView.visibility = View.GONE
                spinner.visibility = View.GONE
                progressText.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = message
            }
        }
    }

    private fun showProgress(message: String?) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                recyclerView.visibility = View.GONE
                spinner.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                progressText.text = message ?: "---"
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun showContent() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                recyclerView.visibility = View.VISIBLE
                spinner.visibility = View.GONE
                progressText.visibility = View.GONE
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun getLastLocation() {
        if (!checkLocationPermission()) {
            setFallbackLocation() // ✅ Set fallback if permission not granted
            loadGasStations()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    CurrentDistancePolicy.setMethod(DistanceFromCurrentPosition(location))
                } else {
                    setFallbackLocation()
                    showToast("Location not available — using Madrid")
                }
                loadGasStations()
            }
            .addOnFailureListener {
                setFallbackLocation()
                showToast("Failed to get location — using Madrid")
            }
    }


    private fun loadGasStations() {
        lifecycleScope.launch {
            val config = FilterSettings.config(this@MainActivity)
            try {
                showProgress("Refreshing data...")

                // 🚧 Do heavy work in IO (or Default) dispatcher
                val stations = repository.getData()?.stations ?: emptyList()
                val sortedStations = timeit("PROCESSING STATIONS") {
                    StationFilter(config).filter(stations)
                }

                timeit("UPDATING CONTENT") {
                    if (sortedStations.isEmpty()) {
                        showEmpty("No stations")
                    } else {
                        gasStationAdapter.updateData(sortedStations)
                        showContent()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Download failed: ${e.message}")
                    showEmpty("Error loading stations: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    private fun onItemClicked(station: GasStation) {
        val intent = Intent(this, StationDetailActivity::class.java)
        intent.putExtra("station_id", station.id)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                setFallbackLocation()
                showToast("Location permission denied — using Madrid")
                loadGasStations()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private var currentLocation: Location? = null
    }
}
