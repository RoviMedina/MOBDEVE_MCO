package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
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
            if (receiptId == -1L) {
                Toast.makeText(this, "Receipt not found.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ReviewReceiptActivity::class.java).apply {
                putExtra(ReviewReceiptActivity.EXTRA_RECEIPT_ID, receiptId)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnDeleteReceipt).setOnClickListener {
            showDeleteConfirmation()
        }
    }

    override fun onResume() {
        super.onResume()
        loadReceiptDetails()
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

        loadReceiptImage(receipt.imageUri)
    }

    private fun loadReceiptImage(imageUri: String?) {
        val ivReceiptImage = findViewById<ImageView>(R.id.ivReceiptImage)
        val tvReceiptImagePlaceholder = findViewById<TextView>(R.id.tvReceiptImagePlaceholder)

        if (imageUri.isNullOrBlank()) {
            ivReceiptImage.visibility = View.GONE
            tvReceiptImagePlaceholder.visibility = View.VISIBLE
            return
        }

        try {
            ivReceiptImage.setImageURI(Uri.parse(imageUri))
            ivReceiptImage.visibility = View.VISIBLE
            tvReceiptImagePlaceholder.visibility = View.GONE
        } catch (exception: Exception) {
            ivReceiptImage.visibility = View.GONE
            tvReceiptImagePlaceholder.visibility = View.VISIBLE
            tvReceiptImagePlaceholder.text = "Could not load receipt image"
        }
    }

    private fun showDeleteConfirmation() {
        if (receiptId == -1L) {
            Toast.makeText(this, "Receipt not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_receipt, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteReceipt()
            }
            .show()
    }

    private fun deleteReceipt() {
        val deleted = receiptDatabaseHelper.deleteReceipt(receiptId)
        if (!deleted) {
            Toast.makeText(this, "Could not delete receipt.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Receipt deleted.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_RECEIPT_ID = "extra_receipt_id"
    }
}
