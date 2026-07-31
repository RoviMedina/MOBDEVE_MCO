package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import java.util.Locale
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
        val tvEmptyRecentReceipts = findViewById<TextView>(R.id.tvEmptyRecentReceipts)

        val totalExpenses = dbHelper.getTotalExpenses()
        val receiptCount = dbHelper.getReceiptCount()

        tvTotalExpenses.text = String.format(
            Locale.getDefault(),
            "₱%,.2f",
            totalExpenses
        )

        tvReceiptCount.text = "$receiptCount Receipts This Month"

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
}