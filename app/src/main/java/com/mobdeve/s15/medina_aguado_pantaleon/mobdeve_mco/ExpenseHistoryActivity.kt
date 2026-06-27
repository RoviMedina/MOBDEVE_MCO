package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ExpenseHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.expense_history_activity)

        val btnViewReceiptDetails = findViewById<Button>(R.id.btnViewReceiptDetails)

        btnViewReceiptDetails.setOnClickListener {
            val intent = Intent(this, ReceiptDetailsActivity::class.java)
            startActivity(intent)
        }
    }
}