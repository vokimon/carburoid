package net.canvoki.carburoid.ui.settings

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R
import net.canvoki.carburoid.algorithms.FilterSettings
import net.canvoki.carburoid.country.CountrySettings
import net.canvoki.carburoid.ui.AppScaffold
import net.canvoki.carburoid.ui.settings.LinkPreference
import net.canvoki.carburoid.ui.settings.PreferenceCategory

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    AppScaffold(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            CountrySettings.Preference()
            PreferenceCategory(
                title = stringResource(R.string.settings_category_language_appearance),
            ) {
                ThemeSettings.Preference()
                LanguageSettings.Preference()
                LinkPreference(
                    url = "https://hosted.weblate.org/projects/carburoid/carburoid-ui/",
                    title = stringResource(R.string.settings_translate_summary),
                    summary = stringResource(R.string.settings_translate_title),
                    iconResId = R.drawable.ic_weblate,
                )
            }
            PreferenceCategory(
                title = stringResource(R.string.settings_category_station_filters),
                // TODO: icon="@drawable/ic_filter_alt
            ) {
                FilterSettings.Preference()
            }
        }
    }
}
