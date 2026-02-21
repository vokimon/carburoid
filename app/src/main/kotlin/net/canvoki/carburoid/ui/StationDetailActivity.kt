package net.canvoki.carburoid.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.R
import net.canvoki.carburoid.databinding.ActivityStationDetailBinding
import net.canvoki.carburoid.model.GasStation
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

        val currentProduct = ProductManager.getCurrent()
        binding.textCurrentProduct.text = translateProductName(currentProduct, this)

        binding.textPrice.text =
            station.price?.let { "%.3f €".format(it) }
                ?: getString(R.string.station_no_price)

        binding.textDistance.text = station.distanceInMeters?.let {
            "%.1f km".format(it / 1000f)
        } ?: getString(R.string.station_no_distance)

        val status = station.openStatus(Instant.now())
        val statusText = status.forHumans(this)
        val statusColor = status.color(this)
        binding.textOpenStatus.text = statusText
        binding.textOpenStatus.setTextColor(statusColor)
        binding.iconOpenStatus.setImageResource(status.icon())
        binding.iconOpenStatus.imageTintList = ColorStateList.valueOf(statusColor)
        binding.textOpenStatus.visibility = View.VISIBLE

        binding.textAddress.text = station.address
        binding.textCityState.text = "${station.city}, ${station.state}"

        binding.layoutAddressMap.setOnClickListener {
            openUri("geo:${station.latitude},${station.longitude}")
        }

        binding.textExclusivePriceWarning.visibility =
            if (station.isPublicPrice) View.GONE else View.VISIBLE

        binding.migratedComponents.setContent {
            MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth()) {
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

@Composable fun OtherProductList(station: GasStation) {
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

@Composable fun OpeningHoursDetails(station: GasStation) {
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
