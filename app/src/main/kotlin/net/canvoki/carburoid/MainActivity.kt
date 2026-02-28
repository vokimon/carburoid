package net.canvoki.carburoid

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import net.canvoki.carburoid.plotnavigator.PlotNavigatorActivity
import net.canvoki.carburoid.product.ProductSelection
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.ui.SettingsActivity
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.shared.component.openActivity
import net.canvoki.shared.log

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

    override fun onCreate(savedInstanceState: Bundle?) {
        log("onCreate")

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setInitialLocation(savedInstanceState)

        handleExternalProductIntent(intent)

        setContent {
            StationListScreen(
                viewModel = viewModel,
                repository = repository,
                onStationClicked = { station ->
                    openActivity<StationDetailActivity> {
                        putExtra(EXTRA_STATION_ID, station.id)
                    }
                },
                onPlotNavigatorClick = { openActivity<PlotNavigatorActivity>() },
                onSettingsClick = { openActivity<SettingsActivity>() },
            )
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

    private fun setInitialLocation(saved: Bundle?) {
        val (current, target) =
            locationFromSavedInstance(saved)
                ?: locationFromDeepLinkIntent(intent) ?: return
        log("Setting Initial position $current -> $target")
        app.locationService.setFixedLocation(current, target)
    }

    /**
     * Returns location retrieved from activity status after pause/stop
     * if available, else returns null.
     */
    private fun locationFromSavedInstance(saved: Bundle?): Pair<Location, Location?>? {
        if (saved == null) return null
        // Not really from the state but from the preferences
        // Using the saved instance just to know we must recover it
        return app.locationService.loadLastLocation()
    }

    /**
     * Returns location retrieved from incoming Deep Link Intent,
     * if available, else returns null.
     */
    private fun locationFromDeepLinkIntent(intent: Intent?): Pair<Location, Location?>? {
        intent ?: return null
        val current =
            IntentCompat.getParcelableExtra(intent, MainActivity.EXTRA_LOCATION, Location::class.java) ?: return null
        val target = IntentCompat.getParcelableExtra(intent, MainActivity.EXTRA_LOCATION, Location::class.java)

        return current to target
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
        val (current, target) = locationFromDeepLinkIntent(intent) ?: return
        app.locationService.setFixedLocation(current, target)
    }
}
