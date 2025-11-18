package net.canvoki.carburoid.algorithms

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log

object FilterSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_HIDE_EXPENSIVE = "hide_expensive_further"
    private const val KEY_ONLY_PUBLIC_PRICES = "only_public_prices"
    private const val KEY_HIDE_CLOSED_MARGIN_MINUTES = "hide_closed_margin_minutes"

    private val relevantKeys =
        setOf(
            KEY_HIDE_EXPENSIVE,
            KEY_ONLY_PUBLIC_PRICES,
        )

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: Flow<Unit> get() = _changes.asSharedFlow()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val prefs = preferences(context)

        // unregister(context) // ✅ Safe unregister in case it's already registered

        updateSummary(screen, prefs, context, KEY_HIDE_CLOSED_MARGIN_MINUTES)

        listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                log("Changed $key")
                updateSummary(screen, prefs, context, key ?: "")
                if (key in relevantKeys) {
                    _changes.tryEmit(Unit)
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(context: Context) {
        listener?.let {
            preferences(context).unregisterOnSharedPreferenceChangeListener(it)
            listener = null
        }
    }

    val DEFAULT_CLOSED_MARGIN = 2 * 60

    fun config(context: Context): FilterConfig {
        val prefs = preferences(context)
        val default = FilterConfig()
        return FilterConfig(
            hideExpensiveFurther = prefs.getBoolean(KEY_HIDE_EXPENSIVE, default.hideExpensiveFurther),
            onlyPublicPrices = prefs.getBoolean(KEY_ONLY_PUBLIC_PRICES, default.onlyPublicPrices),
            hideClosedMarginInMinutes = prefs.getString(KEY_HIDE_CLOSED_MARGIN_MINUTES, null)?.toInt() ?: default.hideClosedMarginInMinutes,
        )
    }

    private fun updateSummary(
        screen: PreferenceScreen,
        preference: SharedPreferences,
        context: Context,
        key: String,
    ) {
        if (key != KEY_HIDE_CLOSED_MARGIN_MINUTES) return

        val listPreference = screen.findPreference<ListPreference>(key)
        listPreference?.let {
            val originalSummary = context.getString(R.string.settings_filter_closed_summary)
            val selectedValue = preference.getString(key, null) ?: DEFAULT_CLOSED_MARGIN.toString()

            // Carregar l'etiqueta corresponent al valor
            val labels = context.resources.getStringArray(R.array.settings_filter_closed_labels)
            val values = context.resources.getStringArray(R.array.settings_filter_closed_values)

            val index = values.indexOf(selectedValue)
            val selectedLabel = if (index >= 0 && index < labels.size) labels[index] else selectedValue

            it.summary = "${originalSummary}\n - $selectedLabel"
        }
    }

    private fun preferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
