package com.example.billsplit.domain

import com.example.billsplit.model.CurrencyOption

object MoneyParser {

    const val MAX_MINOR_UNITS: Long = 1_000_000_000_000L // 1 Trillion minor units

    sealed interface ParseResult {
        data class Success(val minorUnits: Long) : ParseResult
        data class Error(val message: String) : ParseResult
    }

    fun parse(input: String, currency: CurrencyOption): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Error("Enter a valid bill amount.")
        }

        // Clean currency symbols and whitespace
        var sanitized = trimmed
        if (sanitized.startsWith(currency.symbol.trim())) {
            sanitized = sanitized.substring(currency.symbol.trim().length).trim()
        }
        // Remove common currency symbols and commas
        sanitized = sanitized.replace(",", "").replace(" ", "")

        if (sanitized.contains("-")) {
            return ParseResult.Error("Enter a valid bill amount.")
        }

        val decimalPlaces = currency.decimalPlaces.coerceIn(0, 4)

        val parts = sanitized.split(".")
        if (parts.size > 2) {
            return ParseResult.Error("Enter a valid bill amount.")
        }

        val wholePartStr = parts[0]
        if (wholePartStr.isEmpty() && parts.size == 1) {
            return ParseResult.Error("Enter a valid bill amount.")
        }

        if (wholePartStr.isNotEmpty() && !wholePartStr.all { it.isDigit() }) {
            return ParseResult.Error("Enter a valid bill amount.")
        }

        val wholePart = if (wholePartStr.isEmpty()) 0L else {
            wholePartStr.toLongOrNull() ?: return ParseResult.Error("Bill amount is too large.")
        }

        var fractionUnits = 0L
        if (parts.size == 2) {
            val fractionPartStr = parts[1]
            if (!fractionPartStr.all { it.isDigit() }) {
                return ParseResult.Error("Enter a valid bill amount.")
            }

            if (decimalPlaces == 0 && fractionPartStr.isNotEmpty()) {
                return ParseResult.Error("Enter a valid bill amount.")
            }

            if (fractionPartStr.length > decimalPlaces) {
                return ParseResult.Error("Enter a valid bill amount.")
            }

            // Pad fraction to match decimalPlaces
            val paddedFraction = fractionPartStr.padEnd(decimalPlaces, '0')
            fractionUnits = if (paddedFraction.isEmpty()) 0L else paddedFraction.toLong()
        }

        var multiplier = 1L
        repeat(decimalPlaces) {
            multiplier *= 10L
        }

        val totalUnits = try {
            val wholeUnits = Math.multiplyExact(wholePart, multiplier)
            Math.addExact(wholeUnits, fractionUnits)
        } catch (_: ArithmeticException) {
            return ParseResult.Error("Bill amount is too large.")
        }

        if (totalUnits > MAX_MINOR_UNITS) {
            return ParseResult.Error("Bill amount is too large.")
        }

        return ParseResult.Success(totalUnits)
    }

    fun parseOrNull(input: String, currency: CurrencyOption): Long? {
        return when (val result = parse(input, currency)) {
            is ParseResult.Success -> result.minorUnits
            is ParseResult.Error -> null
        }
    }
}
