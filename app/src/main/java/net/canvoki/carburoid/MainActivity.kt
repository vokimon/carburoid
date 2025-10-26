package net.canvoki.carburoid

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.algorithms.StationFilter
import net.canvoki.carburoid.location.LocationSelector
import net.canvoki.carburoid.location.LocationService
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.ProductSelector
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.ui.GasStationAdapter
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.ui.settings.LanguageSettings

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOCATION = "location"
        const val EXTRA_SOURCE = "source"
    }

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var locationService: LocationService
    private lateinit var productSelector: ProductSelector
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var progressText: TextView
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var gasStationAdapter: GasStationAdapter

    private var language: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")
        super.onCreate(savedInstanceState)

        LanguageSettings.initializeLanguage(this)
        language = LanguageSettings.getApplicationLanguage()
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinner = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.text_empty)
        progressText = findViewById(R.id.text_progress)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        gasStationAdapter = GasStationAdapter(this, emptyList(), ::onItemClicked)
        recyclerView.adapter = gasStationAdapter

        showEmpty(getString(R.string.no_gas_stations))

        var locationSelector = findViewById<LocationSelector>(R.id.location_selector)
        locationService = LocationService(
            this,
            notify = ::showToast,
            suggestAction = ::suggestAction,
        )
        locationSelector.bind(this, locationService)
        lifecycleScope.launch {
            locationService.locationChanged.collect {
                loadGasStations()
            }
        }
        val productSelector = findViewById<ProductSelector>(R.id.product_selector)
        productSelector.setOnProductSelectedListener { selectedProduct ->
            loadGasStations()
        }

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                locationService.refreshLocation()
                repository.launchFetch() // Triggers background fetch if needed
            }
        }

        lifecycleScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.UpdateStarted -> {
                        swipeRefreshLayout.isRefreshing = true
                        nolog("EVENT UpdateStarted")
                    }
                    is RepositoryEvent.UpdateReady -> {
                        swipeRefreshLayout.isRefreshing = false
                        showProgress("Processing Data...")
                        nolog("EVENT UpdateReady")
                        loadGasStations()
                    }
                    is RepositoryEvent.UpdateFailed -> {
                        swipeRefreshLayout.isRefreshing = false
                        nolog("EVENT UpdateFailed")
                        showToast(event.error)
                    }
                }
            }
        }

        lifecycleScope.launch {
            FilterSettings.changes.collect {
                nolog("EVENT Filter updated")
                loadGasStations()
            }
        }

        locationService.refreshLocation()
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        val configuredLanguage = LanguageSettings.getApplicationLanguage()
        if (language != configuredLanguage) {
            recreate()
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.getParcelableExtra(MainActivity.EXTRA_LOCATION, Location::class.java)?.let { location ->
            locationService.setFixedLocation(location)
        }
    }

    private fun showToast(message: String) {
        log(message)
        val snackbar = Snackbar.make(
            findViewById<ViewGroup>(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG,
        )
        snackbar.show()
    }

    private fun suggestAction(message: String, actionText: String, action: () -> Unit) {
        log(message)
        val snackbar = Snackbar.make(
            findViewById<ViewGroup>(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG,
        ).setAction(actionText) {
            action()
        }
        snackbar.show()
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

    private fun loadGasStations() {
        lifecycleScope.launch {
            val config = FilterSettings.config(this@MainActivity)
            try {
                showProgress(getString(R.string.refreshing_data))

                // 🚧 Do heavy work in IO (or Default) dispatcher
                val stations = repository.getData()?.stations ?: emptyList()
                val sortedStations = timeit("PROCESSING STATIONS") {
                    StationFilter(config).filter(stations)
                }

                timeit("UPDATING CONTENT") {
                    if (sortedStations.isEmpty()) {
                        showEmpty(getString(R.string.no_gas_stations))
                    } else {
                        gasStationAdapter.updateData(sortedStations)
                        showContent()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.failed_download, e.message))
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
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService.processPermission(requestCode, grantResults)
    }
}
