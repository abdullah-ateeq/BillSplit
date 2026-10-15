package com.example.billsplit

import com.example.billsplit.domain.MoneyFormatter
import com.example.billsplit.domain.MoneyParser
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.repository.CurrencyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyParserAndFormatterTest {

    private val usd = CurrencyRepository.USD
    private val eur = CurrencyRepository.EUR
    private val gbp = CurrencyRepository.GBP
    private val pkr = CurrencyRepository.PKR
    private val jpy = CurrencyRepository.JPY
    private val cad = CurrencyRepository.CAD
    private val aud = CurrencyRepository.AUD
    private val inr = CurrencyRepository.INR
    private val aed = CurrencyRepository.AED
    private val sar = CurrencyRepository.SAR

    @Test
    fun testMoneyFormattingAcrossCurrencies() {
        assertEquals("$1,250.00", MoneyFormatter.format(125000L, usd))
        assertEquals("$30.00", MoneyFormatter.format(3000L, usd))

        assertEquals("€1,250.00", MoneyFormatter.format(125000L, eur))
        assertEquals("£1,250.00", MoneyFormatter.format(125000L, gbp))
        assertEquals("Rs. 5,000.00", MoneyFormatter.format(500000L, pkr))
        assertEquals("¥1,250", MoneyFormatter.format(1250L, jpy))
        assertEquals("¥30", MoneyFormatter.format(30L, jpy))

        assertEquals("$1,250.00", MoneyFormatter.format(125000L, cad))
        assertEquals("$1,250.00", MoneyFormatter.format(125000L, aud))
        assertEquals("₹1,250.00", MoneyFormatter.format(125000L, inr))
        assertEquals("د.إ 1,250.00", MoneyFormatter.format(125000L, aed))
        assertEquals("﷼ 1,250.00", MoneyFormatter.format(125000L, sar))
    }

    @Test
    fun testCustomCurrencyFormatting() {
        val customZero = CurrencyOption("KRW", "₩", "Korean Won", 0)
        assertEquals("₩10,000", MoneyFormatter.format(10000L, customZero))

        val customThree = CurrencyOption("BHD", "BD", "Bahraini Dinar", 3)
        assertEquals("BD 10.500", MoneyFormatter.format(10500L, customThree))
    }

    @Test
    fun testMoneyParserValidAmounts() {
        assertEquals(100L, MoneyParser.parseOrNull("1", usd))
        assertEquals(1000L, MoneyParser.parseOrNull("10", usd))
        assertEquals(10000L, MoneyParser.parseOrNull("100", usd))
        assertEquals(100050L, MoneyParser.parseOrNull("1000.50", usd))
        assertEquals(125000L, MoneyParser.parseOrNull("1,250.00", usd))

        // JPY zero decimals
        assertEquals(1000L, MoneyParser.parseOrNull("1000", jpy))
        assertEquals(1250L, MoneyParser.parseOrNull("1,250", jpy))
    }

    @Test
    fun testMoneyParserWithCurrencySymbolPrefix() {
        assertEquals(12000L, MoneyParser.parseOrNull("$120.00", usd))
        assertEquals(12000L, MoneyParser.parseOrNull("Rs. 120.00", pkr))
        assertEquals(1000L, MoneyParser.parseOrNull("¥1000", jpy))
    }

    @Test
    fun testMoneyParserInvalidInputs() {
        // Negative
        assertNull(MoneyParser.parseOrNull("-10.00", usd))
        // Excessive decimal places for USD (more than 2)
        assertNull(MoneyParser.parseOrNull("10.001", usd))
        // Any decimal places for JPY (0 decimals)
        assertNull(MoneyParser.parseOrNull("10.5", jpy))
        // Non-numeric
        assertNull(MoneyParser.parseOrNull("abc", usd))
    }

    @Test
    fun testMoneyParserOverflowProtection() {
        val result = MoneyParser.parse("1000000000000000", usd)
        assertTrue(result is MoneyParser.ParseResult.Error)
        assertEquals("Bill amount is too large.", (result as MoneyParser.ParseResult.Error).message)
    }
}
