package net.canvoki.carburoid.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.R
import net.canvoki.carburoid.databinding.ActivityStationDetailBinding
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.product.translateProductName
import net.canvoki.carburoid.repository.GasStationRepository
import java.time.Instant
import com.google.android.material.R as MaterialR

class StationDetailActivity : AppCompatActivity() {
    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var binding: ActivityStationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val content = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }


        val stationId = intent.getIntExtra("station_id", 0)
        val station = repository.getStationById(stationId) ?: return

        binding.textName.text = station.name

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
            val intent = Intent(Intent.ACTION_VIEW, "geo:${station.latitude},${station.longitude}".toUri())
            startActivity(intent)
        }

        binding.textOpeningHours.text = station.openingHours?.toString()
            ?: getString(R.string.station_status_permanently_closed)

        binding.textExclusivePriceWarning.visibility =
            if (station.isPublicPrice) View.GONE else View.VISIBLE

        val otherProducts = station.prices.filter { it.key != currentProduct }
        if (otherProducts.isNotEmpty()) {
            binding.labelOtherProducts.visibility = View.VISIBLE
            binding.containerOtherProducts.visibility = View.VISIBLE

            for ((product, price) in otherProducts) {
                if (price == null) continue

                val translatedName = translateProductName(product, this)

                val textView = TextView(this)
                textView.text = "%.3f €".format(price) + " - " + translatedName
                textView.setTextAppearance(MaterialR.style.TextAppearance_Material3_BodyMedium)
                binding.containerOtherProducts.addView(textView)
            }
        }
    }
}
