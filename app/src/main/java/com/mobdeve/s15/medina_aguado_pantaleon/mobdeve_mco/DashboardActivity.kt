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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        val dbHelper = ReceiptDatabaseHelper(this)

        val tvTotalExpenses = findViewById<TextView>(R.id.tvTotalExpenses)
        val tvReceiptCount = findViewById<TextView>(R.id.tvReceiptCount)
        val tvDashboardBudget = findViewById<TextView>(R.id.tvDashboardBudget)
        val progressDashboardBudget = findViewById<ProgressBar>(R.id.progressDashboardBudget)
        val tvEmptyRecentReceipts = findViewById<TextView>(R.id.tvEmptyRecentReceipts)

        val totalExpenses = dbHelper.getTotalExpenses()
        val receiptCount = dbHelper.getReceiptCount()
        val monthlyBudget = getMonthlyBudget()
        val budgetPercent = budgetPercent(totalExpenses, monthlyBudget)

        tvTotalExpenses.text = MoneyFormatter.format(this, totalExpenses)
        tvReceiptCount.text = "$receiptCount Receipts This Month"
        progressDashboardBudget.progress = budgetPercent
        tvDashboardBudget.text = "Budget used: $budgetPercent% of ${MoneyFormatter.format(this, monthlyBudget)}"

        val receiptAdapter = ReceiptAdapter(emptyList()) { receipt ->
            val intent = Intent(this, ReceiptDetailsActivity::class.java).apply {
                putExtra(ReceiptDetailsActivity.EXTRA_RECEIPT_ID, receipt.id)
            }
            startActivity(intent)
        }

        findViewById<RecyclerView>(R.id.rvRecentReceipts).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = receiptAdapter
        }

        val recentReceipts = dbHelper.getRecentReceipts(3)
        receiptAdapter.submitList(recentReceipts)

        tvEmptyRecentReceipts.visibility =
            if (recentReceipts.isEmpty()) View.VISIBLE else View.GONE

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
