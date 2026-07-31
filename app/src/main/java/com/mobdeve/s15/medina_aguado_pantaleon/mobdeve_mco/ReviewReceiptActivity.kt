package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReviewReceiptActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.review_receipt_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)

        val etStoreName = findViewById<EditText>(R.id.etStoreName)
        val etReceiptDate = findViewById<EditText>(R.id.etReceiptDate)
        val etTotalAmount = findViewById<EditText>(R.id.etTotalAmount)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val tvReceiptItems = findViewById<TextView>(R.id.tvReceiptItems)
        val tvOcrRawText = findViewById<TextView>(R.id.tvOcrRawText)

        etStoreName.setText(
            intent.getStringExtra(EXTRA_STORE_NAME) ?: "Jollibee"
        )
        etReceiptDate.setText(
            intent.getStringExtra(EXTRA_RECEIPT_DATE) ?: "June 27, 2026"
        )
        etTotalAmount.setText(
            intent.getStringExtra(EXTRA_TOTAL_AMOUNT) ?: "250.00"
        )
        etCategory.setText(
            intent.getStringExtra(EXTRA_CATEGORY) ?: "Food"
        )
        tvReceiptItems.text =
            intent.getStringExtra(EXTRA_ITEMS)
                ?: "Chickenjoy - PHP 120.00\nBurger Steak - PHP 100.00\nDrink - PHP 30.00"
        tvOcrRawText.text =
            intent.getStringExtra(EXTRA_RAW_TEXT)?.ifBlank { "No OCR text detected." }
                ?: "OCR raw text will appear here after scanning."

        val btnSaveReceipt = findViewById<Button>(R.id.btnSaveReceipt)

        btnSaveReceipt.setOnClickListener {
            val receiptId = receiptDatabaseHelper.insertReceipt(
                storeName = etStoreName.text.toString().ifBlank { "Unknown Store" },
                receiptDate = etReceiptDate.text.toString().ifBlank { "Date not detected" },
                category = etCategory.text.toString().ifBlank { "Uncategorized" },
                totalAmount = etTotalAmount.text.toString().toDoubleOrNull() ?: 0.0,
                items = tvReceiptItems.text.toString(),
                rawText = tvOcrRawText.text.toString(),
                imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
            )

            if (receiptId == -1L) {
                Toast.makeText(this, "Could not save receipt.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Receipt saved.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_STORE_NAME = "extra_store_name"
        const val EXTRA_RECEIPT_DATE = "extra_receipt_date"
        const val EXTRA_TOTAL_AMOUNT = "extra_total_amount"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_ITEMS = "extra_items"
        const val EXTRA_RAW_TEXT = "extra_raw_text"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
