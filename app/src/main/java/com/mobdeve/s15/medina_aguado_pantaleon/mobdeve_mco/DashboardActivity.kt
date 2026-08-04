package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DashboardActivity : AppCompatActivity() {
    private lateinit var dbHelper: ReceiptDatabaseHelper
    private lateinit var receiptAdapter: ReceiptAdapter
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvReceiptCount: TextView
    private lateinit var tvDashboardBudget: TextView
    private lateinit var progressDashboardBudget: ProgressBar
    private lateinit var tvEmptyRecentReceipts: TextView
    private lateinit var rvRecentReceipts: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        dbHelper = ReceiptDatabaseHelper(this)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvReceiptCount = findViewById(R.id.tvReceiptCount)
        tvDashboardBudget = findViewById(R.id.tvDashboardBudget)
        progressDashboardBudget = findViewById(R.id.progressDashboardBudget)
        tvEmptyRecentReceipts = findViewById(R.id.tvEmptyRecentReceipts)
        rvRecentReceipts = findViewById(R.id.rvRecentReceipts)

        receiptAdapter = ReceiptAdapter(emptyList()) { receipt ->
            val intent = Intent(this, ReceiptDetailsActivity::class.java).apply {
                putExtra(ReceiptDetailsActivity.EXTRA_RECEIPT_ID, receipt.id)
            }
            startActivity(intent)
        }

        rvRecentReceipts.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = receiptAdapter
        }

        val btnScanReceipt = findViewById<Button>(R.id.btnScanReceipt)
        val btnExpenseHistory = findViewById<Button>(R.id.btnExpenseHistory)

        btnScanReceipt.setOnClickListener {
            val intent = Intent(this, ScanReceiptActivity::class.java)
            startActivity(intent)
        }

        btnExpenseHistory.setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        val btnReports = findViewById<Button>(R.id.btnReports)

        btnReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }

        val btnProfile = findViewById<Button>(R.id.btnProfile)

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val totalExpenses = dbHelper.getTotalExpenses()
        val receiptCount = dbHelper.getReceiptCount()
        val monthlyBudget = getMonthlyBudget()
        val budgetPercent = budgetPercent(totalExpenses, monthlyBudget)
        val recentReceipts = dbHelper.getRecentReceipts(3)

        tvTotalExpenses.text = MoneyFormatter.format(this, totalExpenses)
        tvReceiptCount.text = "$receiptCount Receipts This Month"
        progressDashboardBudget.progress = budgetPercent
        tvDashboardBudget.text = "Budget used: $budgetPercent% of ${MoneyFormatter.format(this, monthlyBudget)}"
        receiptAdapter.submitList(recentReceipts)

        if (recentReceipts.isEmpty()) {
            tvEmptyRecentReceipts.visibility = View.VISIBLE
            rvRecentReceipts.visibility = View.GONE
        } else {
            tvEmptyRecentReceipts.visibility = View.GONE
            rvRecentReceipts.visibility = View.VISIBLE
        }
    }

    private fun getMonthlyBudget(): Double {
        return getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("monthly_budget", "5000.00")
            ?.toDoubleOrNull()
            ?: 5000.00
    }

    private fun budgetPercent(totalExpenses: Double, monthlyBudget: Double): Int {
        if (monthlyBudget <= 0.0) {
            return 0
        }

        return ((totalExpenses / monthlyBudget) * 100)
            .toInt()
            .coerceIn(0, 100)
    }
}
