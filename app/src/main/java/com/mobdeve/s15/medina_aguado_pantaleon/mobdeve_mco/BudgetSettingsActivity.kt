package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.text.InputType
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView
import java.util.Locale

class BudgetSettingsActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private val categoryBudgetInputs = mutableMapOf<Long, TextInputEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.budget_settings_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        val etMonthlyBudget = findViewById<TextInputEditText>(R.id.etMonthlyBudget)
        val tilMonthlyBudget = findViewById<TextInputLayout>(R.id.tilMonthlyBudget)
        val tvBudgetExamples = findViewById<TextView>(R.id.tvBudgetExamples)
        val layoutCategoryBudgets = findViewById<LinearLayout>(R.id.layoutCategoryBudgets)
        val btnSaveBudget = findViewById<MaterialButton>(R.id.btnSaveBudget)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        tilMonthlyBudget.prefixText = MoneyFormatter.prefix(this)

        val savedBudget = prefs.getString(monthlyBudgetKey(), null)
            ?.toDoubleOrNull()
        etMonthlyBudget.setText(savedBudget?.let { MoneyFormatter.formatInputAmount(this, it) }.orEmpty())
        loadCategoryBudgetFields(layoutCategoryBudgets, tvBudgetExamples, prefs)

        btnSaveBudget.setOnClickListener {
            tilMonthlyBudget.error = null

            val enteredBudgetText = etMonthlyBudget.text.toString().trim()
            val enteredBudget = enteredBudgetText
                .takeIf { it.isNotBlank() }
                ?.toDoubleOrNull()

            if (enteredBudgetText.isNotBlank() && enteredBudget == null) {
                tilMonthlyBudget.error = "Enter a valid amount"
                return@setOnClickListener
            }

            val monthlyBudgetInPhp = enteredBudget?.let {
                MoneyFormatter.toBaseAmount(this, it)
            } ?: 0.0

            val categoryAmounts = mutableMapOf<Long, Double>()

            // Validate every category first
            for ((categoryId, input) in categoryBudgetInputs) {
                val amountText = input.text.toString().trim()
                val amount = amountText
                    .takeIf { it.isNotBlank() }
                    ?.toDoubleOrNull()

                if (amountText.isNotBlank() && amount == null) {
                    input.error = "Enter a valid amount"
                    Toast.makeText(
                        this,
                        "Enter valid category budget amounts.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                input.error = null

                if (amount != null) {
                    val amountInPhp = MoneyFormatter.toBaseAmount(this, amount)
                    categoryAmounts[categoryId] = amountInPhp
                }
            }

            // Calculate total of all category budgets
            val categoryBudgetTotal = categoryAmounts.values.sum()

            // Total category budgets cannot exceed monthly budget
            if (monthlyBudgetInPhp > 0.0 &&
                categoryBudgetTotal > monthlyBudgetInPhp
            ) {
                val remaining = monthlyBudgetInPhp - categoryBudgetTotal

                Toast.makeText(
                    this,
                    "Category budgets total ${MoneyFormatter.format(this, categoryBudgetTotal)}, " +
                            "which exceeds the monthly budget of ${MoneyFormatter.format(this, monthlyBudgetInPhp)}.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val editor = prefs.edit()

            // Save category budgets
            categoryBudgetInputs.forEach { (categoryId, input) ->
                val amountInPhp = categoryAmounts[categoryId]

                if (amountInPhp == null) {
                    editor.remove(categoryBudgetKey(categoryId))
                } else {
                    editor.putString(
                        categoryBudgetKey(categoryId),
                        amountInPhp.toString()
                    )
                }
            }

            // Save monthly budget
            if (monthlyBudgetInPhp > 0.0) {
                editor.putString(
                    monthlyBudgetKey(),
                    monthlyBudgetInPhp.toString()
                )
            } else {
                editor.remove(monthlyBudgetKey())
            }

            editor.apply()

            Toast.makeText(
                this,
                "Budget saved.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    private fun loadCategoryBudgetFields(
        container: LinearLayout,
        title: TextView,
        prefs: android.content.SharedPreferences
    ) {
        categoryBudgetInputs.clear()
        container.removeAllViews()

        val categories = receiptDatabaseHelper.getAllCategories()
        title.text = if (categories.isEmpty()) "No categories yet" else "Category Budgets"

        categories.forEach { category ->
            val inputLayout = TextInputLayout(this).apply {
                hint = category.name
                prefixText = MoneyFormatter.prefix(this@BudgetSettingsActivity)
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val input = TextInputEditText(inputLayout.context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
                val savedAmount = prefs.getString(categoryBudgetKey(category.id), null)?.toDoubleOrNull()
                setText(savedAmount?.let { MoneyFormatter.formatInputAmount(this@BudgetSettingsActivity, it) }.orEmpty())
            }

            inputLayout.addView(input)
            container.addView(inputLayout)
            categoryBudgetInputs[category.id] = input
        }
    }

    private fun monthlyBudgetKey(): String {
        return "monthly_budget_${accountKey()}"
    }

    private fun categoryBudgetKey(categoryId: Long): String {
        return "category_budget_${accountKey()}_$categoryId"
    }

    private fun accountKey(): String {
        val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_guest", false)) {
            return "guest"
        }

        return prefs.getString("email", null)
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
            ?: "legacy"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
