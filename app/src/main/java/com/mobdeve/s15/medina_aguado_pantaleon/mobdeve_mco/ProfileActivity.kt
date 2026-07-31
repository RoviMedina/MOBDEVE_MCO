package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        loadProfileInfo()

        findViewById<Button>(R.id.btnCurrency).setOnClickListener {

            val intent = Intent(this, CurrencySettingsActivity::class.java)
            startActivity(intent)

        }

        findViewById<Button>(R.id.btnBudget).setOnClickListener {

            val intent = Intent(this, BudgetSettingsActivity::class.java)
            startActivity(intent)

        }

        findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            val intent = Intent(this, ManageCategoriesActivity::class.java)
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
            getSharedPreferences("account", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", false)
                .apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileInfo()
    }

    private fun loadProfileInfo() {
        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        val name = prefs.getString("name", "Guest User")
        val email = prefs.getString("email", "No email saved")

        findViewById<TextView>(R.id.tvProfileName).text = name
        findViewById<TextView>(R.id.tvProfileEmail).text = email
    }
}
