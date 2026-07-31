package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color

class ReceiptDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT NOT NULL
            )
            """.trimIndent()
        )
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
        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY_NAME TEXT NOT NULL UNIQUE,
                $COL_CATEGORY_COLOR INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seedReceipts(db)
        seedDefaultUser(db)
        seedCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECEIPTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }

    fun ensureDefaultUser(sessionManager: SessionManager) {
        if (sessionManager.getUserId() == -1L) {
            val db = readableDatabase
            val cursor = db.query(TABLE_USERS, arrayOf(COL_USER_ID), null, null, null, null, null)
            if (cursor.moveToFirst()) {
                sessionManager.saveUserId(cursor.getLong(0))
            } else {
                // Should not happen if seedDefaultUser works, but as a backup:
                val userId = insertUser("John Doe", "johndoe@email.com")
                sessionManager.saveUserId(userId)
            }
            cursor.close()
        }
    }

    fun insertUser(name: String, email: String): Long {
        val values = ContentValues().apply {
            put(COL_USER_NAME, name)
            put(COL_USER_EMAIL, email)
        }
        return writableDatabase.insert(TABLE_USERS, null, values)
    }

    fun getUserById(id: Long): User? {
        val db = readableDatabase
        db.query(
            TABLE_USERS,
            null,
            "$COL_USER_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL))
                )
            }
        }
        return null
    }

    fun updateUser(user: User): Boolean {
        val values = ContentValues().apply {
            put(COL_USER_NAME, user.name)
            put(COL_USER_EMAIL, user.email)
        }
        val rowsUpdated = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_USER_ID = ?",
            arrayOf(user.id.toString())
        )
        return rowsUpdated > 0
    }

    private fun seedDefaultUser(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_USER_NAME, "John Doe")
            put(COL_USER_EMAIL, "johndoe@email.com")
        }
        db.insert(TABLE_USERS, null, values)
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

    fun updateReceipt(
        id: Long,
        storeName: String,
        receiptDate: String,
        category: String,
        totalAmount: Double,
        items: String,
        rawText: String,
        imageUri: String?
    ): Boolean {
        val values = ContentValues().apply {
            put(COL_STORE_NAME, storeName)
            put(COL_RECEIPT_DATE, receiptDate)
            put(COL_CATEGORY, category)
            put(COL_TOTAL_AMOUNT, totalAmount)
            put(COL_ITEMS, items)
            put(COL_RAW_TEXT, rawText)
            put(COL_IMAGE_URI, imageUri)
        }

        val rowsUpdated = writableDatabase.update(
            TABLE_RECEIPTS,
            values,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
        return rowsUpdated > 0
    }

    fun deleteReceipt(id: Long): Boolean {
        val rowsDeleted = writableDatabase.delete(
            TABLE_RECEIPTS,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
        return rowsDeleted > 0
    }

    fun getAllReceipts(): List<Receipt> {
        return queryReceipts(null, null)
    }

    fun getRecentReceipts(limit: Int): List<Receipt> {
        val receipts = mutableListOf<Receipt>()

        readableDatabase.query(
            TABLE_RECEIPTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CREATED_AT DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                receipts.add(cursor.toReceipt())
            }
        }

        return receipts
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

    fun getTotalExpenses(): Double {
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($COL_TOTAL_AMOUNT) FROM $TABLE_RECEIPTS",
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getDouble(0)
            } else {
                0.0
            }
        }
    }

    fun getReceiptCount(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_RECEIPTS",
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()

        readableDatabase.query(
            TABLE_CATEGORIES,
            null,
            null,
            null,
            null,
            null,
            "$COL_CATEGORY_NAME ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                categories.add(cursor.toCategory())
            }
        }

        if (categories.isEmpty()) {
            seedCategories(writableDatabase)
            return getAllCategories()
        }

        return categories
    }

    fun insertCategory(name: String, color: Int): Long {
        val values = ContentValues().apply {
            put(COL_CATEGORY_NAME, name)
            put(COL_CATEGORY_COLOR, color)
        }

        return writableDatabase.insert(TABLE_CATEGORIES, null, values)
    }

    fun updateCategory(id: Long, name: String, color: Int): Boolean {
        val values = ContentValues().apply {
            put(COL_CATEGORY_NAME, name)
            put(COL_CATEGORY_COLOR, color)
        }

        val rowsUpdated = writableDatabase.update(
            TABLE_CATEGORIES,
            values,
            "$COL_CATEGORY_ID = ?",
            arrayOf(id.toString())
        )
        return rowsUpdated > 0
    }

    fun deleteCategory(id: Long): Boolean {
        val rowsDeleted = writableDatabase.delete(
            TABLE_CATEGORIES,
            "$COL_CATEGORY_ID = ?",
            arrayOf(id.toString())
        )
        return rowsDeleted > 0
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

    private fun seedCategories(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CATEGORIES", null)
        val hasCategories = cursor.use {
            it.moveToFirst() && it.getInt(0) > 0
        }

        if (hasCategories) {
            return
        }

        insertSeedCategory(db, "Food", categoryColors[0])
        insertSeedCategory(db, "Transportation", categoryColors[1])
        insertSeedCategory(db, "School", categoryColors[2])
        insertSeedCategory(db, "Coffee", categoryColors[3])
    }

    private fun insertSeedCategory(db: SQLiteDatabase, name: String, color: Int) {
        val values = ContentValues().apply {
            put(COL_CATEGORY_NAME, name)
            put(COL_CATEGORY_COLOR, color)
        }
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
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

    private fun android.database.Cursor.toCategory(): Category {
        return Category(
            id = getLong(getColumnIndexOrThrow(COL_CATEGORY_ID)),
            name = getString(getColumnIndexOrThrow(COL_CATEGORY_NAME)),
            color = getInt(getColumnIndexOrThrow(COL_CATEGORY_COLOR))
        )
    }

    companion object {
        private const val DATABASE_NAME = "receipt_tracker.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "name"
        private const val COL_USER_EMAIL = "email"

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

        private const val TABLE_CATEGORIES = "categories"
        private const val COL_CATEGORY_ID = "category_id"
        private const val COL_CATEGORY_NAME = "name"
        private const val COL_CATEGORY_COLOR = "color"

        val categoryColors = listOf(
            Color.rgb(103, 80, 164),
            Color.rgb(46, 125, 50),
            Color.rgb(2, 119, 189),
            Color.rgb(239, 108, 0),
            Color.rgb(198, 40, 40)
        )
    }
}
