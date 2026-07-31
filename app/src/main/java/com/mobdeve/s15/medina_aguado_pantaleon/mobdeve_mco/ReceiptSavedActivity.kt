package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ReceiptSavedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.receipt_saved_activity)

        findViewById<Button>(R.id.btnViewSavedReceipt).setOnClickListener {
            val intent = Intent(this, ExpenseHistoryActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnScanAnotherReceipt).setOnClickListener {
            val intent = Intent(this, ScanReceiptActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
