package net.canvoki.carburoid.ui.settings
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

object ThemeSettings {

    private const val KEY = "dark_mode"

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val themePref = screen.findPreference<ListPreference>(KEY) ?: return

        updateSummary(themePref, context)

        themePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val mode = newValue as String
            saveAndApplyTheme(context, mode)
            updateSummary(themePref, context)
            true
        }
    }

    fun applyTheme(context: Context) {
        val prefs = preferences(context)
        val themeMode = prefs.getString(KEY, "auto")
        when (themeMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun preferences(context: Context) : SharedPreferences {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    private fun saveAndApplyTheme(context: Context, mode: String) {
        val prefs = preferences(context)
        prefs.edit().putString(KEY, mode).apply()
        applyTheme(context)
    }

    private fun updateSummary(preference: ListPreference, context: Context) {
        val prefs = preferences(context)
        val current = prefs.getString(KEY, "auto")
        preference.summary = when (current) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "Auto (System Default)"
        }
    }
}
