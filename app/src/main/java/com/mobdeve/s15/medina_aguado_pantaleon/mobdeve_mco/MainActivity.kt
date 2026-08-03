package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasActiveSession()) {
            openDashboard()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            loginAccount()
        }

        findViewById<Button>(R.id.btnGuestLogin).setOnClickListener {
            loginAsGuest()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginAccount() {
        val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()

        if (!isLoginInputValid(email, password)) {
            return
        }

        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        val savedPassword = prefs.getString("password", null)

        if (savedEmail.isNullOrBlank() || savedPassword.isNullOrBlank()) {
            Toast.makeText(this, "No account found. Please register first.", Toast.LENGTH_SHORT).show()
            return
        }

        if (email != savedEmail || password != savedPassword) {
            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_guest", false)
            .apply()

        openDashboard()
    }

    private fun loginAsGuest() {
        getSharedPreferences("account", Context.MODE_PRIVATE)
            .edit()
            .putString("name", "Guest User")
            .putString("email", "Guest session")
            .putBoolean("is_logged_in", true)
            .putBoolean("is_guest", true)
            .apply()

        SessionManager(this).clearSession()
        openDashboard()
    }

    private fun isLoginInputValid(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun hasActiveSession(): Boolean {
        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val savedEmail = prefs.getString("email", null)
        val isGuest = prefs.getBoolean("is_guest", false)
        return isLoggedIn && (isGuest || !savedEmail.isNullOrBlank())
    }

    private fun openDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
