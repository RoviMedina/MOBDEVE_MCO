package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        val btnScanReceipt = findViewById<Button>(R.id.btnScanReceipt)

        btnScanReceipt.setOnClickListener {
            val intent = Intent(this, ScanReceiptActivity::class.java)
            startActivity(intent)
        }

        val btnReports = findViewById<Button>(R.id.btnReports)

        btnReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }
    }
}