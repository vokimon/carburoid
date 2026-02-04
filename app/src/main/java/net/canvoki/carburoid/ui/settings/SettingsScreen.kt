package net.canvoki.carburoid.ui.settings

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R
import net.canvoki.carburoid.ui.settings.LinkPreference
import net.canvoki.carburoid.ui.settings.PreferenceCategory

@Composable
fun SettingsScreen() {
    MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            PreferenceCategory(
                title = stringResource(R.string.settings_category_language_appearance),
            ) {
                ThemeSettings.Preference()
                LanguageSettings.Preference()
                LinkPreference(
                    url = "https://hosted.weblate.org/projects/carburoid/carburoid-ui/",
                    title = stringResource(R.string.settings_translate_title),
                    summary = stringResource(R.string.settings_translate_summary),
                    iconResId = R.drawable.ic_weblate,
                )
            }
        }
    }
}
