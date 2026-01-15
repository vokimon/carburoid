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
import net.canvoki.carburoid.ui.StationDetailActivity
import net.canvoki.carburoid.ui.openActivity

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

        val startLocation =
            locationFromSavedInstance(savedInstanceState)
                ?: locationFromDeepLinkIntent(intent)
        startLocation?.let {
            app.locationService.setFixedLocation(it)
        }

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

    /**
     * Returns location retrieved from activity status after pause/stop
     * if available, else returns null.
     */
    private fun locationFromSavedInstance(savedInstanceState: Bundle?): Location? {
        if (savedInstanceState == null) return null
        return app.locationService.getSavedLocation() ?: return null
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
        location?.let { app.locationService.setFixedLocation(it) }
    }
}
