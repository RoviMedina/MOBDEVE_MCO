package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

        // Ensure we have a user session for this demo
        dbHelper.ensureDefaultUser(sessionManager)

        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            Toast.makeText(this, "TODO: Build category management screen.", Toast.LENGTH_SHORT).show()
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
