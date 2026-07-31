package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReceiptDetailsActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private var receiptId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.receipt_details_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        receiptId = intent.getLongExtra(EXTRA_RECEIPT_ID, -1L)
        loadReceiptDetails()

        findViewById<Button>(R.id.btnEditReceipt).setOnClickListener {
            // TODO: Pass the selected receipt id to ReviewReceiptActivity for editing.
            startActivity(Intent(this, ReviewReceiptActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeleteReceipt).setOnClickListener {
            // TODO: Delete the selected receipt from SQLite after adding a confirmation dialog.
            Toast.makeText(this, "TODO: Add delete confirmation and database removal.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReceiptDetails() {
        val receipt = receiptDatabaseHelper.getReceiptById(receiptId)
        if (receipt == null) {
            Toast.makeText(this, "Receipt not found.", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<TextView>(R.id.tvStoreName).text = "Store: ${receipt.storeName}"
        findViewById<TextView>(R.id.tvReceiptDate).text = "Date: ${receipt.receiptDate}"
        findViewById<TextView>(R.id.tvReceiptCategory).text = "Category: ${receipt.category}"
        findViewById<TextView>(R.id.tvReceiptTotal).text =
            String.format(Locale.US, "Total: PHP %.2f", receipt.totalAmount)
        findViewById<TextView>(R.id.tvReceiptItems).text = receipt.items

        if (!receipt.imageUri.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvReceiptImagePlaceholder).text = "Receipt image saved from scan/gallery"
        }
    }

    companion object {
        const val EXTRA_RECEIPT_ID = "extra_receipt_id"
    }
}
