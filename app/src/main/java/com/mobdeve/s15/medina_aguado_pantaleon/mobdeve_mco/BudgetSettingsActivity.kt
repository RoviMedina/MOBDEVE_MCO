package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView

class BudgetSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.budget_settings_activity)

        val etMonthlyBudget = findViewById<TextInputEditText>(R.id.etMonthlyBudget)
        val tilMonthlyBudget = findViewById<TextInputLayout>(R.id.tilMonthlyBudget)
        val tvBudgetExamples = findViewById<TextView>(R.id.tvBudgetExamples)
        val btnSaveBudget = findViewById<MaterialButton>(R.id.btnSaveBudget)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        tilMonthlyBudget.prefixText = MoneyFormatter.prefix(this)
        tvBudgetExamples.text = buildBudgetExampleText()

        val savedBudget = prefs.getString("monthly_budget", null)
            ?.toDoubleOrNull()
        etMonthlyBudget.setText(savedBudget?.let { MoneyFormatter.formatInputAmount(this, it) }.orEmpty())

        btnSaveBudget.setOnClickListener {

            val enteredBudget = etMonthlyBudget.text.toString().toDoubleOrNull() ?: 0.0
            val budgetInPhp = MoneyFormatter.toBaseAmount(this, enteredBudget)

            prefs.edit()
                .putString("monthly_budget", budgetInPhp.toString())
                .apply()

            Toast.makeText(
                this,
                "Budget saved.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    private fun buildBudgetExampleText(): String {
        return listOf(
            "Food" to 2000.0,
            "Transportation" to 1000.0,
            "School" to 1500.0,
            "Coffee" to 500.0
        ).joinToString("\n") { (category, amount) ->
            "$category: ${MoneyFormatter.format(this, amount)}"
        }
    }
}
