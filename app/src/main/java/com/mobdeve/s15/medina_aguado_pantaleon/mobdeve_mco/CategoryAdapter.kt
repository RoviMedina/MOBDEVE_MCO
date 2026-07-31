package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
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
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryColor = itemView.findViewById<TextView>(R.id.tvCategoryColor)
        private val tvCategoryName = itemView.findViewById<TextView>(R.id.tvCategoryName)
        private val btnEditCategory = itemView.findViewById<Button>(R.id.btnEditCategory)
        private val btnDeleteCategory = itemView.findViewById<Button>(R.id.btnDeleteCategory)

        fun bind(category: Category) {
            tvCategoryColor.setBackgroundColor(category.color)
            tvCategoryName.text = category.name

            btnEditCategory.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(position)
                }
            }

            btnDeleteCategory.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }
    }
}
