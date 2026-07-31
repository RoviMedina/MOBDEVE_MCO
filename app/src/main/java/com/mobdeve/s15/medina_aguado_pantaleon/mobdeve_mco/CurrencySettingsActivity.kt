package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CurrencySettingsActivity : AppCompatActivity() {
    private lateinit var rbPeso: RadioButton
    private lateinit var rbDollar: RadioButton
    private lateinit var rbEuro: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.currency_settings_activity)

        rbPeso = findViewById(R.id.rbPeso)
        rbDollar = findViewById(R.id.rbDollar)
        rbEuro = findViewById(R.id.rbEuro)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedCurrency = prefs.getString("currency", "PHP")

        when (savedCurrency) {
            "PHP" -> rbPeso.isChecked = true
            "USD" -> rbDollar.isChecked = true
            "EUR" -> rbEuro.isChecked = true
        }

        findViewById<Button>(R.id.btnSaveCurrency).setOnClickListener {

            val currency = when {
                rbDollar.isChecked -> "USD"
                rbEuro.isChecked -> "EUR"
                else -> "PHP"
            }

            prefs.edit()
                .putString("currency", currency)
                .apply()

            Toast.makeText(
                this,
                "Currency saved: $currency",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}