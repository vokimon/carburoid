package net.canvoki.carburoid

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.country.CountrySettings
import net.canvoki.carburoid.ui.setContentViewWithInsets
import net.canvoki.carburoid.ui.settings.SettingsScreen

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        val composeView =
            ComposeView(this).apply {
                setContent {
                    SettingsScreen(Modifier.fillMaxSize())
                }
            }
        setContentView(composeView)
    }
}
