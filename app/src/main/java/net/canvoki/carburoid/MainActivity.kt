package net.canvoki.carburoid

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
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
import net.canvoki.carburoid.plotnavigator.PlotNavigatorActivity
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.ui.GasStationAdapter
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.ui.StationListView
import net.canvoki.carburoid.ui.setContentViewWithInsets

class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_SELECT_PRODUCT = "net.canvoki.carburoid.ACTION_SELECT_PRODUCT"
        const val EXTRA_PRODUCT = "net.canvoki.carburoid.EXTRA_PRODUCT"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_SOURCE = "source"
    }

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private val viewModel: MainSharedViewModel by lazy {
        ViewModelProvider(this).get(MainSharedViewModel::class.java)
    }

    private lateinit var locationService: LocationService
    private lateinit var spinner: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var progressText: TextView
    private lateinit var loadingPill: LinearLayout
    private lateinit var stationList: StationListView

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")

        super.onCreate(savedInstanceState)

        setContentViewWithInsets(R.layout.activity_main)

        spinner = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.text_empty)
        progressText = findViewById(R.id.text_progress)
        loadingPill = findViewById(R.id.loading_pill)
        stationList = findViewById(R.id.station_list)

        stationList.stations = emptyList()
        stationList.onStationClicked = ::openDetails
        stationList.onRefresh = {
            stationList.isRefreshing = true
            repository.launchFetch()
        }

        showEmpty(getString(R.string.no_gas_stations))

        lifecycleScope.launch {
            viewModel.stationsReloadStarted.collect {
                showProgress(getString(R.string.refreshing_data))
            }
        }

        lifecycleScope.launch {
            viewModel.stationsUpdated.collect { stations ->
                onStationsUpdated(stations)
            }
        }

        var locationSelector = findViewById<LocationSelector>(R.id.location_selector)
        locationService =
            LocationService(
                this,
                notify = ::showToast,
                suggestAction = ::suggestAction,
            )
        locationSelector.bind(this, locationService)

        lifecycleScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is RepositoryEvent.UpdateStarted -> {
                        updateLoadingDataStatus()
                        nolog("EVENT UpdateStarted")
                    }
                    is RepositoryEvent.UpdateReady -> {
                        updateLoadingDataStatus()
                        nolog("EVENT UpdateReady")
                    }
                    is RepositoryEvent.UpdateFailed -> {
                        updateLoadingDataStatus()
                        nolog("EVENT UpdateFailed")
                        showToast(getString(R.string.failed_download, event.error))
                    }
                }
            }
        }
        (
            useSavedLocation(savedInstanceState) ||
                useDeepLinkIntentLocation(intent) ||
                useDeviceLocation()
        )
    }

    private fun handleExternalProductIntent(intent: Intent): Boolean {
        if (intent.action != ACTION_SELECT_PRODUCT) return false

        val requestedProduct = intent.getStringExtra(EXTRA_PRODUCT)
        if (requestedProduct == null) return false

        val availableProducts = ProductManager.available()
        if (availableProducts.contains(requestedProduct)) {
            ProductManager.setCurrent(requestedProduct)
            return true
        }
        log("Bad product '$requestedProduct' received as intent, available products: $availableProducts")
        return false
    }

    override fun onStart() {
        super.onStart()
        updateLoadingDataStatus()
    }

    fun updateLoadingDataStatus() {
        val isUpdating = repository.isFetchInProgress()
        loadingPill.visibility = if (isUpdating) View.VISIBLE else View.GONE
        stationList.isRefreshing = isUpdating
    }

    private fun useSavedLocation(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState == null) return false
        val saved = locationService.getSavedLocation() ?: return false
        locationService.setFixedLocation(saved)
        return true
    }

    private fun useDeepLinkIntentLocation(intent: Intent?): Boolean {
        val location =
            intent?.let {
                IntentCompat.getParcelableExtra(it, MainActivity.EXTRA_LOCATION, Location::class.java)
            } ?: return false

        locationService.setFixedLocation(location)
        return true
    }

    private fun useDeviceLocation(): Boolean {
        locationService.refreshLocation()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_chart -> {
                startActivity(Intent(this, PlotNavigatorActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleExternalProductIntent(intent)) return
        useDeepLinkIntentLocation(intent)
    }

    private fun showToast(message: String) {
        log(message)
        val snackbar =
            Snackbar.make(
                findViewById<ViewGroup>(android.R.id.content),
                message,
                Snackbar.LENGTH_LONG,
            )
        snackbar.show()
    }

    private fun suggestAction(
        message: String,
        actionText: String,
        action: () -> Unit,
    ) {
        log(message)
        val snackbar =
            Snackbar
                .make(
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
                stationList.visibility = View.GONE
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
                stationList.visibility = View.GONE
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
                stationList.visibility = View.VISIBLE
                spinner.visibility = View.GONE
                progressText.visibility = View.GONE
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun loadGasStations() {
        onStationsReloadStarted()
        lifecycleScope.launch {
            val stations = viewModel.getStationsToDisplay()
            onStationsUpdated(stations)
        }
    }

    private fun onStationsReloadStarted() {
        showProgress(getString(R.string.refreshing_data))
    }

    private fun onStationsUpdated(stations: List<GasStation>) {
        timeits("UPDATING CONTENT") {
            // TODO: Move empty label inside the station list and manage this internally
            if (stations.isEmpty()) {
                showEmpty(getString(R.string.no_gas_stations))
            } else {
                stationList.stations = stations
                showContent()
            }
        }
    }

    private fun openDetails(station: GasStation) {
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
