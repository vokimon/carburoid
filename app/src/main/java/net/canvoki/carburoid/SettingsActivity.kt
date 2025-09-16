package net.canvoki.carburoid

import android.os.Bundle
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.canvoki.carburoid.ui.applyPreferencesTheme


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val themePref = findPreference<ListPreference>("dark_mode")

        themePref?.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val context = requireContext()
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("dark_mode", newValue as String).apply()
            applyPreferencesTheme(context)
            true
        }
        // Optional: Update summary to show current selection
        themePref?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
            preference.entry
        }
    }
}
