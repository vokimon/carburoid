package net.canvoki.carburoid.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ListPreference(
    title: String,
    summary: String,
    @DrawableRes icon: Int,
    options: List<Pair<String, String>>, // (value, label)
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
                LazyColumn {
                    items(options) { (optionValue, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(optionValue)
                                        showDialog = false
                                    }.padding(16.dp),
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
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        )
    }
}
