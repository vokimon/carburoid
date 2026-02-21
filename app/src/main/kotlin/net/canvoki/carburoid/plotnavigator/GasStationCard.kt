package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
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
    Column(
        modifier =
            modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ) { station?.let { onClick(station) } }
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val noName = stringResource(R.string.station_no_name)
                val name =
                    remember(station?.id, station?.name) {
                        station?.name ?: noName
                    }
                Text(
                    text = station?.name ?: stringResource(R.string.station_no_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                val noCity = stringResource(R.string.station_no_city)
                val locationText =
                    remember(station?.id, station?.city, station?.state) {
                        listOfNotNull(station?.city, station?.state)
                            .joinToString(" - ")
                            .ifEmpty { noCity }
                    }

                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                val noAddress = stringResource(R.string.station_no_address)
                val address =
                    remember(station?.id, station?.address) {
                        station?.address ?: noAddress
                    }
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                station?.let { OpeningStatusPill(it) }
            }

            Column(horizontalAlignment = Alignment.End) {
                val priceText =
                    remember(station?.id, station?.price) {
                        val prefix = if (station?.isPublicPrice ?: false) "" else "*"
                        station?.price?.let { prefix + "%.03f €".format(it) } ?: "?"
                    }
                val distanceText =
                    remember(station?.id, station?.let { CurrentDistancePolicy.getDistance(it) }) {
                        val distance = station?.let { CurrentDistancePolicy.getDistance(it) }
                        distance?.let { "%.01f km".format(it / 1000) } ?: "?? km"
                    }

                Text(
                    text = priceText,
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
fun OpeningStatusPill(
    station: GasStation,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    val context = LocalContext.current
    val openStatus = station.openStatus(Instant.now())
    val statusColor = openStatus.color(MaterialTheme.colorScheme)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = if (big) 4.dp else 2.dp),
    ) {
        Icon(
            painter = painterResource(id = openStatus.icon()),
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(if (big) 22.dp else 14.dp),
        )
        Spacer(modifier = Modifier.width(if (big) 8.dp else 4.dp))
        Text(
            text = openStatus.forHumans(context),
            color = statusColor,
            style = if (big) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
