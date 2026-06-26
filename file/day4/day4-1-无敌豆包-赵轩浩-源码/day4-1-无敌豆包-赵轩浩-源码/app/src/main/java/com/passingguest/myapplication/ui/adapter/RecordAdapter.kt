package com.passingguest.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.passingguest.myapplication.R
import com.passingguest.myapplication.util.CurrencyUtils
import com.passingguest.myapplication.util.DateUtils

data class DisplayItem(
    val type: Int, // TYPE_HEADER or TYPE_RECORD
    val date: Long = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recordId: Long = 0,
    val categoryName: String = "",
    val categoryIcon: String = "",
    val amount: Double = 0.0,
    val note: String? = null,
    val recordType: String = ""
) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_RECORD = 1
    }
}

class RecordAdapter(
    private val onRecordClick: (Long) -> Unit,
    private val onRecordLongClick: (Long) -> Unit
) : ListAdapter<DisplayItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            DisplayItem.TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_group, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_record, parent, false)
                RecordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is RecordViewHolder -> holder.bind(item)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDayTotal: TextView = itemView.findViewById(R.id.tvDayTotal)

        fun bind(item: DisplayItem) {
            tvDate.text = DateUtils.formatDisplayDate(item.date)
            val incomeStr = CurrencyUtils.format(item.totalIncome)
            val expenseStr = CurrencyUtils.format(item.totalExpense)
            tvDayTotal.text = "收入: $incomeStr  支出: $expenseStr"
        }
    }

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val cardView: androidx.cardview.widget.CardView = itemView as androidx.cardview.widget.CardView

        fun bind(item: DisplayItem) {
            tvCategoryIcon.text = item.categoryIcon
            tvCategoryName.text = item.categoryName
            tvNote.text = item.note ?: ""
            tvAmount.text = CurrencyUtils.format(item.amount)
            tvAmount.setTextColor(
                if (item.recordType == "INCOME")
                    itemView.context.getColor(R.color.income_color)
                else
                    itemView.context.getColor(R.color.expense_color)
            )

            cardView.setOnClickListener {
                onRecordClick(item.recordId)
            }
            cardView.setOnLongClickListener {
                onRecordLongClick(item.recordId)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
            return if (oldItem.type == DisplayItem.TYPE_HEADER) {
                oldItem.type == newItem.type && oldItem.date == newItem.date
            } else {
                oldItem.recordId == newItem.recordId
            }
        }

        override fun areContentsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
