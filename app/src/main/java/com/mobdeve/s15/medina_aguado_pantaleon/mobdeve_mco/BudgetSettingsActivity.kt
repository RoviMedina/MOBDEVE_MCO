package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class BudgetSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.budget_settings_activity)

        val etMonthlyBudget = findViewById<TextInputEditText>(R.id.etMonthlyBudget)
        val btnSaveBudget = findViewById<MaterialButton>(R.id.btnSaveBudget)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        etMonthlyBudget.setText(
            prefs.getString("monthly_budget", "5000.00")
        )

        btnSaveBudget.setOnClickListener {

            val budget = etMonthlyBudget.text.toString()

            prefs.edit()
                .putString("monthly_budget", budget)
                .apply()

            Toast.makeText(
                this,
                "Budget saved.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}