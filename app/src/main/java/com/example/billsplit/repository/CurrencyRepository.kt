package com.example.billsplit.repository

import com.example.billsplit.model.CurrencyOption

object CurrencyRepository {
    val USD = CurrencyOption(code = "USD", symbol = "$", name = "US Dollar", decimalPlaces = 2)
    val EUR = CurrencyOption(code = "EUR", symbol = "€", name = "Euro", decimalPlaces = 2)
    val GBP = CurrencyOption(code = "GBP", symbol = "£", name = "British Pound", decimalPlaces = 2)
    val PKR = CurrencyOption(code = "PKR", symbol = "Rs.", name = "Pakistani Rupee", decimalPlaces = 2)
    val AED = CurrencyOption(code = "AED", symbol = "د.إ", name = "UAE Dirham", decimalPlaces = 2)
    val SAR = CurrencyOption(code = "SAR", symbol = "﷼", name = "Saudi Riyal", decimalPlaces = 2)
    val CAD = CurrencyOption(code = "CAD", symbol = "$", name = "Canadian Dollar", decimalPlaces = 2)
    val AUD = CurrencyOption(code = "AUD", symbol = "$", name = "Australian Dollar", decimalPlaces = 2)
    val INR = CurrencyOption(code = "INR", symbol = "₹", name = "Indian Rupee", decimalPlaces = 2)
    val JPY = CurrencyOption(code = "JPY", symbol = "¥", name = "Japanese Yen", decimalPlaces = 0)
    val CNY = CurrencyOption(code = "CNY", symbol = "¥", name = "Chinese Yuan", decimalPlaces = 2)

    val DEFAULT_CURRENCY = USD

    val standardCurrencies: List<CurrencyOption> = listOf(
        USD,
        EUR,
        GBP,
        PKR,
        AED,
        SAR,
        CAD,
        AUD,
        INR,
        JPY,
        CNY
    )

    fun createCustomCurrency(symbol: String, name: String = "Custom", decimalPlaces: Int = 2): CurrencyOption {
        val safeDecimals = decimalPlaces.coerceIn(0, 4)
        val safeSymbol = symbol.trim().ifEmpty { "$" }
        return CurrencyOption(
            code = "CUSTOM",
            symbol = safeSymbol,
            name = name.trim().ifEmpty { "Custom" },
            decimalPlaces = safeDecimals
        )
    }

    fun findByCode(code: String): CurrencyOption? {
        return standardCurrencies.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
