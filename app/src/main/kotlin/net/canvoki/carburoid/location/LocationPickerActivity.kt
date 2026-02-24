package net.canvoki.carburoid.location

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.canvoki.carburoid.FeatureFlags
import net.canvoki.carburoid.R
import net.canvoki.carburoid.network.Http
import net.canvoki.carburoid.ui.setContentViewWithInsets
import net.canvoki.shared.log
import net.canvoki.shared.settings.ThemeSettings
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.SymbolOverlap
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.material3.ScaleBar
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import java.net.URLEncoder
import kotlin.time.Duration.Companion.milliseconds

data class Suggestion(
    val display: String,
    val lat: Double,
    val lon: Double,
)

suspend fun searchLocation(query: String): List<Suggestion> {
    try {
        val response: String =
            Http.client
                .get("https://nominatim.openstreetmap.org/search") {
                    url {
                        parameters.append("limit", "10")
                        parameters.append("format", "json")
                        parameters.append("q", query)
                        parameters.append("countrycodes", "es") // TODO: Use current country
                    }
                }.body()
        val array = JSONArray(response)
        val newSuggestions = mutableListOf<Suggestion>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val display = obj.getString("display_name")
            val lat = obj.getDouble("lat")
            val lon = obj.getDouble("lon")
            newSuggestions.add(Suggestion(display, lat, lon))
        }
        return newSuggestions
    } catch (e: Exception) {
        log("ERROR while searching location by name '$query': $e")
        return emptyList()
    }
}

suspend fun nameLocation(position: Position): String? =
    try {
        val response: String =
            Http.client
                .get("https://nominatim.openstreetmap.org/reverse") {
                    url {
                        parameters.append("format", "json")
                        parameters.append("lat", position.latitude.toString())
                        parameters.append("lon", position.longitude.toString())
                    }
                }.body()

        val json = JSONObject(response)
        json.optString("display_name", "").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        log("nameLocation failed for $position: $e")
        null
    }

class LocationPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CURRENT_DESCRIPTION = "current_description"
        const val EXTRA_CURRENT_LAT = "current_lat"
        const val EXTRA_CURRENT_LON = "current_lon"
        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LON = "selected_lon"
    }

    private var ongoingCall: okhttp3.Call? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var searchBlocked = false
    private var targetDescription by mutableStateOf<String>("")
    private var suggestions by mutableStateOf<List<Suggestion>>(emptyList())
    private var currentPosition by mutableStateOf<Position>(Position(latitude = 40.0, longitude = -1.0))
    private var targetPosition by mutableStateOf<Position?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentViewWithInsets(R.layout.activity_location_picker)

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
        findViewById<ComposeView>(R.id.migrated_components).setContent {
            MaterialTheme(
                colorScheme = ThemeSettings.effectiveColorScheme(),
            ) {
                Column {
                    LocationSearch(
                        locationDescription = targetDescription,
                        onSuggestionSelected = { suggestion ->
                            updateSearchText(suggestion.display)
                            currentPosition = Position(latitude = suggestion.lat, longitude = suggestion.lon)
                        },
                    )
                    LocationPickerMap(
                        currentPosition = currentPosition,
                        targetPosition = targetPosition,
                        onCurrentPositionChanged = { pos ->
                            currentPosition = pos
                            targetPosition = null
                            targetDescription = "(${ "%.3f".format(pos.latitude) }, ${ "%.3f".format(pos.longitude) })"
                            lifecycleScope.launch {
                                nameLocation(pos)?.let { updateSearchText(it) }
                            }
                        },
                        onTargetPositionChanged = { pos ->
                            targetPosition = pos
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val lat = currentPosition.latitude
        val lon = currentPosition.longitude
        log("MAP STATE TO SAVED INSTANCE  $lat $lon")
        outState.putDouble(EXTRA_CURRENT_LAT, lat)
        outState.putDouble(EXTRA_CURRENT_LON, lon)
    }

    private fun returnResult() {
        val lat = currentPosition.latitude
        val lon = currentPosition.longitude
        log("MAP STATE TO INTENT  $lat $lon")
        val intent =
            Intent().apply {
                putExtra(EXTRA_SELECTED_LAT, lat)
                putExtra(EXTRA_SELECTED_LON, lon)
            }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setStateFromSavedInstance(inState: Bundle) {
        val lat = inState.getDouble(EXTRA_CURRENT_LAT, 40.0)
        val lon = inState.getDouble(EXTRA_CURRENT_LON, -1.0)
        val desc = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: "" // This comes from intent not saved!!
        log("MAP STATE FROM SAVED INSTANCE  $lat $lon $desc")
        updateSearchText(desc)
        moveToLocation(lat, lon)
    }

    private fun setStateFromIntent(intent: Intent) {
        val lat = intent.getDoubleExtra(EXTRA_CURRENT_LAT, 40.0)
        val lon = intent.getDoubleExtra(EXTRA_CURRENT_LON, 0.0)
        val desc = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        log("MAP STATE FROM INTENT  $lat $lon $desc")
        updateSearchText(desc)
        moveToLocation(lat, lon)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        searchBlocked = true
        super.onRestoreInstanceState(savedInstanceState)
        searchBlocked = false
    }

    private fun updateSearchText(newText: String) {
        searchBlocked = true
        targetDescription = newText
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        ongoingCall?.cancel()

        val doFilter = false
        searchBlocked = false
    }

    private fun moveToLocation(
        lat: Double,
        lon: Double,
    ) {
        currentPosition = Position(latitude = lat, longitude = lon)
        targetPosition = null
        //log("Updating target $currentPosition - $lat, $lon")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearch(
    locationDescription: String,
    onSuggestionSelected: (Suggestion) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf(locationDescription) }
    var userQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Sync external updates (map click, suggestion selection)
    LaunchedEffect(locationDescription) {
        if (locationDescription != editingText) {
            editingText = locationDescription
            expanded = false
            suggestions = emptyList()
            userQuery = "" // prevent triggering search
        }
    }

    LaunchedEffect(Unit) {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        snapshotFlow { userQuery }
            .debounce(300)
            .map { it.trim() }
            .filter { it.length > 2 }
            .distinctUntilChanged()
            .collectLatest { query ->
                try {
                    searching = true
                    expanded = true
                    suggestions = searchLocation(query)
                } finally {
                    searching = false
                }
            }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && (suggestions.isNotEmpty() || searching),
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = editingText,
            onValueChange = {
                editingText = it
                userQuery = it
                expanded = true
            },
            label = { Text(stringResource(R.string.location_picker_search_location_hint)) },
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.location_picker_search_icon_description),
                )
            },
            trailingIcon = {
                if (editingText.isNotEmpty()) {
                    IconButton(onClick = {
                        editingText = ""
                        focusRequester.requestFocus()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_backspace),
                            contentDescription = "Clear text", // TODO: Translatable
                        )
                    }
                }
            },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) dropdown@{
            if (searching || suggestions.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (searching) {
                                    R.string.location_picker_searching
                                } else {
                                    R.string.location_picker_no_results
                                },
                            ),
                        )
                    },
                    enabled = false,
                    onClick = {},
                )
                return@dropdown
            }
            suggestions.forEach {
                DropdownMenuItem(
                    text = { Text(it.display, maxLines = 1) },
                    onClick = {
                        expanded = false
                        editingText = it.display
                        suggestions = emptyList()
                        userQuery = ""
                        focusManager.moveFocus(FocusDirection.Next)
                        onSuggestionSelected(it)
                    },
                )
            }
        }
    }
}
