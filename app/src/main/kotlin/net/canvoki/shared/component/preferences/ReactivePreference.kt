package net.canvoki.shared.component.preferences

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
@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> rememberPreferenceState(
    key: String,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val state = remember { mutableStateOf(readValue(prefs, key, defaultValue)) }

    DisposableEffect(key, context) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    state.value = readValue(prefs, key, defaultValue)
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
@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> rememberPreferenceValue(
    key: String,
    defaultValue: T,
): T = rememberPreferenceState(key, defaultValue).value

// Public read-write
@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> rememberMutablePreference(
    key: String,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val state = rememberPreferenceState(key, defaultValue)
    return remember(state, prefs, key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    state.value = value
                    writeValue(prefs, key, value)
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { newValue -> value = newValue }
        }
    }
}

// Private helpers
fun <T> readValue(
    prefs: SharedPreferences,
    key: String,
    default: T,
): T {
    @Suppress("UNCHECKED_CAST")
    return when (default) {
        is String -> prefs.getString(key, default) ?: default
        is Boolean -> prefs.getBoolean(key, default)
        is Int -> prefs.getInt(key, default)
        is Float -> prefs.getFloat(key, default)
        is Long -> prefs.getLong(key, default)
        else -> error("Unsupported type: $key")
    } as T
}

fun <T> writeValue(
    prefs: SharedPreferences,
    key: String,
    value: T,
) {
    prefs.edit {
        when (value) {
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Float -> putFloat(key, value)
            is Long -> putLong(key, value)
            else -> error("Unsupported type: $key")
        }
    }
}
