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

    private fun getMedicationTypeLabel(medName: String): String {
        val lower = medName.lowercase()
        return when {
            lower.contains("metformin") || lower.contains("sugar") || lower.contains("glucose") || lower.contains("glipizide") || lower.contains("insulin") ->
                "🩸 Type 2 Diabetes / Glycemic Medication"
            lower.contains("amlodipine") || lower.contains("lisinopril") || lower.contains("pressure") || lower.contains("losartan") || lower.contains("atenolol") ->
                "❤️ Hypertension / Cardiovascular Medication"
            lower.contains("amoxicillin") || lower.contains("antibiotic") || lower.contains("cipro") || lower.contains("azithromycin") ->
                "🔬 Antibiotic / Infection Medication"
            lower.contains("ibuprofen") || lower.contains("paracetamol") || lower.contains("aspirin") || lower.contains("pain") ->
                "💊 Analgesic / Pain & Anti-inflammatory"
            else ->
                "💊 Chronic Prescription Therapy"
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.medicationName
        val isTaken = item.status == "Taken"

        val medCategory = getMedicationTypeLabel(item.medicationName)
        holder.tvDosageTime.text = "$medCategory\n⏰ Scheduled Time: ${item.scheduledTime}"

        if (isTaken) {
            holder.tvStatusBadge?.text = "✓ Taken"
            holder.tvStatusBadge?.setTextColor(Color.parseColor("#10B981"))
            holder.tvStatusBadge?.setBackgroundResource(R.drawable.bg_badge_green)

            holder.tvStatusDesc?.text = "Completed on ${item.date} at ${item.takenTime ?: "scheduled time"}"
            holder.tvStatusDesc?.setTextColor(Color.parseColor("#10B981"))

            holder.btnAction?.visibility = View.GONE
        } else {
            holder.tvStatusBadge?.text = "⏰ Not Taken"
            holder.tvStatusBadge?.setTextColor(Color.parseColor("#F59E0B"))
            holder.tvStatusBadge?.setBackgroundResource(R.drawable.bg_badge_amber)

            holder.tvStatusDesc?.text = "Due dose scheduled for ${item.scheduledTime}"
            holder.tvStatusDesc?.setTextColor(Color.parseColor("#9CA3AF"))

            holder.btnAction?.visibility = View.VISIBLE
            holder.btnAction?.text = "✓ Mark Taken"
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
