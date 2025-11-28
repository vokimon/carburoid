package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.R
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.model.GasStation
import java.time.Instant

@Composable
fun GasStationCard(
    station: GasStation?,
    modifier: Modifier = Modifier,
    onClick: (GasStation) -> Unit = {},
) {
    val context = LocalContext.current
    val priceText = station?.price?.let { "%.03f €".format(it) } ?: "?"
    val distance = station?.let { CurrentDistancePolicy.getDistance(it) }
    val distanceText = distance?.let { "%.01f km".format(it / 1000) } ?: "?? km"
    val locationText =
        listOfNotNull(station?.city, station?.state)
            .joinToString(" - ")
            .ifEmpty { context.getString(R.string.station_no_city) }

    Column(
        modifier =
            modifier
//            .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ) { station?.let { onClick(station) } }
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station?.name ?: context.getString(R.string.station_no_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = station?.address ?: context.getString(R.string.station_no_address),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // <-- Replace previous pill Row with this composable -->
                station?.let { OpeningStatusPill(it) }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (station == null || station.isPublicPrice) priceText else "*$priceText",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    textAlign = TextAlign.End, // Aling digits
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End, // Aling digits
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun OpeningStatusPill(station: GasStation) {
    val context = LocalContext.current
    val openStatus = station.openStatus(Instant.now())
    val statusColor = openStatus.color(MaterialTheme.colorScheme)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            painter = painterResource(id = openStatus.icon()),
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = openStatus.forHumans(context),
            color = statusColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
