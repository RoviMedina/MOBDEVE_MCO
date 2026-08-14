package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptAdapter(
    private var receipts: List<Receipt>,
    private val onReceiptClick: (Receipt) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(receipts[position])
    }

    override fun getItemCount(): Int = receipts.size

    fun submitList(updatedReceipts: List<Receipt>) {
        receipts = updatedReceipts
        notifyDataSetChanged()
    }

    inner class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReceiptStore: TextView = itemView.findViewById(R.id.tvReceiptStore)
        private val tvReceiptMeta: TextView = itemView.findViewById(R.id.tvReceiptMeta)
        private val tvReceiptAmount: TextView = itemView.findViewById(R.id.tvReceiptAmount)

        fun bind(receipt: Receipt) {
            tvReceiptStore.text = receipt.storeName
            tvReceiptMeta.text = "${receipt.category} - ${receipt.receiptDate}"
            tvReceiptAmount.text = MoneyFormatter.format(itemView.context, receipt.totalAmount)
            itemView.setOnClickListener { onReceiptClick(receipt) }
        }
    }
}
