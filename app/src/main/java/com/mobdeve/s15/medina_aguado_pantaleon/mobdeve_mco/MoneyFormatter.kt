package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import java.util.Locale

object MoneyFormatter {
    fun format(context: Context, amount: Double): String {
        val currency = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("currency", "PHP")

        val prefix = when (currency) {
            "USD" -> "$"
            "EUR" -> "EUR "
            else -> "PHP "
        }

        return String.format(Locale.US, "%s%,.2f", prefix, amount)
    }

    fun prefix(context: Context): String {
        val currency = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("currency", "PHP")

        return when (currency) {
            "USD" -> "$"
            "EUR" -> "EUR "
            else -> "PHP "
        }
    }
}
