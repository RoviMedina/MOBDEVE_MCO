package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etProfileName: TextInputEditText
    private lateinit var etProfileEmail: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var dbHelper: ReceiptDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile_activity)

        dbHelper = ReceiptDatabaseHelper(this)
        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId()

        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        loadUserData()

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        if (currentUserId != -1L) {
            val user = dbHelper.getUserById(currentUserId)
            user?.let {
                etProfileName.setText(it.name)
                etProfileEmail.setText(it.email)
            }
        }
    }

    private fun saveProfile() {
        val name = etProfileName.text.toString().trim()
        val email = etProfileEmail.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill out missing field.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == -1L) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(id = currentUserId, name = name, email = email)
        val success = dbHelper.updateUser(updatedUser)

        if (success) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }
}
