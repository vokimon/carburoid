package net.canvoki.carburoid.location

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.net.URLEncoder
import java.util.Locale
import net.canvoki.carburoid.R


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_location_picker)

        val initialLocation = GeoPoint(
            intent.getDoubleExtra(EXTRA_CURRENT_LAT, 40.0),
            intent.getDoubleExtra(EXTRA_CURRENT_LON, -1.0),
        )
        val initDescription = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""

        supportActionBar?.apply {
            title = getString(R.string.location_picker_title)
            setDisplayHomeAsUpEnabled(true)
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val controller: IMapController = map.controller
        controller.setZoom(15.0)
        controller.setCenter(initialLocation)

        val personIcon = ResourcesCompat.getDrawable(
            resources,
            org.osmdroid.library.R.drawable.person,
            theme
        )
        marker = Marker(map).apply {
            icon = personIcon
            position = initialLocation
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {}
                override fun onMarkerDragStart(marker: Marker?) {}
                override fun onMarkerDragEnd(marker: Marker?) {
                    marker?.position?.let {
                        map.controller.animateTo(it)
                        reverseGeocode(it)
                    }
                }
            })
        }
        map.overlays.add(marker)
        map.invalidate()

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    marker?.position = it
                    map.controller.animateTo(it)
                    reverseGeocode(it)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }

        map.overlays.add(MapEventsOverlay(mapEventsReceiver))

        searchBox = findViewById(R.id.searchBox)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (searchBlocked) return
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                if (query.length < 3) return  // avoid searching for tiny inputs

                // Debounce: wait 400ms after last keystroke
                searchRunnable = Runnable { searchSuggestions(query) }
                searchHandler.postDelayed(searchRunnable!!, 400)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Choosing a suggestion
        searchBox.setOnItemClickListener { _, _, position, _ ->
            val suggestion = suggestions[position]
            updateSearchText(suggestion.display)
            moveToLocation(suggestion.lat, suggestion.lon)
        }

        updateSearchText(initDescription)

    }

    data class Suggestion(val display: String, val lat: Double, val lon: Double)
    private var suggestions: List<Suggestion> = emptyList()

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        searchBlocked = true
        super.onRestoreInstanceState(savedInstanceState)
        searchBlocked = false
    }

    private fun searchSuggestions(query: String) {
        runOnUiThread {
            val searchingAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                listOf(getString(R.string.location_picker_searching))
            )
            searchBox.setAdapter(searchingAdapter)
            searchBox.showDropDown()
        }
        val url = "https://nominatim.openstreetmap.org/search?" +
            "format=json&q=${URLEncoder.encode(query, "UTF-8")}&limit=5&countrycodes=es"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", packageName)
            .build()
        val call = client.newCall(request)
        ongoingCall = call

        Thread {
            try {
                val response = call.execute()
                val body = response.body?.string() ?: return@Thread
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
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        titles
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
        val url = "https://nominatim.openstreetmap.org/reverse?" +
            "format=json&lat=${point.latitude}&lon=${point.longitude}"

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", packageName)
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@Thread
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

    private fun moveToLocation(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        marker?.position = point
        map.controller.animateTo(point)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pick_location, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // ← back arrow in ActionBar
                returnCancel()
                true
                }
            R.id.action_accept -> {
                returnResult()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun returnResult() {
        marker?.position?.let { pos ->
            val intent = Intent().apply {
                putExtra(EXTRA_SELECTED_LAT, pos.latitude)
                putExtra(EXTRA_SELECTED_LON, pos.longitude)
            }
            setResult(RESULT_OK, intent)
        }
        finish()
    }

    private fun returnCancel() {
        setResult(RESULT_CANCELED)
        finish()
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
