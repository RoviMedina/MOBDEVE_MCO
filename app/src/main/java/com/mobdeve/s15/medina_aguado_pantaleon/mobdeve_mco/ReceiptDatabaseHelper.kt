package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import java.util.Locale

class ReceiptDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL
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
                $COL_OWNER_KEY TEXT NOT NULL DEFAULT '$OWNER_LEGACY',
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
        seedCategories(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 3) {
            removeSeedReceipts(db)
            removeSeedUser(db)
        }

        if (oldVersion < 4) {
            db.execSQL(
                "ALTER TABLE $TABLE_RECEIPTS ADD COLUMN $COL_OWNER_KEY TEXT NOT NULL DEFAULT '$OWNER_LEGACY'"
            )
        }

        if (oldVersion < 5) {
            db.execSQL(
                "ALTER TABLE $TABLE_USERS ADD COLUMN $COL_USER_PASSWORD TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    fun ensureDefaultUser(sessionManager: SessionManager) {
        if (sessionManager.getUserId() == -1L) {
            val userId = insertUser("Guest User", "Guest session", "fHk30f`!s=5j")
            sessionManager.saveUserId(userId)
        }
    }

    fun insertUser(name: String, email: String, password: String): Long {
        val values = ContentValues().apply {
            put(COL_USER_NAME, name)
            put(COL_USER_EMAIL, email)
            put(COL_USER_PASSWORD, password)
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

    fun loginUser(email: String, password: String): User? {
        val db = readableDatabase

        db.query(
            TABLE_USERS,
            null,
            "$COL_USER_EMAIL = ? AND $COL_USER_PASSWORD = ?",
            arrayOf(email, password),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return User(
                    id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(COL_USER_ID)
                    ),
                    name = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_USER_NAME)
                    ),
                    email = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_USER_EMAIL)
                    )
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
            put(COL_OWNER_KEY, currentOwnerKey())
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
            "$COL_ID = ? AND $COL_OWNER_KEY = ?",
            arrayOf(id.toString(), currentOwnerKey())
        )
        return rowsUpdated > 0
    }

    fun deleteReceipt(id: Long): Boolean {
        val rowsDeleted = writableDatabase.delete(
            TABLE_RECEIPTS,
            "$COL_ID = ? AND $COL_OWNER_KEY = ?",
            arrayOf(id.toString(), currentOwnerKey())
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
            "$COL_OWNER_KEY = ?",
            arrayOf(currentOwnerKey()),
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
            "$COL_ID = ? AND $COL_OWNER_KEY = ?",
            arrayOf(id.toString(), currentOwnerKey()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toReceipt() else null
        }
    }

    fun getTotalExpenses(): Double {
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($COL_TOTAL_AMOUNT) FROM $TABLE_RECEIPTS WHERE $COL_OWNER_KEY = ?",
            arrayOf(currentOwnerKey())
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
            "SELECT COUNT(*) FROM $TABLE_RECEIPTS WHERE $COL_OWNER_KEY = ?",
            arrayOf(currentOwnerKey())
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
        val oldName = getCategoryById(id)?.name
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
        if (rowsUpdated > 0 && oldName != null && !oldName.equals(name, ignoreCase = true)) {
            updateReceiptsCategory(oldName, name)
        }
        return rowsUpdated > 0
    }

    fun deleteCategory(id: Long): Boolean {
        val category = getCategoryById(id)
        val rowsDeleted = writableDatabase.delete(
            TABLE_CATEGORIES,
            "$COL_CATEGORY_ID = ?",
            arrayOf(id.toString())
        )
        if (rowsDeleted > 0 && category != null) {
            updateReceiptsCategory(category.name, NONE_CATEGORY)
        }
        return rowsDeleted > 0
    }

    fun getCategoryColorMap(): Map<String, Int> {
        return getAllCategories().associate { category ->
            category.name to category.color
        } + (NONE_CATEGORY to fallbackCategoryColor)
    }

    private fun getCategoryById(id: Long): Category? {
        readableDatabase.query(
            TABLE_CATEGORIES,
            null,
            "$COL_CATEGORY_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toCategory() else null
        }
    }

    private fun updateReceiptsCategory(oldName: String, newName: String) {
        val values = ContentValues().apply {
            put(COL_CATEGORY, newName)
        }
        writableDatabase.update(
            TABLE_RECEIPTS,
            values,
            "$COL_CATEGORY = ?",
            arrayOf(oldName)
        )
    }

    private fun queryReceipts(selection: String?, selectionArgs: Array<String>?): List<Receipt> {
        val receipts = mutableListOf<Receipt>()
        val ownerSelection = if (selection.isNullOrBlank()) {
            "$COL_OWNER_KEY = ?"
        } else {
            "$COL_OWNER_KEY = ? AND ($selection)"
        }
        val ownerSelectionArgs = arrayOf(currentOwnerKey()) + (selectionArgs ?: emptyArray())

        readableDatabase.query(
            TABLE_RECEIPTS,
            null,
            ownerSelection,
            ownerSelectionArgs,
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

    fun clearReceiptsForCurrentOwner() {
        writableDatabase.delete(
            TABLE_RECEIPTS,
            "$COL_OWNER_KEY = ?",
            arrayOf(currentOwnerKey())
        )
    }

    private fun currentOwnerKey(): String {
        val prefs = appContext.getSharedPreferences("account", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_guest", false)) {
            return OWNER_GUEST
        }

        return prefs.getString("email", null)
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
            ?: OWNER_LEGACY
    }

    private fun removeSeedReceipts(db: SQLiteDatabase) {
        val seedStores = arrayOf("Jollibee", "National Bookstore", "Grab", "Starbucks")
        db.delete(
            TABLE_RECEIPTS,
            "$COL_STORE_NAME IN (?, ?, ?, ?) AND $COL_IMAGE_URI IS NULL",
            seedStores
        )
    }

    private fun removeSeedUser(db: SQLiteDatabase) {
        db.delete(
            TABLE_USERS,
            "$COL_USER_NAME = ? AND $COL_USER_EMAIL = ?",
            arrayOf("John Doe", "johndoe@email.com")
        )
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
        private const val DATABASE_VERSION = 5

        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "name"
        private const val COL_USER_EMAIL = "email"
        private const val COL_USER_PASSWORD = "password"

        private const val TABLE_RECEIPTS = "receipts"
        private const val COL_ID = "id"
        private const val COL_STORE_NAME = "store_name"
        private const val COL_RECEIPT_DATE = "receipt_date"
        private const val COL_CATEGORY = "category"
        private const val COL_TOTAL_AMOUNT = "total_amount"
        private const val COL_ITEMS = "items"
        private const val COL_RAW_TEXT = "raw_text"
        private const val COL_IMAGE_URI = "image_uri"
        private const val COL_OWNER_KEY = "owner_key"
        private const val COL_CREATED_AT = "created_at"
        private const val OWNER_GUEST = "guest"
        private const val OWNER_LEGACY = "legacy"

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
        const val NONE_CATEGORY = "None"
        val fallbackCategoryColor: Int = Color.rgb(117, 117, 117)
    }
}
