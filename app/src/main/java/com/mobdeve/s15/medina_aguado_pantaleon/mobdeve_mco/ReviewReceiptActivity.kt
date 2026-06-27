package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ReviewReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.review_receipt_activity)

        val btnSaveReceipt = findViewById<Button>(R.id.btnSaveReceipt)

        btnSaveReceipt.setOnClickListener {
            val intent = Intent(this, ExpenseHistoryActivity::class.java)
            startActivity(intent)
        }
    }
}