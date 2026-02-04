package net.canvoki.carburoid.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import net.canvoki.carburoid.R

object ThemeSettings {
    private const val KEY = "dark_mode"
    private const val VALUE_LIGHT = "light"
    private const val VALUE_DARK = "dark"
    private const val VALUE_AUTO = "auto"

    fun initialize(context: Context) {
        apply(context)
    }

    private fun apply(context: Context) {
        val themeMode = currentValue(context)
        AppCompatDelegate.setDefaultNightMode(
            when (themeMode) {
                VALUE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                VALUE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            },
        )
    }

    private fun preferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun currentValue(context: Context): String? {
        val prefs = preferences(context)
        return prefs.getString(KEY, VALUE_AUTO)
    }

    @Composable
    fun effectiveColorScheme(): ColorScheme {
        val currentValue = rememberPreferenceValue(KEY, VALUE_AUTO)
        val isSystemDark = isSystemInDarkTheme()
        return remember(currentValue, isSystemDark) {
            val isDark =
                when (currentValue) {
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

        var currentValue by rememberMutablePreference(KEY, VALUE_AUTO)

        val values = remember(resources) { resources.getStringArray(R.array.theme_values) }
        val entries = remember(resources) { resources.getStringArray(R.array.theme_entries) }
        val options = remember(values, entries) { values.zip(entries) } // (value, label)

        val title = stringResource(R.string.settings_theme)
        val summary =
            options.find { it.first == currentValue }?.second
                ?: stringResource(R.string.theme_system)

        ListPreference(
            title = title,
            summary = summary,
            icon = R.drawable.ic_brightness_medium,
            options = options,
            value = currentValue,
            onChange = { newValue ->
                currentValue = newValue
                apply(context)
            },
        )
    }
}
