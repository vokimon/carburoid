package net.canvoki.carburoid.ui.settings

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.R

@Composable
fun SettingsScreen() {
    MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            PreferenceCategory(
                title = stringResource(R.string.settings_category_language_appearance),
            ) {
                ThemePreferenceItem()
            }
        }
    }
}

@Composable
private fun PreferenceCategory(
    title: String?,
    content: @Composable () -> Unit,
) {
    if (!title.isNullOrBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
            modifier = Modifier.padding(start = 56.dp, top = 24.dp, bottom = 8.dp),
        )
    }
    content()
}

@Composable
fun ThemePreferenceItem() {
    val context = LocalContext.current
    val resources = context.resources

    var currentValue by remember { mutableStateOf("auto") }

    val entries = remember(resources) { resources.getStringArray(R.array.theme_entries) }
    val values = remember(resources) { resources.getStringArray(R.array.theme_values) }
    val options = remember(entries, values) { entries.zip(values) }

    val title = stringResource(R.string.settings_theme)
    val summary =
        options.find { it.second == currentValue }?.first
            ?: stringResource(R.string.theme_system)

    ListPreference(
        title = title,
        summary = summary,
        icon = R.drawable.ic_brightness_medium,
        options = options,
        value = currentValue,
        onChange = { currentValue = it },
    )
}

@Composable
fun ListPreference(
    title: String,
    summary: String,
    @DrawableRes icon: Int,
    options: List<Pair<String, String>>, // (label, value)
    value: String,
    onChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (label, optionValue) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(optionValue)
                                        showDialog = false
                                    }.padding(8.dp),
                        ) {
                            RadioButton(
                                selected = value == optionValue,
                                onClick = null,
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }
}
