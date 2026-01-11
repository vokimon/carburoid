package net.canvoki.carburoid

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.canvoki.carburoid.location.LocationSelector
import net.canvoki.carburoid.location.LocationService
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.plotnavigator.PlotNavigatorActivity
import net.canvoki.carburoid.product.CategorizedProductSelector
import net.canvoki.carburoid.product.ProductSelection
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.ui.openActivity
import net.canvoki.carburoid.ui.setContentViewWithInsets
import net.canvoki.carburoid.ui.usermessage.UserMessage

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

    private var stations by mutableStateOf<List<GasStation>>(emptyList())
    private var isProcessing by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")

        super.onCreate(savedInstanceState)

        setContentViewWithInsets(R.layout.activity_main)

        lifecycleScope.launch {
            repository.events.collect { event ->
                updateLoadingDataStatus()
                when (event) {
                    is RepositoryEvent.UpdateStarted -> {
                        nolog("REPO EVENT UpdateStarted")
                    }
                    is RepositoryEvent.UpdateReady -> {
                        nolog("REPO EVENT UpdateReady")
                    }
                    is RepositoryEvent.UpdateFailed -> {
                        nolog("REPO EVENT UpdateFailed")
                        UserMessage.Info(getString(R.string.failed_download, event.error)).post()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.stationsReloadStarted.collect {
                isProcessing = true
            }
        }

        lifecycleScope.launch {
            viewModel.stationsUpdated.collect { updatedStations ->
                stations = updatedStations
                isProcessing = false
            }
        }

        locationService = LocationService(this)

        val composeView = findViewById<ComposeView>(R.id.composable_view)
        val activity = this
        composeView.setContent {
            androidx.compose.material3.MaterialTheme(
                colorScheme =
                    net.canvoki.carburoid.ui.settings.ThemeSettings
                        .effectiveColorScheme(),
            ) {
                net.canvoki.carburoid.ui.AppScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CategorizedProductSelector(
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            LocationSelector(
                                activity = activity,
                                service = locationService,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        net.canvoki.carburoid.ui.StationList(
                            stations = stations,
                            downloading = isDownloading,
                            processing = isProcessing,
                            onRefresh = {
                                repository.launchFetch()
                            },
                            onStationClicked = { station ->
                                openActivity<StationDetailActivity> {
                                    putExtra(EXTRA_STATION_ID, station.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
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
        isDownloading = repository.isFetchInProgress()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService.processPermission(requestCode, grantResults)
    }
}
