package com.passingguest.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.passingguest.myapplication.R
import com.passingguest.myapplication.data.entity.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun setSelected(categoryId: Long) {
        val position = categories.indexOfFirst { it.id == categoryId }
        if (position >= 0) {
            selectedPosition = position
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, position == selectedPosition)
    }

    override fun getItemCount() = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(category: Category, isSelected: Boolean) {
            tvName.text = category.name
            tvIcon.text = category.name.first().toString()
            tvIcon.setBackgroundResource(
                if (isSelected) R.drawable.circle_icon_background
                else R.drawable.circle_icon_background
            )
            tvIcon.alpha = if (isSelected) 1.0f else 0.6f

            itemView.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category)
            }
        }
    }
}
