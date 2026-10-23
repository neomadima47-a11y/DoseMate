package com.example.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.data.HealthReadingEntity
import com.example.util.ClinicalEvaluator
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthLogXmlAdapter(
    private var logs: List<HealthReadingEntity> = emptyList(),
    private val onItemClick: ((HealthReadingEntity) -> Unit)? = null
) : RecyclerView.Adapter<HealthLogXmlAdapter.ViewHolder>() {

    fun updateData(newLogs: List<HealthReadingEntity>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tv_log_icon)
        val tvType: TextView = view.findViewById(R.id.tv_log_type)
        val tvSeverityBadge: TextView? = view.findViewById(R.id.tv_log_status_badge)
        val tvValue: TextView = view.findViewById(R.id.tv_log_value)
        val tvDate: TextView = view.findViewById(R.id.tv_log_date)
        val tvNotes: TextView = view.findViewById(R.id.tv_log_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = logs[position]

        val icon = when {
            item.type.contains("pressure", ignoreCase = true) || item.type.contains("BP", ignoreCase = true) -> "❤️"
            item.type.contains("sugar", ignoreCase = true) || item.type.contains("glucose", ignoreCase = true) -> "💧"
            else -> "🧠"
        }

        holder.tvIcon.text = icon
        holder.tvType.text = if (item.type.isNotBlank()) item.type else "Health Record"
        holder.tvValue.text = if (item.unit.isNotBlank()) "${item.value} ${item.unit}" else item.value

        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        holder.tvDate.text = if (item.displayTime.isNotBlank()) item.displayTime else sdf.format(Date(item.timestamp))

        holder.tvNotes.text = if (item.notes.isNotBlank()) item.notes else "No notes attached"

        // Clinical Evaluation severity styling
        if (item.type.contains("pressure", ignoreCase = true) && item.value.contains("/")) {
            try {
                val parts = item.value.split("/")
                val sys = parts[0].trim().toDouble()
                val dia = parts[1].trim().toDouble()
                val eval = ClinicalEvaluator.evaluateBloodPressure(sys, dia, item.notes, item.whenTaken)
                holder.tvSeverityBadge?.text = "${eval.badgeIcon} ${eval.classification}"
                holder.tvSeverityBadge?.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.tvSeverityBadge?.visibility = View.GONE
            }
        } else if (item.type.contains("sugar", ignoreCase = true) || item.type.contains("glucose", ignoreCase = true)) {
            try {
                val valNum = item.value.trim().toDouble()
                val eval = ClinicalEvaluator.evaluateBloodSugar(valNum, item.unit.ifBlank { "mmol/L" }, item.notes, item.whenTaken)
                holder.tvSeverityBadge?.text = "${eval.badgeIcon} ${eval.classification}"
                holder.tvSeverityBadge?.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.tvSeverityBadge?.visibility = View.GONE
            }
        } else {
            holder.tvSeverityBadge?.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = logs.size
}
