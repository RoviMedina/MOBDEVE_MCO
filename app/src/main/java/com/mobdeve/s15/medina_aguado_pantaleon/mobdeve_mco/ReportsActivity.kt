package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private lateinit var spinnerReportMonth: Spinner
    private lateinit var pieCategoryExpenses: PieChart
    private lateinit var barMonthlySpending: BarChart
    private lateinit var tvMonthlyTotal: TextView
    private lateinit var tvHighestCategory: TextView
    private lateinit var reportCategoryAdapter: ReportCategoryAdapter

    private var receipts: List<Receipt> = emptyList()
    private var monthOptions: List<MonthOption> = emptyList()
    private var selectedMonthKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reports_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        spinnerReportMonth = findViewById(R.id.spinnerReportMonth)
        pieCategoryExpenses = findViewById(R.id.pieCategoryExpenses)
        barMonthlySpending = findViewById(R.id.barMonthlySpending)
        tvMonthlyTotal = findViewById(R.id.tvMonthlyTotal)
        tvHighestCategory = findViewById(R.id.tvHighestCategory)
        reportCategoryAdapter = ReportCategoryAdapter()

        findViewById<RecyclerView>(R.id.rvReportCategories).apply {
            layoutManager = LinearLayoutManager(this@ReportsActivity)
            adapter = reportCategoryAdapter
            isNestedScrollingEnabled = false
        }

        loadReportData()
    }

    override fun onResume() {
        super.onResume()
        loadReportData()
    }

    private fun loadReportData() {
        receipts = receiptDatabaseHelper.getAllReceipts()
        monthOptions = buildMonthOptions(receipts)

        if (monthOptions.isEmpty()) {
            selectedMonthKey = null
            spinnerReportMonth.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("No saved receipts")
            )
            updateReportsForSelectedMonth()
            return
        }

        val previousSelection = selectedMonthKey
        selectedMonthKey = previousSelection
            ?.takeIf { key -> monthOptions.any { it.key == key } }
            ?: monthOptions.first().key

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            monthOptions.map { it.label }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerReportMonth.adapter = adapter
        spinnerReportMonth.setSelection(monthOptions.indexOfFirst { it.key == selectedMonthKey }.coerceAtLeast(0))
        spinnerReportMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonthKey = monthOptions.getOrNull(position)?.key
                updateReportsForSelectedMonth()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        updateReportsForSelectedMonth()
    }

    private fun updateReportsForSelectedMonth() {
        val selectedReceipts = selectedMonthKey?.let { monthKey ->
            receipts.filter { receipt -> monthKeyForReceipt(receipt) == monthKey }
        }.orEmpty()

        val selectedMonthLabel = monthOptions.firstOrNull { it.key == selectedMonthKey }?.label ?: "Selected Month"
        val totalExpenses = selectedReceipts.sumOf { it.totalAmount }
        val categoryTotals = selectedReceipts
            .groupBy { it.category.ifBlank { "Uncategorized" } }
            .mapValues { (_, categoryReceipts) -> categoryReceipts.sumOf { it.totalAmount } }
            .toList()
            .sortedByDescending { it.second }

        val dateTotals = selectedReceipts
            .groupBy { displayDateForReceipt(it) }
            .mapValues { (_, dateReceipts) -> dateReceipts.sumOf { it.totalAmount } }
            .toList()
            .sortedBy { it.first }

        tvMonthlyTotal.text = String.format(Locale.US, "Total for %s\nPHP %.2f", selectedMonthLabel, totalExpenses)
        tvHighestCategory.text = if (categoryTotals.isEmpty()) {
            "Highest Category\nNone"
        } else {
            val highestCategory = categoryTotals.first()
            String.format(Locale.US, "Highest Category\n%s - PHP %.2f", highestCategory.first, highestCategory.second)
        }

        setupPieChart(categoryTotals, selectedMonthLabel)
        setupBarChart(dateTotals)
        reportCategoryAdapter.submitList(categoryTotals, totalExpenses)
    }

    private fun setupPieChart(categoryTotals: List<Pair<String, Double>>, selectedMonthLabel: String) {
        if (categoryTotals.isEmpty()) {
            pieCategoryExpenses.clear()
            pieCategoryExpenses.centerText = "No expenses for $selectedMonthLabel"
            pieCategoryExpenses.invalidate()
            return
        }

        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            sliceSpace = 2f
        }

        pieCategoryExpenses.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(false)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            centerText = selectedMonthLabel
            setCenterTextSize(14f)
            animateY(CHART_ANIMATION_MS)
            invalidate()
        }
    }

    private fun setupBarChart(dateTotals: List<Pair<String, Double>>) {
        if (dateTotals.isEmpty()) {
            barMonthlySpending.clear()
            barMonthlySpending.description.text = "No expenses yet"
            barMonthlySpending.invalidate()
            return
        }

        val labels = dateTotals.map { it.first }
        val entries = dateTotals.mapIndexed { index, (_, total) ->
            BarEntry(index.toFloat(), total.toFloat())
        }

        val dataSet = BarDataSet(entries, "Expenses").apply {
            colors = chartColors
            valueTextColor = Color.DKGRAY
            valueTextSize = 11f
        }

        barMonthlySpending.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.55f
            }
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -30f
            }
            setFitBars(true)
            animateY(CHART_ANIMATION_MS)
            invalidate()
        }
    }

    private fun buildMonthOptions(receipts: List<Receipt>): List<MonthOption> {
        return receipts
            .map { receipt ->
                val monthDate = monthDateForReceipt(receipt)
                MonthOption(
                    key = monthKeyFormat.format(monthDate),
                    label = monthLabelFormat.format(monthDate),
                    sortTime = monthDate.time
                )
            }
            .distinctBy { it.key }
            .sortedByDescending { it.sortTime }
    }

    private fun monthKeyForReceipt(receipt: Receipt): String {
        return monthKeyFormat.format(monthDateForReceipt(receipt))
    }

    private fun monthDateForReceipt(receipt: Receipt): Date {
        val parsedDate = parseReceiptDate(receipt.receiptDate)
        val calendar = Calendar.getInstance()
        calendar.time = parsedDate ?: Date(receipt.createdAt)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun displayDateForReceipt(receipt: Receipt): String {
        return receipt.receiptDate.ifBlank {
            dayLabelFormat.format(Date(receipt.createdAt))
        }
    }

    private fun parseReceiptDate(receiptDate: String): Date? {
        val cleanedDate = receiptDate.trim()
        if (cleanedDate.isBlank()) return null

        if (cleanedDate.equals("today", ignoreCase = true)) {
            return Date()
        }

        if (cleanedDate.equals("yesterday", ignoreCase = true)) {
            return Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.time
        }

        return dateFormats.firstNotNullOfOrNull { format ->
            runCatching { format.parse(cleanedDate) }.getOrNull()
        } ?: parseMonthDay(cleanedDate)
    }

    private fun parseMonthDay(receiptDate: String): Date? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return runCatching {
            monthDayFormat.parse("$receiptDate $currentYear")
        }.getOrNull()
    }

    private data class MonthOption(
        val key: String,
        val label: String,
        val sortTime: Long
    )

    companion object {
        private const val CHART_ANIMATION_MS = 700
        private val chartColors = listOf(
            Color.rgb(103, 80, 164),
            Color.rgb(76, 175, 80),
            Color.rgb(244, 67, 54),
            Color.rgb(33, 150, 243),
            Color.rgb(255, 193, 7),
            Color.rgb(0, 150, 136)
        ) + ColorTemplate.MATERIAL_COLORS.toList()

        private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        private val dayLabelFormat = SimpleDateFormat("MMM d", Locale.US)
        private val monthDayFormat = SimpleDateFormat("MMMM d yyyy", Locale.US).apply {
            isLenient = false
        }
        private val dateFormats = listOf(
            "MMMM d, yyyy",
            "MMMM d yyyy",
            "MMM d, yyyy",
            "MMM d yyyy",
            "MM/dd/yyyy",
            "M/d/yyyy",
            "MM-dd-yyyy",
            "M-d-yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        ).map { pattern ->
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }
        }
    }
}
