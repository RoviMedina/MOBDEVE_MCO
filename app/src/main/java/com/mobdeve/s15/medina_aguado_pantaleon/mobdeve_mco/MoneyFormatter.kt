package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import java.util.Locale

object MoneyFormatter {
    fun format(context: Context, amountInPhp: Double): String {
        val currency = selectedCurrency(context)
        val convertedAmount = fromBaseAmount(currency, amountInPhp)

        return String.format(Locale.US, "%s%,.2f", prefix(currency), convertedAmount)
    }

    fun formatInputAmount(context: Context, amountInPhp: Double): String {
        return String.format(Locale.US, "%.2f", fromBaseAmount(selectedCurrency(context), amountInPhp))
    }

    fun toBaseAmount(context: Context, enteredAmount: Double): Double {
        return toBaseAmount(selectedCurrency(context), enteredAmount)
    }

    fun prefix(context: Context): String {
        return prefix(selectedCurrency(context))
    }

    fun formatLineItems(context: Context, itemsText: String): String {
        return itemsText.lines()
            .map { line -> line.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                val amountMatch = amountPattern.findAll(line).lastOrNull()
                val amountText = amountMatch?.value.orEmpty()
                val amount = parseDisplayAmount(amountText)
                val baseAmount = toBaseAmount(currencyFromText(amountText), amount)
                val name = amountMatch?.let { line.removeRange(it.range).trim(' ', '-', ':') }
                    ?.ifBlank { line }
                    ?: line
                "$name - ${format(context, baseAmount)}"
            }
    }

    fun formatAsBaseCurrency(amountInPhp: Double): String {
        return String.format(Locale.US, "PHP %,.2f", amountInPhp)
    }

    fun toBaseAmount(currency: String, enteredAmount: Double): Double {
        return enteredAmount / rateFor(currency)
    }

    fun currencyFromText(text: String): String {
        val upperText = text.uppercase(Locale.US)
        return when {
            upperText.contains("USD") || text.contains("$") -> "USD"
            upperText.contains("EUR") -> "EUR"
            else -> "PHP"
        }
    }

    fun parseDisplayAmount(text: String): Double {
        return text
            .replace(",", "")
            .replace("PHP", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("₱", "")
            .replace("$", "")
            .replace("P", "", ignoreCase = true)
            .trim()
            .toDoubleOrNull()
            ?: 0.0
    }

    private fun selectedCurrency(context: Context): String {
        return context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("currency", "PHP")
            ?: "PHP"
    }

    private fun prefix(currency: String): String {
        return when (currency) {
            "USD" -> "$"
            "EUR" -> "EUR "
            else -> "PHP "
        }
    }

    private fun fromBaseAmount(currency: String, amountInPhp: Double): Double {
        return amountInPhp * rateFor(currency)
    }

    private fun rateFor(currency: String): Double {
        return when (currency) {
            "USD" -> 0.017
            "EUR" -> 0.016
            else -> 1.0
        }
    }

    private val amountPattern = Regex("""(?:PHP|USD|EUR|P|₱|\$)?\s*\d{1,3}(?:,\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})""")
}
