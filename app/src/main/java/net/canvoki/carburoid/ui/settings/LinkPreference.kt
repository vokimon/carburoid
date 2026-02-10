package net.canvoki.carburoid.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import net.canvoki.carburoid.R

@Composable
fun LinkPreference(
    url: String,
    title: String,
    summary: String,
    @DrawableRes iconResId: Int,
) {
    val context = LocalContext.current

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier =
            Modifier.clickable {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        url.toUri(),
                    ),
                    null,
                )
            },
    )
}
