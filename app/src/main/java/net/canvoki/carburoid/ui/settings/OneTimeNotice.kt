package net.canvoki.shared.component.preferences

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "one_time_notices")

@Composable
fun OneTimeNotice(
    noticeId: String,
    title: String = "Notice",
    message: String? = null,
    confirmText: String = stringResource(id = android.R.string.ok),
    dialogContent: @Composable (() -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val prefKey: Preferences.Key<Boolean> =
        remember(noticeId) { booleanPreferencesKey("notice_seen_$noticeId") }

    var showNotice = remember { mutableStateOf(false) }

    LaunchedEffect(noticeId) {
        val seen = context.dataStore.data.first()[prefKey] ?: false
        showNotice.value = !seen
    }

    if (showNotice.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    if (!message.isNullOrEmpty()) {
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (dialogContent != null) {
                        dialogContent()
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[prefKey] = true
                        }
                    }
                    showNotice.value = false
                }) {
                    Text(text = confirmText)
                }
            },
        )
    }
}

@Composable
fun ExperimentalFeatureNotice(
    noticeId: String,
    message: String,
    title: String = "Experimental feature",
    confirmText: String = stringResource(id = android.R.string.ok),
    dialogContent: @Composable (() -> Unit)? = null,
) {
    OneTimeNotice(
        noticeId = noticeId,
        title = title,
        message = message,
        confirmText = confirmText,
        dialogContent = dialogContent,
    )
}
