package com.example.billsplit.domain

import com.example.billsplit.model.CurrencyOption
import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {

    fun format(minorUnits: Long, currency: CurrencyOption): String {
        val numberPart = formatNumberOnly(minorUnits, currency.decimalPlaces)
        val symbol = currency.symbol.trim()
        val isNegative = minorUnits < 0

        val formattedSymbol = when {
            symbol == "Rs." || symbol == "د.إ" || symbol == "﷼" || symbol.any { it.isLetter() } -> "$symbol "
            symbol.length == 1 && !symbol[0].isLetterOrDigit() -> symbol
            else -> "$symbol "
        }

        return if (isNegative) {
            "-$formattedSymbol$numberPart"
        } else {
            "$formattedSymbol$numberPart"
        }
    }

    fun formatNumberOnly(minorUnits: Long, decimalPlaces: Int): String {
        val safeDecimals = decimalPlaces.coerceIn(0, 4)
        val absUnits = abs(minorUnits)

        if (safeDecimals == 0) {
            return String.format(Locale.US, "%,d", absUnits)
        }

        var divisor = 1L
        repeat(safeDecimals) {
            divisor *= 10
        }

        val wholePart = absUnits / divisor
        val fractionPart = absUnits % divisor

        val formattedWhole = String.format(Locale.US, "%,d", wholePart)
        val formattedFraction = String.format(Locale.US, "%0${safeDecimals}d", fractionPart)

        return "$formattedWhole.$formattedFraction"
    }
}
