package net.canvoki.carburoid.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.R
import net.canvoki.carburoid.databinding.ActivityStationDetailBinding
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.plotnavigator.OpeningStatusPill
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.product.translateProductName
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.shared.component.openUri
import net.canvoki.shared.settings.ThemeSettings
import java.time.Instant
import com.google.android.material.R as MaterialR

/**
 * Turns every first word letter uppercase
 * considering each acronym part as a word.
 * TODO: Move it to GasStation and unit test it
 */
fun String.titlecase(): String {
    val regex = Regex("""\p{L}+""") // cualquier secuencia de letras

    return regex.replace(this) { match ->
        match.value.lowercase().replaceFirstChar { it.uppercase() }
    }
}

class StationDetailActivity : AppCompatActivity() {
    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var binding: ActivityStationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = setContentViewWithInsets(ActivityStationDetailBinding::inflate)

        val stationId = intent.getIntExtra("station_id", 0)
        val station = repository.getStationById(stationId) ?: return

        supportActionBar?.apply {
            title = station.name?.titlecase() ?: getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.migratedComponents.setContent {
            MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HeroDetails(station)
                        LocationDetails(station)
                        OpeningHoursDetails(station)
                        OtherProductList(station)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> { // ← back arrow in ActionBar
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}

@Composable
fun OtherProductList(station: GasStation) {
    val currentProduct = ProductManager.getCurrent()
    val otherProducts = station.prices.filter { it.key != currentProduct }
    if (otherProducts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.detail_other_products),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(modifier = Modifier.padding(8.dp)) {
            for ((product, price) in otherProducts) {
                if (price == null) continue
                val translatedName = translateProductName(product)
                Text(
                    text = "%.3f € - %s".format(price, translatedName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun OpeningHoursDetails(station: GasStation) {
    if (station.openingHours == null) return
    Text(
        text = stringResource(R.string.detail_opening_hours),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication =
                        ripple(
                            color =
                                MaterialTheme
                                    .colorScheme.onSurface
                                    .copy(alpha = 0.12f),
                        ),
                    onClick = {},
                ).padding(8.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_schedule),
            contentDescription = stringResource(R.string.open_in_maps),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = station.openingHours.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        )
    }
}

@Composable
fun LocationDetails(station: GasStation) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.detail_location),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                ) {
                    context.openUri("geo:${station.latitude},${station.longitude}")
                }.padding(8.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_location_on),
            contentDescription = stringResource(R.string.open_in_maps),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        ) {
            Text(
                text = "${station.address}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${station.city}, ${station.state}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Icon(
            imageVector = ImageVector.vectorResource(id = net.canvoki.shared.R.drawable.ic_arrow_outward),
            contentDescription = stringResource(R.string.open_in_maps),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun HeroDetails(station: GasStation) {
    val currentProduct = ProductManager.getCurrent()
    val hasPrice = station.price != null
    val priceText =
        if (hasPrice) {
            "%.3f €".format(station.price!!)
        } else {
            stringResource(R.string.station_no_price)
        }
    val productName = translateProductName(currentProduct)
    val distanceText =
        station.distanceInMeters?.let {
            "%.1f km".format(it / 1000f)
        } ?: stringResource(R.string.station_no_distance)

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Price and product name section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            // Product name (left of price)
            Text(
                text = productName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 12.dp, bottom = 8.dp),
            )

            // Price (right-aligned)
            Text(
                text = priceText,
                style = MaterialTheme.typography.displayLarge,
                color = if (hasPrice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        // Warning for non-public prices
        if (!station.isPublicPrice) {
            Text(
                text = stringResource(R.string.price_not_public),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, end = 12.dp),
                textAlign = TextAlign.End,
            )
        }

        // Distance and Open Status row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            OpeningStatusPill(station = station, big = true)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = distanceText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
