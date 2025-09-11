package net.canvoki.carburoid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation

class GasStationAdapter(private val stations: List<GasStation>) :
    RecyclerView.Adapter<GasStationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_name)
        val address: TextView = view.findViewById(R.id.text_address)
        val price: TextView = view.findViewById(R.id.text_price)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gas_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = stations[position]
        holder.name.text = station.name ?: "Unknown"
        holder.address.text = station.address ?: "No address"
        holder.price.text = station.priceGasoleoA ?: "Price N/A"  // ← Updated property name
    }

    override fun getItemCount() = stations.size
}
