package net.canvoki.carburoid.location

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.canvoki.carburoid.R
import net.canvoki.carburoid.network.Suggestion
import net.canvoki.carburoid.network.nameLocation
import net.canvoki.carburoid.ui.AppScaffold
import net.canvoki.shared.log
import org.maplibre.spatialk.geojson.Position

fun Position.pretty(): String = "(${ "%.3f".format(latitude) }, ${ "%.3f".format(longitude) })"

class LocationPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CURRENT_DESCRIPTION = "current_description"
        const val EXTRA_CURRENT_LAT = "current_lat"
        const val EXTRA_CURRENT_LON = "current_lon"
        const val EXTRA_TARGET_LAT = "target_lat"
        const val EXTRA_TARGET_LON = "target_lon"
        val FALLBACK_POSITION = Position(latitude = 40.0, longitude = -1.0)
    }

    private var currentDescription by mutableStateOf<String>("")
    private var currentPosition by mutableStateOf<Position>(FALLBACK_POSITION)
    private var targetDescription by mutableStateOf<String>("")
    private var targetPosition by mutableStateOf<Position?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            title = getString(R.string.location_picker_title)
            setDisplayHomeAsUpEnabled(true)
        }

        onBackPressedDispatcher.addCallback(this) {
            returnResult()
            finish()
        }

        setupMap()

        if (savedInstanceState != null) {
            setStateFromSavedInstance(savedInstanceState)
        } else {
            setStateFromIntent(intent)
        }
    }

    private fun setupMap() {
        setContent {
            AppScaffold {
                Column {
                    LocationSearch(
                        locationDescription = currentDescription,
                        onSuggestionSelected = { suggestion ->
                            currentDescription = suggestion.display
                            currentPosition = Position(latitude = suggestion.lat, longitude = suggestion.lon)
                        },
                    )
                    LocationPickerMap(
                        currentPosition = currentPosition,
                        targetPosition = targetPosition,
                        onCurrentPositionChanged = { pos ->
                            currentPosition = pos
                            currentDescription = "${pos.pretty()}"
                            lifecycleScope.launch {
                                nameLocation(pos)?.let { currentDescription = it }
                            }
                        },
                        onTargetPositionChanged = { pos ->
                            targetPosition = pos
                            targetDescription = "${pos?.pretty() ?: ""}"
                            pos?.let { pos ->
                                lifecycleScope.launch {
                                    nameLocation(pos)?.let { targetDescription = it }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log("MAP STATE TO SAVED INSTANCE  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
        positionToBundle(outState, LocationService.EXTRA_CURRENT_PREFIX, currentPosition)
        positionToBundle(outState, LocationService.EXTRA_TARGET_PREFIX, targetPosition)
    }

    private fun returnResult() {
        log("MAP STATE TO INTENT  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
        val intent = Intent()
        positionToIntent(intent, LocationService.EXTRA_CURRENT_PREFIX, currentPosition)
        positionToIntent(intent, LocationService.EXTRA_TARGET_PREFIX, targetPosition)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setStateFromSavedInstance(inState: Bundle) {
        currentPosition = positionFromBundle(inState, LocationService.EXTRA_CURRENT_PREFIX) ?: FALLBACK_POSITION
        targetPosition = positionFromBundle(inState, LocationService.EXTRA_TARGET_PREFIX)
        currentDescription = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        log("MAP STATE FROM SAVED INSTANCE  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
    }

    private fun setStateFromIntent(intent: Intent) {
        currentPosition = positionFromIntent(intent, LocationService.EXTRA_CURRENT_PREFIX) ?: FALLBACK_POSITION
        targetPosition = positionFromIntent(intent, LocationService.EXTRA_TARGET_PREFIX)
        currentDescription = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        log("MAP STATE FROM INTENT  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home) {
            // ← back arrow in ActionBar
            returnResult()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
}
