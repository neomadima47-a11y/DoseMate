package com.example.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.DoseLogEntity
import com.example.data.MedicationEntity

class MedicationXmlAdapter(
    private var items: List<DoseLogEntity> = emptyList(),
    private val onMarkTaken: (Long) -> Unit,
    // Feature 4: refill tracking needs the parent medication for each dose,
    // because the remaining tablet count is stored on the medication row.
    private val onRefill: ((Long, Int) -> Unit)? = null
) : RecyclerView.Adapter<MedicationXmlAdapter.ViewHolder>() {

    /** Medication rows keyed by id, used to read pack size and remaining count. */
    private var medicationsById: Map<Long, MedicationEntity> = emptyMap()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_med_name)
        val tvDosageTime: TextView = itemView.findViewById(R.id.tv_med_dosage_time)
        val tvStatusBadge: TextView? = itemView.findViewById(R.id.tv_med_status_badge)
        val tvStatusDesc: TextView? = itemView.findViewById(R.id.tv_med_status_desc)
        val btnAction: Button? = itemView.findViewById(R.id.btn_mark_dose_action)
        val cbTaken: CheckBox? = itemView.findViewById(R.id.cb_taken)

        // Refill tracking views
        val layoutRefill: LinearLayout? = itemView.findViewById(R.id.layout_refill_row)
        val tvRefillCount: TextView? = itemView.findViewById(R.id.tv_refill_count)
        val pbRefill: ProgressBar? = itemView.findViewById(R.id.pb_refill_progress)
        val btnRefill: Button? = itemView.findViewById(R.id.btn_refill_med)
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

        bindRefillRow(holder, item)

        holder.cbTaken?.setOnCheckedChangeListener(null)
        holder.cbTaken?.isChecked = isTaken
        holder.cbTaken?.isEnabled = !isTaken
        holder.cbTaken?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isTaken) {
                onMarkTaken(item.id)
            }
        }
    }

    /**
     * Shows the remaining tablet count for medications that have refill
     * tracking switched on. Medications added without a pack size keep the
     * row hidden, so the card looks exactly as it did before.
     */
    private fun bindRefillRow(holder: ViewHolder, item: DoseLogEntity) {
        val med = medicationsById[item.medicationId]

        if (med == null || med.packSize <= 0) {
            holder.layoutRefill?.visibility = View.GONE
            holder.pbRefill?.visibility = View.GONE
            return
        }

        holder.layoutRefill?.visibility = View.VISIBLE
        holder.pbRefill?.visibility = View.VISIBLE

        val remaining = med.pillsRemaining
        val total = med.packSize
        val percent = if (total > 0) (remaining * 100) / total else 0
        val isLow = remaining <= med.refillThreshold

        holder.tvRefillCount?.text = when {
            remaining == 0 -> "Pack empty — collect a refill"
            // "Only" is misleading on a pack that has not been used yet
            isLow && remaining < total -> "Only $remaining of $total tablets left"
            else -> "$remaining of $total tablets left"
        }

        val countColour = when {
            remaining == 0 -> "#EF4444"
            isLow -> "#F59E0B"
            else -> "#10B981"
        }
        holder.tvRefillCount?.setTextColor(Color.parseColor(countColour))
        holder.pbRefill?.progress = percent
        holder.pbRefill?.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(countColour))

        // The Refill button only appears once stock is actually low
        if (isLow && onRefill != null) {
            holder.btnRefill?.visibility = View.VISIBLE
            holder.btnRefill?.setOnClickListener {
                onRefill.invoke(med.id, med.packSize)
            }
        } else {
            holder.btnRefill?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DoseLogEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    /** Supplies the medication rows so the refill counts can be displayed. */
    fun updateMedications(medications: List<MedicationEntity>) {
        medicationsById = medications.associateBy { it.id }
        notifyDataSetChanged()
    }
}