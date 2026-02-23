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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import net.canvoki.carburoid.FeatureFlags
import net.canvoki.carburoid.R
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

class LocationPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CURRENT_DESCRIPTION = "current_description"
        const val EXTRA_CURRENT_LAT = "current_lat"
        const val EXTRA_CURRENT_LON = "current_lon"
        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LON = "selected_lon"
    }

    private lateinit var searchBox: MaterialAutoCompleteTextView
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

        setupSearchBox()
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
                        text = targetDescription,
                        suggestions = suggestions,
                        onTextChanged = { updateSearchText(it) },
                        onPositionChanged = { lat, lon ->
                            currentPosition = Position(latitude = lat, longitude = lon)
                        },
                        onSearchQuery = { query ->
                            searchRunnable?.let { searchHandler.removeCallbacks(it) }
                            searchRunnable = Runnable { searchSuggestions(query) }
                            searchHandler.postDelayed(searchRunnable!!, 400)
                        },
                    )
                    LocationPickerMap(
                        currentPosition = currentPosition,
                        targetPosition = targetPosition,
                        onCurrentPositionChanged = { pos ->
                            currentPosition = pos
                            targetPosition = null
                            reverseGeocode(
                                GeoPoint(
                                    latitude = pos.latitude,
                                    longitude = pos.longitude,
                                ),
                            )
                        },
                        onTargetPositionChanged = { pos ->
                            targetPosition = pos
                        },
                    )
                }
            }
        }
    }

    private fun setupSearchBox() {
        searchBox = findViewById(R.id.searchBox)

        searchBox.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    if (searchBlocked) return
                    searchRunnable?.let { searchHandler.removeCallbacks(it) }
                    val query = s?.toString()?.trim() ?: ""
                    if (query.length < 3) return // avoid searching for tiny inputs

                    // Debounce: wait 400ms after last keystroke
                    searchRunnable = Runnable { searchSuggestions(query) }
                    searchHandler.postDelayed(searchRunnable!!, 400)
                }

                override fun afterTextChanged(s: Editable?) {}
            },
        )

        // Choosing a suggestion
        searchBox.setOnItemClickListener { _, _, position, _ ->
            val suggestion = suggestions[position]
            updateSearchText(suggestion.display)
            moveToLocation(suggestion.lat, suggestion.lon)
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

    private fun searchSuggestions(query: String) {
        runOnUiThread {
            val searchingAdapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    listOf(getString(R.string.location_picker_searching)),
                )
            searchBox.setAdapter(searchingAdapter)
            searchBox.showDropDown()
        }
        // TODO: change the country
        val url =
            "https://nominatim.openstreetmap.org/search?" +
                "format=json&q=${URLEncoder.encode(query, "UTF-8")}&limit=5&countrycodes=es"

        val client = OkHttpClient()
        val request =
            Request
                .Builder()
                .url(url)
                .header("User-Agent", packageName)
                .build()
        val call = client.newCall(request)
        ongoingCall = call

        Thread {
            try {
                val response = call.execute()
                val body = response.body.string()
                val array = JSONArray(body)

                val newSuggestions = mutableListOf<Suggestion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val display = obj.getString("display_name")
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    newSuggestions.add(Suggestion(display, lat, lon))
                }

                runOnUiThread {
                    suggestions = newSuggestions
                    val titles = newSuggestions.map { it.display }
                    val adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            titles,
                        )
                    searchBox.setAdapter(adapter)
                    searchBox.showDropDown()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun reverseGeocode(point: GeoPoint) {
        val url =
            "https://nominatim.openstreetmap.org/reverse?" +
                "format=json&lat=${point.latitude}&lon=${point.longitude}"

        Thread {
            try {
                val client = OkHttpClient()
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("User-Agent", packageName)
                        .build()
                val response = client.newCall(request).execute()
                val body = response.body.string()
                val json = JSONObject(body)
                val displayName = json.optString("display_name", "")

                runOnUiThread {
                    updateSearchText(displayName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun updateSearchText(newText: String) {
        targetDescription = newText
        searchBlocked = true

        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        ongoingCall?.cancel()

        val doFilter = false
        searchBox.setText(newText, doFilter)
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
        // ← back arrow in ActionBar
        if (item.itemId == android.R.id.home) {
            returnResult()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
}

@Composable
fun LocationSearch(
    text: String,
    onTextChanged: (String) -> Unit,
    onPositionChanged: (Double, Double) -> Unit,
    suggestions: List<Suggestion>,
    onSearchQuery: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf(text) }
    LaunchedEffect(text) {
        editingText = text
    }

    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = editingText,
            onValueChange = {
                editingText = it.trim()
                expanded = true
                if (editingText.length < 3) {
                    onSearchQuery(editingText)
                }
            },
            label = { Text(stringResource(R.string.location_picker_search_location_hint)) },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.location_picker_search_icon_description),
                )
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { editingText = "" }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_backspace),
                            contentDescription = "Clear text",
                        )
                    }
                }
            },
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach {
                DropdownMenuItem(
                    text = { Text(it.display) },
                    onClick = {
                        editingText = it.display
                        onTextChanged(it.display)
                        onPositionChanged(it.lat, it.lon)
                        expanded = false
                    },
                )
            }
        }
    }
}
