package com.example.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import java.util.Calendar

data class CalendarDayItem(
    val dayNumber: Int,
    val fullDateString: String,
    val calendar: Calendar?,
    val isCurrentMonth: Boolean,
    val status: DayAdherenceStatus,
    val isSelected: Boolean
)

enum class DayAdherenceStatus {
    ALL_TAKEN,      // Green ✓
    PARTIAL_PENDING,// Amber/Orange ⏰
    MISSED,         // Amber/Orange ⏰
    NO_DOSES        // None
}

class CalendarGridXmlAdapter(
    private var items: List<CalendarDayItem> = emptyList(),
    private val onDaySelected: (CalendarDayItem) -> Unit
) : RecyclerView.Adapter<CalendarGridXmlAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutCellBg: View = itemView.findViewById(R.id.layout_day_cell_bg)
        val tvDayNum: TextView = itemView.findViewById(R.id.tv_day_number)
        val tvBadge: TextView = itemView.findViewById(R.id.tv_day_status_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.dayNumber <= 0 || !item.isCurrentMonth) {
            holder.tvDayNum.text = ""
            holder.tvBadge.visibility = View.GONE
            holder.layoutCellBg.setBackgroundResource(0)
            holder.itemView.isClickable = false
            return
        }

        holder.itemView.isClickable = true
        holder.tvDayNum.text = "${item.dayNumber}"

        if (item.isSelected) {
            holder.layoutCellBg.setBackgroundResource(R.drawable.bg_button_teal)
            holder.tvDayNum.setTextColor(Color.WHITE)
            holder.tvBadge.visibility = View.VISIBLE
            when (item.status) {
                DayAdherenceStatus.ALL_TAKEN -> {
                    holder.tvBadge.text = "✓ Taken"
                    holder.tvBadge.setTextColor(Color.WHITE)
                    holder.tvBadge.setBackgroundResource(0)
                }
                DayAdherenceStatus.PARTIAL_PENDING, DayAdherenceStatus.MISSED -> {
                    holder.tvBadge.text = "⏰ Not Taken"
                    holder.tvBadge.setTextColor(Color.WHITE)
                    holder.tvBadge.setBackgroundResource(0)
                }
                DayAdherenceStatus.NO_DOSES -> {
                    holder.tvBadge.visibility = View.GONE
                }
            }
        } else {
            when (item.status) {
                DayAdherenceStatus.ALL_TAKEN -> {
                    holder.layoutCellBg.setBackgroundResource(R.drawable.bg_badge_green)
                    holder.tvDayNum.setTextColor(Color.parseColor("#10B981"))
                    holder.tvBadge.visibility = View.VISIBLE
                    holder.tvBadge.text = "✓ Taken"
                    holder.tvBadge.setTextColor(Color.parseColor("#10B981"))
                    holder.tvBadge.setBackgroundResource(0)
                }
                DayAdherenceStatus.PARTIAL_PENDING, DayAdherenceStatus.MISSED -> {
                    holder.layoutCellBg.setBackgroundResource(R.drawable.bg_badge_amber)
                    holder.tvDayNum.setTextColor(Color.parseColor("#F59E0B"))
                    holder.tvBadge.visibility = View.VISIBLE
                    holder.tvBadge.text = "⏰ Not Taken"
                    holder.tvBadge.setTextColor(Color.parseColor("#F59E0B"))
                    holder.tvBadge.setBackgroundResource(0)
                }
                DayAdherenceStatus.NO_DOSES -> {
                    holder.layoutCellBg.setBackgroundResource(R.drawable.bg_card_elevated)
                    holder.tvDayNum.setTextColor(Color.parseColor("#9CA3AF"))
                    holder.tvBadge.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener {
            onDaySelected(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CalendarDayItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
