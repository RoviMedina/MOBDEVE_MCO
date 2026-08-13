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
    private var categoryColors: Map<String, Int> = emptyMap()

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

    fun submitList(
        updatedCategories: List<Pair<String, Double>>,
        updatedMonthlyTotal: Double,
        updatedCategoryColors: Map<String, Int>
    ) {
        categories = updatedCategories
        monthlyTotal = updatedMonthlyTotal
        categoryColors = updatedCategoryColors
        notifyDataSetChanged()
    }

    inner class ReportCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReportCategory: TextView = itemView.findViewById(R.id.tvReportCategory)
        private val tvReportCategoryColor: TextView = itemView.findViewById(R.id.tvReportCategoryColor)
        private val tvReportPercent: TextView = itemView.findViewById(R.id.tvReportPercent)
        private val tvReportAmount: TextView = itemView.findViewById(R.id.tvReportAmount)

        fun bind(categoryTotal: Pair<String, Double>, total: Double) {
            val percent = if (total > 0.0) {
                (categoryTotal.second / total) * 100.0
            } else {
                0.0
            }

            tvReportCategoryColor.setBackgroundColor(
                categoryColors[categoryTotal.first] ?: ReceiptDatabaseHelper.fallbackCategoryColor
            )
            tvReportCategory.text = categoryTotal.first
            tvReportPercent.text = String.format(Locale.US, "%.0f%%", percent)
            tvReportAmount.text = MoneyFormatter.format(itemView.context, categoryTotal.second)
        }
    }
}
