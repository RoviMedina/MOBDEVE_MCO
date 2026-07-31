package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

data class Receipt(
    val id: Long,
    val storeName: String,
    val receiptDate: String,
    val category: String,
    val totalAmount: Double,
    val items: String,
    val rawText: String,
    val imageUri: String?
)
