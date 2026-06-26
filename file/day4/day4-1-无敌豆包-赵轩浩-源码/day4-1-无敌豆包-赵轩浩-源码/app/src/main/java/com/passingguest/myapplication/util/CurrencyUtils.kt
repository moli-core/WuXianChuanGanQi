package com.passingguest.myapplication.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    private val format = NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun format(amount: Double): String {
        return "¥${format.format(amount)}"
    }

    fun formatWithoutSymbol(amount: Double): String {
        return format.format(amount)
    }

    fun parseAmountText(text: String): Double {
        return text.replace("¥", "")
            .replace(",", "")
            .replace("，", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }
}
