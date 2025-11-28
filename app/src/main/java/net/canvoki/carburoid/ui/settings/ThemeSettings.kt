package net.canvoki.carburoid.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import net.canvoki.carburoid.R

object ThemeSettings {
    private const val KEY = "dark_mode"
    private const val VALUE_LIGHT = "light"
    private const val VALUE_DARK = "dark"
    private const val VALUE_AUTO = "auto"

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val themePref = screen.findPreference<ListPreference>(KEY) ?: return

        updateSummary(themePref, context)

        themePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val mode = newValue as String
            saveAndApply(context, mode)
            updateSummary(themePref, context)
            true
        }
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
        PreferenceManager.getDefaultSharedPreferences(
            context,
        )

    private fun currentValue(context: Context): String? {
        val prefs = preferences(context)
        return prefs.getString(KEY, VALUE_AUTO)
    }

    private fun setCurrentValue(
        context: Context,
        value: String,
    ) {
        val prefs = preferences(context)
        prefs.edit {
            putString(KEY, value)
        }
    }

    private fun saveAndApply(
        context: Context,
        mode: String,
    ) {
        setCurrentValue(context, mode)
        apply(context)
    }

    private fun updateSummary(
        preference: ListPreference,
        context: Context,
    ) {
        val current = currentValue(context)
        preference.summary =
            when (current) {
                VALUE_LIGHT -> context.getString(R.string.theme_light)
                VALUE_DARK -> context.getString(R.string.theme_dark)
                else -> context.getString(R.string.theme_system)
            }
    }

    @Composable
    fun effectiveColorScheme(): ColorScheme {
        val isDark =
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO -> false
                AppCompatDelegate.MODE_NIGHT_YES -> true
                else -> isSystemInDarkTheme()
            }
        return if (isDark) darkColorScheme() else lightColorScheme()
    }
}
