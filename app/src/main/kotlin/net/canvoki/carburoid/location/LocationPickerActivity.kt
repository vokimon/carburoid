package net.canvoki.carburoid.location

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.canvoki.carburoid.FeatureFlags
import net.canvoki.carburoid.R
import net.canvoki.carburoid.network.Suggestion
import net.canvoki.carburoid.network.nameLocation
import net.canvoki.carburoid.network.searchLocation
import net.canvoki.carburoid.ui.setContentViewWithInsets
import net.canvoki.shared.log
import net.canvoki.shared.settings.ThemeSettings
import org.maplibre.spatialk.geojson.Position

fun Position.pretty(): String = "(${ "%.3f".format(latitude) }, ${ "%.3f".format(longitude) })"

class LocationPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CURRENT_DESCRIPTION = "current_description"
        const val EXTRA_CURRENT_LAT = "current_lat"
        const val EXTRA_CURRENT_LON = "current_lon"
        const val EXTRA_TARGET_LAT = "target_lat"
        const val EXTRA_TARGET_LON = "target_lon"
    }

    private var currentDescription by mutableStateOf<String>("")
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
                        locationDescription = currentDescription,
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
                            currentDescription = "${pos.pretty()}"
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
        log("MAP STATE TO SAVED INSTANCE  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
        outState.apply {
            currentPosition.let {
                putDouble(EXTRA_CURRENT_LAT, it.latitude)
                putDouble(EXTRA_CURRENT_LON, it.longitude)
            }
            targetPosition?.let {
                putDouble(EXTRA_TARGET_LAT, it.latitude)
                putDouble(EXTRA_TARGET_LON, it.longitude)
            }
        }
    }

    private fun returnResult() {
        log("MAP STATE TO INTENT  ${currentPosition.pretty()} -> ${targetPosition?.pretty()}")
        val intent =
            Intent().apply {
                currentPosition.let {
                    putExtra(EXTRA_CURRENT_LAT, it.latitude)
                    putExtra(EXTRA_CURRENT_LON, it.longitude)
                }
                targetPosition?.let {
                    putExtra(EXTRA_TARGET_LAT, it.latitude)
                    putExtra(EXTRA_TARGET_LON, it.longitude)
                }
            }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setStateFromSavedInstance(inState: Bundle) {
        val lat = inState.getDouble(EXTRA_CURRENT_LAT, 40.0)
        val lon = inState.getDouble(EXTRA_CURRENT_LON, -1.0)
        val desc = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        val targetLat = if (inState.containsKey(EXTRA_TARGET_LAT)) inState.getDouble(EXTRA_TARGET_LAT) else null
        val targetLon = if (inState.containsKey(EXTRA_TARGET_LON)) inState.getDouble(EXTRA_TARGET_LON) else null
        log("MAP STATE FROM SAVED INSTANCE  $lat $lon $desc")
        updateSearchText(desc)
        moveToLocation(lat, lon)
        targetPosition =
            targetLat?.let { tglat ->
                targetLon?.let { tglon ->
                    Position(latitude = tglat, longitude = tglon)
                }
            }
    }

    private fun setStateFromIntent(intent: Intent) {
        val lat = intent.getDoubleExtra(EXTRA_CURRENT_LAT, 40.0)
        val lon = intent.getDoubleExtra(EXTRA_CURRENT_LON, 0.0)
        val desc = intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION) ?: ""
        val targetLat = if (intent.hasExtra(EXTRA_TARGET_LAT)) intent.getDoubleExtra(EXTRA_TARGET_LAT, 40.0) else null
        val targetLon = if (intent.hasExtra(EXTRA_TARGET_LON)) intent.getDoubleExtra(EXTRA_TARGET_LON, -1.0) else null
        log("MAP STATE FROM INTENT  $lat $lon $desc")
        updateSearchText(desc)
        moveToLocation(lat, lon)
        targetPosition =
            targetLat?.let { tglat ->
                targetLon?.let { tglon ->
                    Position(latitude = tglat, longitude = tglon)
                }
            }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun updateSearchText(newText: String) {
        log("Updating text updateSearchText: $newText")
        currentDescription = newText
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
