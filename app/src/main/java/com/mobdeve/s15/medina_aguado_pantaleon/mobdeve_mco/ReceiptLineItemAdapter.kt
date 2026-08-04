package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReceiptLineItemAdapter(
    private val items: MutableList<ReceiptLineItem>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptLineItemAdapter.LineItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt_line, parent, false)
        return LineItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: LineItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: ReceiptLineItem) {
        items.add(item)
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, item: ReceiptLineItem) {
        if (position !in items.indices) return
        items[position] = item
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        if (position !in items.indices) return
        items.removeAt(position)
        notifyDataSetChanged()
    }

    fun currentItems(): List<ReceiptLineItem> = items

    inner class LineItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLineItemName: TextView = itemView.findViewById(R.id.tvLineItemName)
        private val tvLineItemAmount: TextView = itemView.findViewById(R.id.tvLineItemAmount)
        private val btnEditLineItem: Button = itemView.findViewById(R.id.btnEditLineItem)
        private val btnDeleteLineItem: Button = itemView.findViewById(R.id.btnDeleteLineItem)

        fun bind(item: ReceiptLineItem) {
            tvLineItemName.text = item.name
            tvLineItemAmount.text = MoneyFormatter.format(itemView.context, item.amount)
            btnEditLineItem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(position)
                }
            }
            btnDeleteLineItem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }
    }
}
