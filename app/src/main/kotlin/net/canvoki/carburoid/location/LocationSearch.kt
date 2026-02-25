package net.canvoki.carburoid.location

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.canvoki.carburoid.FeatureFlags
import net.canvoki.carburoid.R
import net.canvoki.carburoid.network.Suggestion
import net.canvoki.carburoid.network.searchLocation

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
                    suggestions = searchLocation(query)
                } finally {
                    searching = false
                }
            }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            // Do nothing, focus controls it
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = editingText,
            onValueChange = {
                editingText = it
                userQuery = it
            },
            label = { Text(stringResource(R.string.location_picker_search_location_hint)) },
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        expanded = it.isFocused
                    }.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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
            onDismissRequest = {
                // Do nothing, focus controls it
            },
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
