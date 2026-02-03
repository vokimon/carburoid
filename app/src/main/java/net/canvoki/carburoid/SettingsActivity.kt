package net.canvoki.carburoid

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.country.CountrySettings
import net.canvoki.carburoid.ui.setContentViewWithInsets
import net.canvoki.carburoid.ui.settings.LanguageSettings
import net.canvoki.carburoid.ui.settings.SettingsScreen
import net.canvoki.carburoid.ui.settings.ThemeSettings

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
        CountrySettings.registerIn(preferenceScreen)
        FilterSettings.registerIn(preferenceScreen)
    }
}

// transitional while view -> compose migration
class ComposePreference
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.preferenceStyle,
    ) : Preference(context, attrs, defStyleAttr) {
        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val rootView = holder.itemView as? ViewGroup ?: return
            rootView.removeAllViews()
            rootView.addView(
                ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent { SettingsScreen() }
                },
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }
