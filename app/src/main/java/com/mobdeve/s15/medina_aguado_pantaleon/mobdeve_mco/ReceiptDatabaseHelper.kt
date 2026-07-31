package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReceiptDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECEIPTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_STORE_NAME TEXT NOT NULL,
                $COL_RECEIPT_DATE TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_TOTAL_AMOUNT REAL NOT NULL,
                $COL_ITEMS TEXT NOT NULL,
                $COL_RAW_TEXT TEXT NOT NULL,
                $COL_IMAGE_URI TEXT,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seedReceipts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECEIPTS")
        onCreate(db)
    }

    fun insertReceipt(
        storeName: String,
        receiptDate: String,
        category: String,
        totalAmount: Double,
        items: String,
        rawText: String,
        imageUri: String?
    ): Long {
        val values = ContentValues().apply {
            put(COL_STORE_NAME, storeName)
            put(COL_RECEIPT_DATE, receiptDate)
            put(COL_CATEGORY, category)
            put(COL_TOTAL_AMOUNT, totalAmount)
            put(COL_ITEMS, items)
            put(COL_RAW_TEXT, rawText)
            put(COL_IMAGE_URI, imageUri)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }

        return writableDatabase.insert(TABLE_RECEIPTS, null, values)
    }

    fun getAllReceipts(): List<Receipt> {
        return queryReceipts(null, null)
    }

    fun searchReceipts(searchText: String): List<Receipt> {
        val query = "%${searchText.trim()}%"
        return queryReceipts(
            "$COL_STORE_NAME LIKE ? OR $COL_CATEGORY LIKE ? OR $COL_RECEIPT_DATE LIKE ?",
            arrayOf(query, query, query)
        )
    }

    fun getReceiptById(id: Long): Receipt? {
        readableDatabase.query(
            TABLE_RECEIPTS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toReceipt() else null
        }
    }

    private fun queryReceipts(selection: String?, selectionArgs: Array<String>?): List<Receipt> {
        val receipts = mutableListOf<Receipt>()
        readableDatabase.query(
            TABLE_RECEIPTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COL_CREATED_AT DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                receipts.add(cursor.toReceipt())
            }
        }
        return receipts
    }

    private fun seedReceipts(db: SQLiteDatabase) {
        insertSeedReceipt(db, "Jollibee", "Today", "Food", 250.00, "Chickenjoy - PHP 120.00\nBurger Steak - PHP 100.00\nDrink - PHP 30.00")
        insertSeedReceipt(db, "National Bookstore", "Yesterday", "School", 500.00, "Notebook - PHP 120.00\nPens - PHP 80.00\nReference Book - PHP 300.00")
        insertSeedReceipt(db, "Grab", "June 26", "Transportation", 180.00, "Ride fare - PHP 165.00\nPlatform fee - PHP 15.00")
        insertSeedReceipt(db, "Starbucks", "June 25", "Food", 320.00, "Latte - PHP 190.00\nSandwich - PHP 130.00")
    }

    private fun insertSeedReceipt(
        db: SQLiteDatabase,
        storeName: String,
        receiptDate: String,
        category: String,
        totalAmount: Double,
        items: String
    ) {
        val values = ContentValues().apply {
            put(COL_STORE_NAME, storeName)
            put(COL_RECEIPT_DATE, receiptDate)
            put(COL_CATEGORY, category)
            put(COL_TOTAL_AMOUNT, totalAmount)
            put(COL_ITEMS, items)
            put(COL_RAW_TEXT, items)
            putNull(COL_IMAGE_URI)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        db.insert(TABLE_RECEIPTS, null, values)
    }

    private fun android.database.Cursor.toReceipt(): Receipt {
        return Receipt(
            id = getLong(getColumnIndexOrThrow(COL_ID)),
            storeName = getString(getColumnIndexOrThrow(COL_STORE_NAME)),
            receiptDate = getString(getColumnIndexOrThrow(COL_RECEIPT_DATE)),
            category = getString(getColumnIndexOrThrow(COL_CATEGORY)),
            totalAmount = getDouble(getColumnIndexOrThrow(COL_TOTAL_AMOUNT)),
            items = getString(getColumnIndexOrThrow(COL_ITEMS)),
            rawText = getString(getColumnIndexOrThrow(COL_RAW_TEXT)),
            imageUri = getString(getColumnIndexOrThrow(COL_IMAGE_URI)),
            createdAt = getLong(getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }

    companion object {
        private const val DATABASE_NAME = "receipt_tracker.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_RECEIPTS = "receipts"
        private const val COL_ID = "id"
        private const val COL_STORE_NAME = "store_name"
        private const val COL_RECEIPT_DATE = "receipt_date"
        private const val COL_CATEGORY = "category"
        private const val COL_TOTAL_AMOUNT = "total_amount"
        private const val COL_ITEMS = "items"
        private const val COL_RAW_TEXT = "raw_text"
        private const val COL_IMAGE_URI = "image_uri"
        private const val COL_CREATED_AT = "created_at"
    }
}
