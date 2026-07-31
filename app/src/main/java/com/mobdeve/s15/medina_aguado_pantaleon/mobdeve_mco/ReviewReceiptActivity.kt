package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReviewReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.review_receipt_activity)

        findViewById<EditText>(R.id.etStoreName).setText(
            intent.getStringExtra(EXTRA_STORE_NAME) ?: "Jollibee"
        )
        findViewById<EditText>(R.id.etReceiptDate).setText(
            intent.getStringExtra(EXTRA_RECEIPT_DATE) ?: "June 27, 2026"
        )
        findViewById<EditText>(R.id.etTotalAmount).setText(
            intent.getStringExtra(EXTRA_TOTAL_AMOUNT) ?: "250.00"
        )
        findViewById<EditText>(R.id.etCategory).setText(
            intent.getStringExtra(EXTRA_CATEGORY) ?: "Food"
        )
        findViewById<TextView>(R.id.tvReceiptItems).text =
            intent.getStringExtra(EXTRA_ITEMS)
                ?: "Chickenjoy - PHP 120.00\nBurger Steak - PHP 100.00\nDrink - PHP 30.00"
        findViewById<TextView>(R.id.tvOcrRawText).text =
            intent.getStringExtra(EXTRA_RAW_TEXT)?.ifBlank { "No OCR text detected." }
                ?: "OCR raw text will appear here after scanning."

        val btnSaveReceipt = findViewById<Button>(R.id.btnSaveReceipt)

        btnSaveReceipt.setOnClickListener {
            // TODO: Save edited receipt fields to SQLite before opening history.
            val intent = Intent(this, ExpenseHistoryActivity::class.java)
            startActivity(intent)
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
