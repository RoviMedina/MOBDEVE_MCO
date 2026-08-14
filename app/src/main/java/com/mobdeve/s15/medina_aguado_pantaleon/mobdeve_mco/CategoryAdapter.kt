package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val onCategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun replaceItems(newCategories: List<Category>) {
        val updatedCategories = newCategories.toList()
        categories.clear()
        categories.addAll(updatedCategories)
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryColor = itemView.findViewById<TextView>(R.id.tvCategoryColor)
        private val tvCategoryName = itemView.findViewById<TextView>(R.id.tvCategoryName)

        fun bind(category: Category) {
            tvCategoryColor.setBackgroundColor(category.color)
            tvCategoryName.text = category.name

            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCategoryClick(position)
                }
            }
        }
    }
}
