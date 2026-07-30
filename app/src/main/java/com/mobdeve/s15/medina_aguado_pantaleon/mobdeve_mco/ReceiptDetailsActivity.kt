package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReceiptDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.receipt_details_activity)

        findViewById<Button>(R.id.btnEditReceipt).setOnClickListener {
            // TODO: Pass the selected receipt id to ReviewReceiptActivity for editing.
            startActivity(Intent(this, ReviewReceiptActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeleteReceipt).setOnClickListener {
            // TODO: Delete the selected receipt from SQLite after adding a confirmation dialog.
            Toast.makeText(this, "TODO: Add delete confirmation and database removal.", Toast.LENGTH_SHORT).show()
        }
    }
}
