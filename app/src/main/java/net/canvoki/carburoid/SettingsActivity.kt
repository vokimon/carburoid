package net.canvoki.carburoid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.ui.settings.LanguageSettings
import net.canvoki.carburoid.ui.settings.ThemeSettings
import net.canvoki.carburoid.ui.setContentViewWithInsets

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentViewWithInsets(R.layout.activity_settings)

        supportActionBar?.setTitle(R.string.menu_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        LanguageSettings.registerIn(preferenceScreen)
        ThemeSettings.registerIn(preferenceScreen)
        FilterSettings.registerIn(preferenceScreen)
    }
}
