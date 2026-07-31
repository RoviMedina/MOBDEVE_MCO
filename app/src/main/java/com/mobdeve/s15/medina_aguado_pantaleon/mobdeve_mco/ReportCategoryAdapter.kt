package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ReportCategoryAdapter :
    RecyclerView.Adapter<ReportCategoryAdapter.ReportCategoryViewHolder>() {

    private var categories: List<Pair<String, Double>> = emptyList()
    private var monthlyTotal: Double = 0.0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_category, parent, false)
        return ReportCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportCategoryViewHolder, position: Int) {
        holder.bind(categories[position], monthlyTotal)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun submitList(updatedCategories: List<Pair<String, Double>>, updatedMonthlyTotal: Double) {
        categories = updatedCategories
        monthlyTotal = updatedMonthlyTotal
        notifyDataSetChanged()
    }

    inner class ReportCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReportCategory: TextView = itemView.findViewById(R.id.tvReportCategory)
        private val tvReportPercent: TextView = itemView.findViewById(R.id.tvReportPercent)
        private val tvReportAmount: TextView = itemView.findViewById(R.id.tvReportAmount)

        fun bind(categoryTotal: Pair<String, Double>, total: Double) {
            val percent = if (total > 0.0) {
                (categoryTotal.second / total) * 100.0
            } else {
                0.0
            }

            tvReportCategory.text = categoryTotal.first
            tvReportPercent.text = String.format(Locale.US, "%.0f%%", percent)
            tvReportAmount.text = String.format(Locale.US, "PHP %.2f", categoryTotal.second)
        }
    }
}
