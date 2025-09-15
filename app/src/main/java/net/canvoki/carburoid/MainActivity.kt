package net.canvoki.carburoid
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
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
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.CarburoidApplication

class MainActivity : AppCompatActivity() {

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinner = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.text_empty)
        progressText = findViewById(R.id.text_progress)

        showEmpty("No stations")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }

        lifecycleScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.UpdateStarted -> {
                        // Show loading, disable refresh, etc.
                        //showLoading()
                        log("EVENT UpdateStarted")
                    }
                    is RepositoryEvent.UpdateReady -> {
                        log("EVENT UpdateReady")
                        loadGasStations()
                    }
                    is RepositoryEvent.UpdateFailed -> {
                        log("EVENT UpdateFailed")
                        showToast(event.error)
                    }
                }
            }
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
                loadGasStations()
            }
    }

    private fun loadGasStations() {
        lifecycleScope.launch {
            try {
                showProgress("Refreshing data...")
                val stations = repository.getData()?.stations ?: emptyList()
                val sortedStations = StationFilter().filterParetoOptimal(stations)
                log("Final list: ${sortedStations.size} stations")

                if (sortedStations.isEmpty()) {
                    showEmpty("No stations")
                } else {
                    showContent()
                }

                val adapter = GasStationAdapter(sortedStations)
                recyclerView.adapter = adapter

            } catch (e: Exception) {
                showToast("Download failed: ${e.message}")
                e.printStackTrace()
                showEmpty("Error loading stations: ${e.message}")
            }
        }
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
