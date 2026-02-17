package net.canvoki.shared.component.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.canvoki.shared.R

@Composable
fun WeblateLink(
    project: String,
    component: String? = null,
    instanceUrl: String = "https://hosted.weblate.org",
) {
    LinkPreference(
        url = "${instanceUrl}/projects/$project/${ component ?: "" }",
        title = stringResource(R.string.settings_translate_title),
        summary = stringResource(R.string.settings_translate_summary),
        iconResId = R.drawable.ic_weblate,
    )
}
