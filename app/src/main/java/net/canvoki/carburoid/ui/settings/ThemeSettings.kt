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
        val themeMode = currentValue(context)
        when (themeMode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun preferences(context: Context) : SharedPreferences {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    private fun currentValue(context: Context) : String? {
        val prefs = preferences(context)
        return prefs.getString(KEY, "auto")
    }

    private fun setCurrentValue(context: Context, value: String) {
        val prefs = preferences(context)
        prefs.edit().putString(KEY, value).apply()
    }

    private fun saveAndApplyTheme(context: Context, mode: String) {
        setCurrentValue(context, mode)
        applyTheme(context)
    }

    private fun updateSummary(preference: ListPreference, context: Context) {
        val current = currentValue(context)
        preference.summary = when (current) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "Auto (System Default)"
        }
    }
}
