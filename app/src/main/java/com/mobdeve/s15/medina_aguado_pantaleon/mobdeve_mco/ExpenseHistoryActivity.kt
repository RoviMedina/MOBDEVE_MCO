package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class ExpenseHistoryActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private lateinit var receiptAdapter: ReceiptAdapter
    private lateinit var tvEmptyReceipts: TextView
    private lateinit var rvReceipts: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.expense_history_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        tvEmptyReceipts = findViewById(R.id.tvEmptyReceipts)

        receiptAdapter = ReceiptAdapter(emptyList()) { receipt ->
            val intent = Intent(this, ReceiptDetailsActivity::class.java).apply {
                putExtra(ReceiptDetailsActivity.EXTRA_RECEIPT_ID, receipt.id)
            }
            startActivity(intent)
        }

        rvReceipts = findViewById(R.id.rvReceipts)

        rvReceipts.apply {
            layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)
            adapter = receiptAdapter
        }

        findViewById<TextInputEditText>(R.id.etSearchReceipts).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    loadReceipts(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )

        loadReceipts()
    }

    override fun onResume() {
        super.onResume()
        loadReceipts(findViewById<TextInputEditText>(R.id.etSearchReceipts).text?.toString().orEmpty())
    }

    private fun loadReceipts(searchText: String = "") {
        val receipts = if (searchText.isBlank()) {
            receiptDatabaseHelper.getAllReceipts()
        } else {
            receiptDatabaseHelper.searchReceipts(searchText)
        }

        receiptAdapter.submitList(receipts)

        if (receipts.isEmpty()) {
            tvEmptyReceipts.visibility = View.VISIBLE
            rvReceipts.visibility = View.GONE
        } else {
            tvEmptyReceipts.visibility = View.GONE
            rvReceipts.visibility = View.VISIBLE
        }
    }
}
