package net.canvoki.carburoid.algorithms

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log
import net.canvoki.carburoid.ui.settings.ListPreference
import net.canvoki.carburoid.ui.settings.SwitchPreference
import net.canvoki.carburoid.ui.settings.rememberMutablePreference

object FilterSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_HIDE_EXPENSIVE = "hide_expensive_further"
    private const val KEY_ONLY_PUBLIC_PRICES = "only_public_prices"
    private const val KEY_HIDE_CLOSED_MARGIN_MINUTES = "hide_closed_margin_minutes"
    private const val KEY_HIDE_BEYOND_SEA = "hide_beyond_sea"

    private val relevantKeys =
        setOf(
            KEY_HIDE_EXPENSIVE,
            KEY_ONLY_PUBLIC_PRICES,
            KEY_HIDE_CLOSED_MARGIN_MINUTES,
            KEY_HIDE_BEYOND_SEA,
        )

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: Flow<Unit> get() = _changes.asSharedFlow()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun apply() {
        _changes.tryEmit(Unit)
    }

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
            hideClosedMarginInMinutes =
                prefs.getString(KEY_HIDE_CLOSED_MARGIN_MINUTES, null)?.toInt()
                    ?: default.hideClosedMarginInMinutes,
            hideBeyondSea = prefs.getBoolean(KEY_HIDE_BEYOND_SEA, default.hideBeyondSea),
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

    private fun preferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(
            context,
        )

    @Composable
    fun Preference() {
        var hideExpensive by rememberMutablePreference(KEY_HIDE_EXPENSIVE, true)
        SwitchPreference(
            title = stringResource(R.string.settings_filter_expensive),
            summary = stringResource(R.string.settings_filter_expensive_summary),
            iconResId = R.drawable.ic_filter_alt,
            checked = hideExpensive,
            onCheckedChange = {
                hideExpensive = it
                apply()
            },
        )

        var onlyPublicPrices by rememberMutablePreference(KEY_ONLY_PUBLIC_PRICES, true)
        SwitchPreference(
            title = stringResource(R.string.settings_filter_non_public),
            summary = stringResource(R.string.settings_filter_non_public_summary),
            iconResId = R.drawable.ic_filter_alt,
            checked = onlyPublicPrices,
            onCheckedChange = { newValue ->
                onlyPublicPrices = newValue
                apply()
            },
        )

        var hideBeyondSea by rememberMutablePreference(KEY_HIDE_BEYOND_SEA, true)
        SwitchPreference(
            title = stringResource(R.string.settings_filter_beyond_sea),
            summary = stringResource(R.string.settings_filter_beyond_sea_summary),
            iconResId = R.drawable.ic_filter_alt,
            checked = hideBeyondSea,
            onCheckedChange = { newValue ->
                hideBeyondSea = newValue
                apply()
            },
        )

        var hideClosedMargin by rememberMutablePreference(KEY_HIDE_CLOSED_MARGIN_MINUTES, "30")

        val context = LocalContext.current
        val resources = context.resources

        val labels = remember(resources) { resources.getStringArray(R.array.settings_filter_closed_labels) }
        val values = remember(resources) { resources.getStringArray(R.array.settings_filter_closed_values) }
        val options = remember(labels, values) { values.zip(labels) } // (value, label)

        val selectedLabel = options.find { it.first == hideClosedMargin }?.second ?: hideClosedMargin
        val originalSummary = stringResource(R.string.settings_filter_closed_summary)
        val fullSummary = "$originalSummary\n - $selectedLabel"

        ListPreference(
            title = stringResource(R.string.settings_filter_closed),
            summary = fullSummary,
            icon = R.drawable.ic_filter_alt,
            options = options,
            value = hideClosedMargin,
            onChange = { newValue ->
                hideClosedMargin = newValue
                apply()
            },
        )
    }
}
