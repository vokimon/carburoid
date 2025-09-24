package net.canvoki.carburoid.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR
import java.time.Instant
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.databinding.ActivityStationDetailBinding
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.repository.GasStationRepository

class StationDetailActivity : AppCompatActivity() {

    private val app: CarburoidApplication
        get() = application as CarburoidApplication

    private val repository: GasStationRepository
        get() = app.repository

    private lateinit var binding: ActivityStationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stationId = intent.getIntExtra("station_id", 0);
        val station = repository.getStationById(stationId) ?: return // TODO: blank page

        binding.textName.text = station.name
        binding.textCurrentProduct.text = ProductManager.getCurrent()
        binding.textPrice.text = station.price?.let { "${String.format("%.3f", it)} €" } ?: "No price"
        binding.textDistance.text = station.distanceInMeters?.let { "%.1f km".format(it / 1000f) } ?: "Unknown distance"

        val status = station.openStatus(Instant.now())
        if (status != null) {
            val statusText = "TODO: opening status" // formatOpeningStatus(this, status, station.timeZone(), 60)
            binding.textOpenStatus.text = statusText
            val colorAttr = if (status.isOpen)
                    MaterialR.attr.colorSecondary
                else
                    AppCompatR.attr.colorError
            val typedValue = TypedValue()
            theme.resolveAttribute(colorAttr, typedValue, true)
            binding.textOpenStatus.setTextColor(typedValue.data)
            binding.textOpenStatus.visibility = View.VISIBLE
        } else {
            binding.textOpenStatus.visibility = View.GONE
        }

        binding.textAddress.text = station.address
        binding.textCityState.text = "${station.city}, ${station.state}"

        binding.textOpeningHours.text = station.openingHours?.toString() ?: "Permanently Closed"

        binding.textExclusivePriceWarning.visibility = if (station.isPublicPrice) View.GONE else View.VISIBLE

        // Other products
        val otherProducts = station.prices.filter { it.key != ProductManager.getCurrent() }
        if (otherProducts.isNotEmpty()) {
            binding.labelOtherProducts.visibility = View.VISIBLE
            binding.containerOtherProducts.visibility = View.VISIBLE

            for ((product, price) in otherProducts) {
                if (price == null) continue
                val textView = TextView(this)
                textView.text = "${String.format("%.3f", price)} € - $product"
                textView.setTextAppearance(MaterialR.style.TextAppearance_Material3_BodyMedium)
                binding.containerOtherProducts.addView(textView)
            }
        }
    }

    private fun findStationById(id: String): GasStation? {
        // ← Implement this — e.g., from repository or passed as Parcelable
        return null
    }
}
