package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.data.ClinicEntity
import com.example.R

class ClinicXmlAdapter(
    private var clinics: List<ClinicEntity> = emptyList(),
    private val onCallClick: ((ClinicEntity) -> Unit)? = null,
    private val onDirectionsClick: ((ClinicEntity) -> Unit)? = null
) : RecyclerView.Adapter<ClinicXmlAdapter.ViewHolder>() {

    fun updateData(newClinics: List<ClinicEntity>) {
        clinics = newClinics
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_clinic_name)
        val tvStatusBadge: TextView = view.findViewById(R.id.tv_clinic_status_badge)
        val tvAddress: TextView = view.findViewById(R.id.tv_clinic_address)
        val btnCall: Button = view.findViewById(R.id.btn_call_clinic)
        val btnDirections: Button = view.findViewById(R.id.btn_directions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clinic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = clinics[position]
        holder.tvName.text = item.name
        holder.tvStatusBadge.text = item.openHours
        holder.tvAddress.text = "${item.address} • ${String.format("%.1f", item.distanceKm)} km away"

        holder.btnCall.setOnClickListener {
            onCallClick?.invoke(item)
        }

        holder.btnDirections.setOnClickListener {
            onDirectionsClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = clinics.size
}
