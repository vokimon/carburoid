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
import net.canvoki.carburoid.ui.settings.OneTimeNotice
import net.canvoki.carburoid.ui.settings.SwitchPreference
import net.canvoki.carburoid.ui.settings.rememberMutablePreference

object FilterSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_HIDE_EXPENSIVE = "hide_expensive_further"
    private const val KEY_ONLY_PUBLIC_PRICES = "only_public_prices"
    private const val KEY_HIDE_CLOSED_MARGIN_MINUTES = "hide_closed_margin_minutes"
    private const val KEY_HIDE_BEYOND_SEA = "hide_beyond_sea"

    val DEFAULT_CLOSED_MARGIN = (2 * 60).toString()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: Flow<Unit> get() = _changes.asSharedFlow()

    fun apply() {
        _changes.tryEmit(Unit)
    }

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

    private fun preferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(
            context,
        )

    @Composable
    fun Preference() {
        var hideExpensive by rememberMutablePreference(KEY_HIDE_EXPENSIVE, true)
        var showHideExpensiveDisabledWarning by remember { mutableStateOf(false) }
        SwitchPreference(
            title = stringResource(R.string.settings_filter_expensive),
            summary = stringResource(R.string.settings_filter_expensive_summary),
            iconResId = R.drawable.ic_euro,
            checked = hideExpensive,
            onCheckedChange = {
                hideExpensive = it
                apply()
                showHideExpensiveDisabledWarning = hideExpensive == false
            },
        )
        if (showHideExpensiveDisabledWarning) {
            OneTimeNotice(
                noticeId = "hide_expensive_disabled",
                title = stringResource(R.string.warning_dialog_title),
                message = stringResource(R.string.settings_filter_expensive_warning),
            )
        }

        var onlyPublicPrices by rememberMutablePreference(KEY_ONLY_PUBLIC_PRICES, true)
        SwitchPreference(
            title = stringResource(R.string.settings_filter_non_public),
            summary = stringResource(R.string.settings_filter_non_public_summary),
            iconResId = R.drawable.ic_price_check,
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
            iconResId = R.drawable.ic_water,
            checked = hideBeyondSea,
            onCheckedChange = { newValue ->
                hideBeyondSea = newValue
                apply()
            },
        )

        var hideClosedMargin by rememberMutablePreference(
            KEY_HIDE_CLOSED_MARGIN_MINUTES,
            DEFAULT_CLOSED_MARGIN,
        )

        val context = LocalContext.current
        val resources = context.resources

        val labels = remember(resources) { resources.getStringArray(R.array.settings_filter_closed_labels) }
        val values = remember(resources) { resources.getStringArray(R.array.settings_filter_closed_values) }
        val options = remember(labels, values) { values.zip(labels) } // (value, label)

        val selectedLabel = options.find { it.first == hideClosedMargin }?.second ?: hideClosedMargin
        val summary = stringResource(R.string.settings_filter_closed_summary)

        ListPreference(
            title = stringResource(R.string.settings_filter_closed),
            summary = summary,
            icon = R.drawable.ic_door_front,
            options = options,
            value = hideClosedMargin,
            onChange = { newValue ->
                hideClosedMargin = newValue
                apply()
            },
            trailingText = selectedLabel,
        )
    }
}
