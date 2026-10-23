package com.example.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.DoseLogEntity

class MedicationXmlAdapter(
    private var items: List<DoseLogEntity> = emptyList(),
    private val onMarkTaken: (Long) -> Unit
) : RecyclerView.Adapter<MedicationXmlAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_med_name)
        val tvDosageTime: TextView = itemView.findViewById(R.id.tv_med_dosage_time)
        val tvStatusBadge: TextView? = itemView.findViewById(R.id.tv_med_status_badge)
        val tvStatusDesc: TextView? = itemView.findViewById(R.id.tv_med_status_desc)
        val btnAction: Button? = itemView.findViewById(R.id.btn_mark_dose_action)
        val cbTaken: CheckBox? = itemView.findViewById(R.id.cb_taken)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.medicationName
        val isTaken = item.status == "Taken"

        holder.tvDosageTime.text = "Scheduled for ${item.scheduledTime} (${item.date})"

        if (isTaken) {
            holder.tvStatusBadge?.text = "✓ Taken"
            holder.tvStatusBadge?.setTextColor(Color.parseColor("#10B981"))
            holder.tvStatusBadge?.setBackgroundResource(R.drawable.bg_badge_green)

            holder.tvStatusDesc?.text = "Completed today at ${item.takenTime ?: "earlier"}"
            holder.tvStatusDesc?.setTextColor(Color.parseColor("#10B981"))

            holder.btnAction?.visibility = View.GONE
        } else {
            holder.tvStatusBadge?.text = "⏰ Due"
            holder.tvStatusBadge?.setTextColor(Color.parseColor("#F59E0B"))
            holder.tvStatusBadge?.setBackgroundResource(R.drawable.bg_badge_amber)

            holder.tvStatusDesc?.text = "Scheduled dose: ${item.scheduledTime}"
            holder.tvStatusDesc?.setTextColor(Color.parseColor("#9CA3AF"))

            holder.btnAction?.visibility = View.VISIBLE
            holder.btnAction?.text = "✓ Take Dose"
            holder.btnAction?.setOnClickListener {
                onMarkTaken(item.id)
            }
        }

        holder.cbTaken?.setOnCheckedChangeListener(null)
        holder.cbTaken?.isChecked = isTaken
        holder.cbTaken?.isEnabled = !isTaken
        holder.cbTaken?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isTaken) {
                onMarkTaken(item.id)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DoseLogEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
