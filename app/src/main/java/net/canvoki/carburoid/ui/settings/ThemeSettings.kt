package net.canvoki.carburoid.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.canvoki.carburoid.R

object ThemeSettings {
    private const val KEY = "dark_mode"
    private const val VALUE_LIGHT = "light"
    private const val VALUE_DARK = "dark"
    private const val VALUE_AUTO = "auto"

    private val _themeMode = MutableStateFlow<String?>(null)
    val themeMode: StateFlow<String?> = _themeMode

    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun initialize(context: Context) {
        if (prefListener != null) return

        val prefs = preferences(context)
        prefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY) {
                    _themeMode.value = currentValue(context) ?: VALUE_AUTO
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        _themeMode.value = currentValue(context) ?: VALUE_AUTO
        apply(context)
    }

    fun apply(context: Context) {
        val themeMode = currentValue(context)
        when (themeMode) {
            VALUE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            VALUE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            VALUE_AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun preferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun currentValue(context: Context): String? {
        val prefs = preferences(context)
        return prefs.getString(KEY, VALUE_AUTO)
    }

    private fun setCurrentValue(
        context: Context,
        value: String,
    ) {
        val prefs = preferences(context)
        prefs.edit { putString(KEY, value) }
    }

    private fun saveAndApply(
        context: Context,
        mode: String,
    ) {
        setCurrentValue(context, mode)
        apply(context)
    }

    @Composable
    fun effectiveColorScheme(): ColorScheme {
        val context = LocalContext.current
        val initialMode = remember { currentValue(context) ?: VALUE_AUTO }
        val currentMode by _themeMode.collectAsState(initial = initialMode)
        val isSystemDark = isSystemInDarkTheme()

        return remember(currentMode) {
            val isDark =
                when (currentMode) {
                    VALUE_LIGHT -> false
                    VALUE_DARK -> true
                    else -> isSystemDark
                }
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    }

    @Composable
    fun Preference() {
        val context = LocalContext.current
        val resources = context.resources

        val currentValue by _themeMode.collectAsState(
            initial = currentValue(context),
        )

        val values = remember(resources) { resources.getStringArray(R.array.theme_values) }
        val entries = remember(resources) { resources.getStringArray(R.array.theme_entries) }
        val options = remember(values, entries) { values.zip(entries) }

        val title = stringResource(R.string.settings_theme)
        val summary =
            options.find { it.first == currentValue }?.second
                ?: stringResource(R.string.theme_system)

        ListPreference(
            title = title,
            summary = summary,
            icon = R.drawable.ic_brightness_medium,
            options = options,
            value = currentValue ?: VALUE_AUTO,
            onChange = { newValue ->
                saveAndApply(context, newValue)
            },
        )
    }
}
