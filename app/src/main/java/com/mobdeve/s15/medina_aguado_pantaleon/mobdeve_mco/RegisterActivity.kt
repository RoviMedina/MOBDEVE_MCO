package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        tilName = findViewById(R.id.tilRegisterName)
        tilEmail = findViewById(R.id.tilRegisterEmail)
        tilPassword = findViewById(R.id.tilRegisterPassword)
        tilConfirmPassword = findViewById(R.id.tilRegisterConfirmPassword)
        etName = findViewById(R.id.etRegisterName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword)

        findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            registerAccount()
        }

        findViewById<Button>(R.id.btnBackToLogin).setOnClickListener {
            finish()
        }
    }

    private fun registerAccount() {
        clearErrors()

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        var hasError = false

        if (name.isBlank()) {
            tilName.error = "Full name is required"
            hasError = true
        }

        if (email.isBlank()) {
            tilEmail.error = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            hasError = true
        }

        if (password.isBlank()) {
            tilPassword.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            tilPassword.error = "Use at least 6 characters"
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            tilConfirmPassword.error = "Confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            return
        }

        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name", name)
            .putString("email", email)
            .putString("password", password)
            .putBoolean("is_logged_in", true)
            .putBoolean("is_guest", false)
            .apply()

        Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun clearErrors() {
        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }
}
