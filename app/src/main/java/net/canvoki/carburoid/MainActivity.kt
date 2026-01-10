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
import net.canvoki.carburoid.product.ProductSelection
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.ui.StationListView
import net.canvoki.carburoid.ui.openActivity
import net.canvoki.carburoid.ui.setContentViewWithInsets

class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_SELECT_PRODUCT = "net.canvoki.carburoid.ACTION_SELECT_PRODUCT"
        const val EXTRA_PRODUCT = "net.canvoki.carburoid.EXTRA_PRODUCT"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_STATION_ID = "station_id"
    }

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private val viewModel: MainSharedViewModel by lazy {
        ViewModelProvider(this).get(MainSharedViewModel::class.java)
    }

    private lateinit var locationService: LocationService
    private lateinit var stationList: StationListView

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")

        super.onCreate(savedInstanceState)

        setContentViewWithInsets(R.layout.activity_main)

        stationList = findViewById(R.id.station_list)

        stationList.stations = emptyList()
        stationList.onStationClicked = { station ->
            openActivity<StationDetailActivity> {
                putExtra(EXTRA_STATION_ID, station.id)
            }
        }
        stationList.onRefresh = {
            stationList.isDownloading = true
            repository.launchFetch()
        }

        lifecycleScope.launch {
            viewModel.stationsReloadStarted.collect {
                stationList.isProcessing = true
            }
        }

        lifecycleScope.launch {
            viewModel.stationsUpdated.collect { stations ->
                timeits("UPDATING CONTENT") {
                    stationList.stations = stations
                    stationList.isProcessing = false
                }
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
        handleExternalProductIntent(intent)
    }

    private fun handleExternalProductIntent(intent: Intent): Boolean {
        if (intent.action != ACTION_SELECT_PRODUCT) return false

        val requestedProduct = intent.getStringExtra(EXTRA_PRODUCT)
        if (requestedProduct == null) return false
        val selection = ProductSelection(this)

        val availableProducts = selection.choices()
        if (availableProducts.contains(requestedProduct)) {
            selection.setCurrent(requestedProduct)
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
        val isFetching = repository.isFetchInProgress()
        stationList.isDownloading = isFetching
    }

    /**
     * Retrieves location from activity status after pause/stop
     * if available, else returns false.
     */
    private fun useSavedLocation(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState == null) return false
        val saved = locationService.getSavedLocation() ?: return false
        locationService.setFixedLocation(saved)
        return true
    }

    /**
     * Retrieves location from incoming Deep Link Intent,
     * if available, else returns false.
     */
    private fun useDeepLinkIntentLocation(intent: Intent?): Boolean {
        val location =
            intent?.let {
                IntentCompat.getParcelableExtra(it, MainActivity.EXTRA_LOCATION, Location::class.java)
            } ?: return false

        locationService.setFixedLocation(location)
        return true
    }

    /**
     * Retrieves location asynchronously from location services,
     * meanwhile it uses last location in settings or a fallback.
     */
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
            R.id.action_settings -> openActivity<SettingsActivity>()
            R.id.action_chart -> openActivity<PlotNavigatorActivity>()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService.processPermission(requestCode, grantResults)
    }
}
