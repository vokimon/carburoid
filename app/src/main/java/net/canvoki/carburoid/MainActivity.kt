package net.canvoki.carburoid

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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

class MainActivity : ComponentActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        locationService = LocationService(this)
        val startLocation =
            locationFromSavedInstance(savedInstanceState)
                ?: locationFromDeepLinkIntent(intent)
        startLocation?.let {
            locationService.setFixedLocation(it)
        }

        handleExternalProductIntent(intent)

        val activity = this
        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            val viewModel = this@MainActivity.viewModel
            val repository = this@MainActivity.repository

            var isDownloading by remember { mutableStateOf(repository.isFetchInProgress()) }
            var isProcessing by remember { mutableStateOf(viewModel.isProcessingStations) }
            var stations by remember { mutableStateOf<List<GasStation>>(viewModel.getStationsToDisplay()) }

            LaunchedEffect(repository) {
                repository.events.collect { event ->
                    isDownloading = repository.isFetchInProgress()
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

            LaunchedEffect(viewModel) {
                viewModel.stationsReloadStarted.collect {
                    isProcessing = true
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.stationsUpdated.collect { updatedStations ->
                    stations = updatedStations
                    isProcessing = false
                }
            }

            LaunchedEffect(startLocation, locationService) {
                startLocation?.let {
                    locationService.setFixedLocation(it)
                }
            }

            net.canvoki.carburoid.ui.AppScaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Carburoid") },
                        actions = {
                            IconButton(
                                onClick = { openActivity<PlotNavigatorActivity>() },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_show_chart),
                                    contentDescription = stringResource(R.string.menu_chart),
                                )
                            }
                            IconButton(
                                onClick = { openActivity<SettingsActivity>() },
                            ) {
                                Icon(
                                    contentDescription = stringResource(R.string.menu_settings),
                                    painter = painterResource(R.drawable.ic_settings),
                                )
                            }
                        },
                    )
                },
            ) {
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

    /**
     * Returns location retrieved from activity status after pause/stop
     * if available, else returns null.
     */
    private fun locationFromSavedInstance(savedInstanceState: Bundle?): Location? {
        if (savedInstanceState == null) return null
        return locationService.getSavedLocation() ?: return null
    }

    /**
     * Returns location retrieved from incoming Deep Link Intent,
     * if available, else returns null.
     */
    private fun locationFromDeepLinkIntent(intent: Intent?): Location? {
        val location =
            intent?.let {
                IntentCompat.getParcelableExtra(it, MainActivity.EXTRA_LOCATION, Location::class.java)
            } ?: return null

        return location
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
        val location = locationFromDeepLinkIntent(intent)
        location?.let { locationService.setFixedLocation(it) }
    }
}
