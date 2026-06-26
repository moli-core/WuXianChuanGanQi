package com.passingguest.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.passingguest.myapplication.R
import com.passingguest.myapplication.util.CurrencyUtils

data class BudgetDisplayItem(
    val budgetId: Long,
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val budgetAmount: Double,
    val spentAmount: Double
) {
    val progressPercent: Float
        get() = if (budgetAmount > 0) (spentAmount / budgetAmount * 100).toFloat() else 0f

    val isOverBudget: Boolean
        get() = spentAmount > budgetAmount
}

class BudgetAdapter : ListAdapter<BudgetDisplayItem, BudgetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvBudgetStatus: TextView = itemView.findViewById(R.id.tvBudgetStatus)
        private val tvProgressPercent: TextView = itemView.findViewById(R.id.tvProgressPercent)
        private val progressBudget: ProgressBar = itemView.findViewById(R.id.progressBudget)

        fun bind(item: BudgetDisplayItem) {
            tvCategoryIcon.text = item.categoryIcon
            tvCategoryName.text = item.categoryName

            val spentStr = CurrencyUtils.format(item.spentAmount)
            val budgetStr = CurrencyUtils.format(item.budgetAmount)
            tvBudgetStatus.text = "$spentStr / $budgetStr"

            val percent = item.progressPercent.toInt()
            tvProgressPercent.text = "$percent%"
            progressBudget.progress = percent.coerceIn(0, 100)

            val colorRes = if (item.isOverBudget) R.color.expense_color
            else android.R.color.holo_orange_dark
            progressBudget.progressDrawable.setTint(itemView.context.getColor(colorRes))
            tvProgressPercent.setTextColor(itemView.context.getColor(colorRes))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BudgetDisplayItem>() {
        override fun areItemsTheSame(oldItem: BudgetDisplayItem, newItem: BudgetDisplayItem): Boolean {
            return oldItem.budgetId == newItem.budgetId
        }

        override fun areContentsTheSame(oldItem: BudgetDisplayItem, newItem: BudgetDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
