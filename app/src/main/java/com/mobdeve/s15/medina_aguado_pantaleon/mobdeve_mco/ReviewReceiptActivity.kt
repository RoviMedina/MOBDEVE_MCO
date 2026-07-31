package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class ReviewReceiptActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private lateinit var lineItemAdapter: ReceiptLineItemAdapter
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
        val tvOcrRawText = findViewById<TextView>(R.id.tvOcrRawText)

        val existingReceipt = receiptDatabaseHelper.getReceiptById(editingReceiptId)
        existingImageUri = existingReceipt?.imageUri
        val initialItemText = intent.getStringExtra(EXTRA_ITEMS)
            ?: existingReceipt?.items
            ?: "Chickenjoy - PHP 120.00\nBurger Steak - PHP 100.00\nDrink - PHP 30.00"

        etStoreName.setText(intent.getStringExtra(EXTRA_STORE_NAME) ?: existingReceipt?.storeName ?: "Jollibee")
        etReceiptDate.setText(intent.getStringExtra(EXTRA_RECEIPT_DATE) ?: existingReceipt?.receiptDate ?: "June 27, 2026")
        etTotalAmount.setText(intent.getStringExtra(EXTRA_TOTAL_AMOUNT) ?: existingReceipt?.totalAmount?.toString() ?: "250.00")
        etCategory.setText(intent.getStringExtra(EXTRA_CATEGORY) ?: existingReceipt?.category ?: "Food")
        tvOcrRawText.text = intent.getStringExtra(EXTRA_RAW_TEXT)?.ifBlank { "No OCR text detected." }
            ?: existingReceipt?.rawText
            ?: "OCR raw text will appear here after scanning."

        lineItemAdapter = ReceiptLineItemAdapter(
            parseLineItems(initialItemText).toMutableList(),
            onEditClick = { position -> showLineItemDialog(position) },
            onDeleteClick = { position -> lineItemAdapter.deleteItem(position) }
        )

        findViewById<RecyclerView>(R.id.rvReceiptItems).apply {
            layoutManager = LinearLayoutManager(this@ReviewReceiptActivity)
            adapter = lineItemAdapter
            isNestedScrollingEnabled = false
        }

        findViewById<Button>(R.id.btnAddReceiptItem).setOnClickListener {
            showLineItemDialog()
        }

        val btnSaveReceipt = findViewById<Button>(R.id.btnSaveReceipt)
        btnSaveReceipt.text = if (isEditMode()) "Update Receipt" else "Save Receipt"

        btnSaveReceipt.setOnClickListener {
            val storeName = etStoreName.text.toString().ifBlank { "Unknown Store" }
            val receiptDate = etReceiptDate.text.toString().ifBlank { "Date not detected" }
            val category = etCategory.text.toString().ifBlank { "Uncategorized" }
            val totalAmount = etTotalAmount.text.toString().toDoubleOrNull() ?: 0.0
            val items = formatLineItemsForStorage()
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

                val intent = Intent(this, ReceiptSavedActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun isEditMode(): Boolean {
        return editingReceiptId != -1L
    }

    private fun showLineItemDialog(position: Int? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_line_item, null)
        val tilLineItemName = dialogView.findViewById<TextInputLayout>(R.id.tilLineItemName)
        val tilLineItemAmount = dialogView.findViewById<TextInputLayout>(R.id.tilLineItemAmount)
        val etLineItemName = dialogView.findViewById<TextInputEditText>(R.id.etLineItemName)
        val etLineItemAmount = dialogView.findViewById<TextInputEditText>(R.id.etLineItemAmount)
        val existingItem = position?.let { lineItemAdapter.currentItems().getOrNull(it) }

        etLineItemName.setText(existingItem?.name.orEmpty())
        etLineItemAmount.setText(existingItem?.amount?.takeIf { it > 0.0 }?.toString().orEmpty())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (position == null) "Add" else "Update", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilLineItemName.error = null
                tilLineItemAmount.error = null

                val name = etLineItemName.text?.toString()?.trim().orEmpty()
                val amountText = etLineItemAmount.text?.toString()?.trim().orEmpty()
                val amount = amountText.toDoubleOrNull()

                if (name.isBlank()) {
                    tilLineItemName.error = "Item name is required"
                    return@setOnClickListener
                }

                if (amountText.isBlank()) {
                    tilLineItemAmount.error = "Amount is required"
                    return@setOnClickListener
                }

                if (amount == null || amount < 0.0) {
                    tilLineItemAmount.error = "Enter a valid amount"
                    return@setOnClickListener
                }

                val item = ReceiptLineItem(name, amount)
                if (position == null) {
                    lineItemAdapter.addItem(item)
                } else {
                    lineItemAdapter.updateItem(position, item)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun parseLineItems(itemsText: String): List<ReceiptLineItem> {
        return itemsText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val amountMatch = amountPattern.findAll(line).lastOrNull()
                val amount = amountMatch?.value
                    ?.replace(",", "")
                    ?.replace("PHP", "", ignoreCase = true)
                    ?.replace("P", "", ignoreCase = true)
                    ?.trim()
                    ?.toDoubleOrNull()
                    ?: 0.0
                val name = amountMatch?.let { line.removeRange(it.range).trim(' ', '-', ':') }
                    ?.ifBlank { line }
                    ?: line
                ReceiptLineItem(name, amount)
            }
    }

    private fun formatLineItemsForStorage(): String {
        return lineItemAdapter.currentItems()
            .joinToString("\n") { item ->
                String.format(Locale.US, "%s - PHP %.2f", item.name, item.amount)
            }
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
        private val amountPattern = Regex("""(?:PHP|P)?\s*\d{1,3}(?:,\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})""")
    }
}
