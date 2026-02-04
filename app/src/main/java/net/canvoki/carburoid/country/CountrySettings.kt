package net.canvoki.carburoid.country

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import net.canvoki.carburoid.R
import net.canvoki.carburoid.country.CountryRegistry
import net.canvoki.carburoid.log
import net.canvoki.carburoid.ui.settings.ListPreference
import net.canvoki.carburoid.ui.settings.rememberMutablePreference

object CountrySettings {
    private const val KEY = "app_country"

    data class CountryOption(
        val code: String,
        val nameResId: Int,
    )

    fun initialize(context: Context) {
        apply(context)
    }

    fun apply(context: Context) {
        CountryRegistry.setCurrent(getPreferencesValue(context))
    }

    private fun getAvailableCountries(): List<CountryOption> =
        CountryRegistry.availableCountries().map { country ->
            CountryOption(
                code = country.countryCode,
                nameResId = country.nameResId,
            )
        }

    private fun getPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun setPreferencesValue(
        context: Context,
        code: String,
    ) {
        getPrefs(context).edit {
            putString(KEY, code)
        }
    }

    fun getPreferencesValue(context: Context): String =
        getPrefs(context).getString(KEY, CountryRegistry.DEFAULT_COUNTRY_CODE)
            ?: CountryRegistry.DEFAULT_COUNTRY_CODE

    /** Return the currently selected country from the registry */
    fun current(context: Context) = CountryRegistry.getCountry(getPreferencesValue(context))

    @Composable
    fun rememberMutableCountry() = rememberMutablePreference(KEY, CountryRegistry.DEFAULT_COUNTRY_CODE)

    @Composable
    fun rememberCountry() =
        CountryRegistry.getCountry(rememberMutablePreference(KEY, CountryRegistry.DEFAULT_COUNTRY_CODE).value)

    @Composable
    fun Preference() {
        val context = LocalContext.current

        // Reactive state synced with SharedPreferences
        var currentValue by rememberMutableCountry()

        val countries = getAvailableCountries()
        val options =
            countries.map { country ->
                country.code to stringResource(country.nameResId)
            }

        val title = stringResource(R.string.settings_country_title)
        val currentCountry = CountryRegistry.getCountry(currentValue)
        val summary = stringResource(currentCountry.nameResId)

        ListPreference(
            title = title,
            summary = summary,
            icon = R.drawable.ic_system_update_alt,
            options = options,
            value = currentValue,
            onChange = { newCode ->
                currentValue = newCode
                apply(context)
            },
        )
    }
}
