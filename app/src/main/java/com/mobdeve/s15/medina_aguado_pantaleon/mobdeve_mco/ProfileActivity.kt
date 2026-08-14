package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var dbHelper: ReceiptDatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        dbHelper = ReceiptDatabaseHelper(this)
        sessionManager = SessionManager(this)

        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            val intent = Intent(this, ManageCategoriesActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnCurrency).setOnClickListener {
            val intent = Intent(this, CurrencySettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnBudget).setOnClickListener {
            val intent = Intent(this, BudgetSettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            getSharedPreferences("account", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", false)
                .putBoolean("is_guest", false)
                .apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val accountPrefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        val isGuest = accountPrefs.getBoolean("is_guest", false)
        if (isGuest) {
            tvProfileName.text = "Guest User"
            tvProfileEmail.text = "Guest session"
            findViewById<Button>(R.id.btnEditProfile).isEnabled = false
            return
        }

        val savedName = accountPrefs.getString("name", null)
        val savedEmail = accountPrefs.getString("email", null)
        if (!savedName.isNullOrBlank() && !savedEmail.isNullOrBlank()) {
            tvProfileName.text = savedName
            tvProfileEmail.text = savedEmail
            findViewById<Button>(R.id.btnEditProfile).isEnabled = true
            return
        }

        dbHelper.ensureDefaultUser(sessionManager)
        findViewById<Button>(R.id.btnEditProfile).isEnabled = true

        val userId = sessionManager.getUserId()
        if (userId != -1L) {
            val user = dbHelper.getUserById(userId)
            user?.let {
                tvProfileName.text = it.name
                tvProfileEmail.text = it.email
            }
        }
    }
}
