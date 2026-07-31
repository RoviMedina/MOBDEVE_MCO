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
    private var editingReceiptId: Long = -1L
    private var existingImageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.review_receipt_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        editingReceiptId = intent.getLongExtra(EXTRA_RECEIPT_ID, -1L)

        val etStoreName = findViewById<EditText>(R.id.etStoreName)
        val etReceiptDate = findViewById<EditText>(R.id.etReceiptDate)
        val etTotalAmount = findViewById<EditText>(R.id.etTotalAmount)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val tvReceiptItems = findViewById<TextView>(R.id.tvReceiptItems)
        val tvOcrRawText = findViewById<TextView>(R.id.tvOcrRawText)

        val existingReceipt = receiptDatabaseHelper.getReceiptById(editingReceiptId)
        existingImageUri = existingReceipt?.imageUri

        etStoreName.setText(intent.getStringExtra(EXTRA_STORE_NAME) ?: existingReceipt?.storeName ?: "Jollibee")
        etReceiptDate.setText(intent.getStringExtra(EXTRA_RECEIPT_DATE) ?: existingReceipt?.receiptDate ?: "June 27, 2026")
        etTotalAmount.setText(intent.getStringExtra(EXTRA_TOTAL_AMOUNT) ?: existingReceipt?.totalAmount?.toString() ?: "250.00")
        etCategory.setText(intent.getStringExtra(EXTRA_CATEGORY) ?: existingReceipt?.category ?: "Food")
        tvReceiptItems.text = intent.getStringExtra(EXTRA_ITEMS)
            ?: existingReceipt?.items
            ?: "Chickenjoy - PHP 120.00\nBurger Steak - PHP 100.00\nDrink - PHP 30.00"
        tvOcrRawText.text = intent.getStringExtra(EXTRA_RAW_TEXT)?.ifBlank { "No OCR text detected." }
            ?: existingReceipt?.rawText
            ?: "OCR raw text will appear here after scanning."

        val btnSaveReceipt = findViewById<Button>(R.id.btnSaveReceipt)
        btnSaveReceipt.text = if (isEditMode()) "Update Receipt" else "Save Receipt"

        btnSaveReceipt.setOnClickListener {
            val storeName = etStoreName.text.toString().ifBlank { "Unknown Store" }
            val receiptDate = etReceiptDate.text.toString().ifBlank { "Date not detected" }
            val category = etCategory.text.toString().ifBlank { "Uncategorized" }
            val totalAmount = etTotalAmount.text.toString().toDoubleOrNull() ?: 0.0
            val items = tvReceiptItems.text.toString()
            val rawText = tvOcrRawText.text.toString()
            val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI) ?: existingImageUri

            if (isEditMode()) {
                val updated = receiptDatabaseHelper.updateReceipt(
                    id = editingReceiptId,
                    storeName = storeName,
                    receiptDate = receiptDate,
                    category = category,
                    totalAmount = totalAmount,
                    items = items,
                    rawText = rawText,
                    imageUri = imageUri
                )

                if (!updated) {
                    Toast.makeText(this, "Could not update receipt.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Toast.makeText(this, "Receipt updated.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ReceiptDetailsActivity::class.java).apply {
                    putExtra(ReceiptDetailsActivity.EXTRA_RECEIPT_ID, editingReceiptId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } else {
                val receiptId = receiptDatabaseHelper.insertReceipt(
                    storeName = storeName,
                    receiptDate = receiptDate,
                    category = category,
                    totalAmount = totalAmount,
                    items = items,
                    rawText = rawText,
                    imageUri = imageUri
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
    }

    private fun isEditMode(): Boolean {
        return editingReceiptId != -1L
    }

    companion object {
        const val EXTRA_RECEIPT_ID = "extra_receipt_id"
        const val EXTRA_STORE_NAME = "extra_store_name"
        const val EXTRA_RECEIPT_DATE = "extra_receipt_date"
        const val EXTRA_TOTAL_AMOUNT = "extra_total_amount"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_ITEMS = "extra_items"
        const val EXTRA_RAW_TEXT = "extra_raw_text"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
