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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
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
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
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

    private lateinit var map: MapView
    private lateinit var searchBox: MaterialAutoCompleteTextView
    private var marker: Marker? = null
    private var ongoingCall: okhttp3.Call? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var searchBlocked = false
    private var suggestions: List<Suggestion> = emptyList()
    private var targetPosition by mutableStateOf<Position>(Position(latitude = 40.0, longitude = -1.0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

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
        setupSearchBox()

        findViewById<ComposeView>(R.id.migrated_components).setContent {
            val cameraState =
                rememberCameraState(
                    CameraPosition(
                        target = targetPosition,
                        zoom = 15.0,
                    ),
                )

            LaunchedEffect(targetPosition) {
                log("Updating target $targetPosition")
                cameraState.animateTo(
                    finalPosition =
                        cameraState.position.copy(
                            target = targetPosition,
                        ),
                    duration = 500.milliseconds,
                )
            }

            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                //baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/positron"),
                baseStyle =
                    BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                cameraState = cameraState,
                options =
                    MapOptions(
                        gestureOptions =
                            GestureOptions(
                                isTiltEnabled = true,
                                isZoomEnabled = true,
                                isRotateEnabled = false,
                                isScrollEnabled = true,
                            ),
                    ),
                onMapClick = { pos, offset ->
                    moveToLocation(pos.latitude, pos.longitude)
                    reverseGeocode(org.osmdroid.util.GeoPoint(pos.latitude, pos.longitude))
                    ClickResult.Consume
                },
            ) {
                val points by derivedStateOf {
                    FeatureCollection(
                        listOf(
                            Feature(
                                geometry = Point(targetPosition),
                                properties = kotlinx.serialization.json.JsonObject(emptyMap()),
                            ),
                        ),
                    )
                }
                val source = rememberGeoJsonSource(data = GeoJsonData.Features(points))
                SymbolLayer(
                    id = "click-markers",
                    source = source,
                    //iconImage = image("marker"), // TODO: Change it
                    iconImage = image(painterResource(R.drawable.ic_emoji_people)), // TODO: Change it
                    iconSize = const(2f),
                    iconAnchor = const(org.maplibre.compose.expressions.value.SymbolAnchor.Bottom),
                )
            }
        }

        if (savedInstanceState != null) {
            setStateFromSavedInstance(savedInstanceState)
        } else {
            setStateFromIntent(intent)
        }
    }

    private fun setupMap() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val controller: IMapController = map.controller
        controller.setZoom(15.0)

        val personIcon =
            ResourcesCompat.getDrawable(
                resources,
                org.osmdroid.library.R.drawable.person,
                theme,
            )

        marker =
            Marker(map).apply {
                icon = personIcon
                isDraggable = true
                setOnMarkerDragListener(
                    object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker?) {}

                        override fun onMarkerDragStart(marker: Marker?) {}

                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let {
                                moveToLocation(it.latitude, it.longitude)
                                reverseGeocode(it)
                            }
                        }
                    },
                )
            }
        map.overlays.add(marker)
        map.invalidate()

        val mapEventsReceiver =
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        moveToLocation(it.latitude, it.longitude)
                        reverseGeocode(it)
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }
        map.overlays.add(MapEventsOverlay(mapEventsReceiver))
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
        marker?.position?.let { pos ->
            outState.putDouble(EXTRA_CURRENT_LAT, pos.latitude)
            outState.putDouble(EXTRA_CURRENT_LON, pos.longitude)
        }
    }

    private fun setStateFromSavedInstance(inState: Bundle) {
        moveToLocation(
            inState.getDouble(EXTRA_CURRENT_LAT, 40.0),
            inState.getDouble(EXTRA_CURRENT_LON, -1.0),
        )
        val initDescription = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        updateSearchText(initDescription)
    }

    private fun setStateFromIntent(intent: Intent) {
        moveToLocation(
            intent.getDoubleExtra(EXTRA_CURRENT_LAT, 40.0),
            intent.getDoubleExtra(EXTRA_CURRENT_LON, -1.0),
        )
        val initDescription = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        updateSearchText(initDescription)
    }

    private fun returnResult() {
        marker?.position?.let { pos ->
            val intent =
                Intent().apply {
                    putExtra(EXTRA_SELECTED_LAT, pos.latitude)
                    putExtra(EXTRA_SELECTED_LON, pos.longitude)
                }
            setResult(RESULT_OK, intent)
        }
        finish()
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
        val point = GeoPoint(lat, lon)
        marker?.position = point
        map.controller.animateTo(point)
        targetPosition = Position(latitude = lat, longitude = lon)
        log("Updating target $targetPosition")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        // ← back arrow in ActionBar
        if (item.itemId == android.R.id.home) {
            returnResult()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    override fun onResume() {
        super.onResume()
        // avoids map leaks
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        // avoids map leaks
        map.onPause()
    }
}
