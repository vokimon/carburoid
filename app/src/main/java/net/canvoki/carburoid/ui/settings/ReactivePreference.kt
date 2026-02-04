package net.canvoki.carburoid.ui.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.preference.PreferenceManager

// Private helper: creates reactive state synced with SharedPreferences
@Composable
private fun rememberPreferenceState(
    key: String,
    defaultValue: String,
): MutableState<String> {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val state = remember { mutableStateOf(prefs.getString(key, defaultValue) ?: defaultValue) }

    DisposableEffect(key, context) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    state.value = prefs.getString(key, defaultValue) ?: defaultValue
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return state
}

// Public read-only
@Composable
fun rememberPreferenceValue(
    key: String,
    defaultValue: String,
): String = rememberPreferenceState(key, defaultValue).value

// Public read-write
@Composable
fun rememberMutablePreference(
    key: String,
    defaultValue: String,
): MutableState<String> {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val state = rememberPreferenceState(key, defaultValue)
    return remember {
        object : MutableState<String> {
            override var value: String
                get() = state.value
                set(value) {
                    state.value = value
                    prefs.edit { putString(key, value) }
                }

            override fun component1(): String = value

            override fun component2(): (String) -> Unit = { newValue -> value = newValue }
        }
    }
}
