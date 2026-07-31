package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        findViewById<Button>(R.id.btnManageCategories).setOnClickListener {
            Toast.makeText(this, "TODO: Build category management screen.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCurrency).setOnClickListener {
            Toast.makeText(this, "TODO: Add currency preference picker.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAbout).setOnClickListener {

            val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)

            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // TODO: Clear saved session once authentication is implemented.
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
