package com.example.billsplit

import com.example.billsplit.domain.BillSplitValidator
import com.example.billsplit.model.BillPerson
import com.example.billsplit.repository.CurrencyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillSplitValidatorTest {

    private val usd = CurrencyRepository.USD

    @Test
    fun testValidateBillValid() {
        val result = BillSplitValidator.validateBill("120.00", usd)
        assertTrue(result is BillSplitValidator.ValidationResult.Valid)
        assertEquals(12000L, (result as BillSplitValidator.ValidationResult.Valid).value)
    }

    @Test
    fun testValidateBillZeroAndNegative() {
        val zeroResult = BillSplitValidator.validateBill("0.00", usd)
        assertTrue(zeroResult is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Enter a valid bill amount.", (zeroResult as BillSplitValidator.ValidationResult.Invalid).message)

        val negativeResult = BillSplitValidator.validateBill("-50.00", usd)
        assertTrue(negativeResult is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Enter a valid bill amount.", (negativeResult as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidateBillOverflow() {
        val overflowResult = BillSplitValidator.validateBill("999999999999999999.00", usd)
        assertTrue(overflowResult is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Bill amount is too large.", (overflowResult as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidatePeopleCountBounds() {
        // Valid bounds: 2 to 50
        assertTrue(BillSplitValidator.validatePeopleCount(2) is BillSplitValidator.ValidationResult.Valid)
        assertTrue(BillSplitValidator.validatePeopleCount(4) is BillSplitValidator.ValidationResult.Valid)
        assertTrue(BillSplitValidator.validatePeopleCount(50) is BillSplitValidator.ValidationResult.Valid)

        // Invalid: < 2
        val belowMin = BillSplitValidator.validatePeopleCount(1)
        assertTrue(belowMin is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("At least 2 people are required.", (belowMin as BillSplitValidator.ValidationResult.Invalid).message)

        // Invalid: > 50 (Critical Test: 51 people)
        val aboveMax = BillSplitValidator.validatePeopleCount(51)
        assertTrue(aboveMax is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Maximum 50 people.", (aboveMax as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidateSharesValid() {
        val people = listOf(
            BillPerson("1", "Alex", 2L),
            BillPerson("2", "Sam", 1L)
        )
        val result = BillSplitValidator.validateShares(people)
        assertTrue(result is BillSplitValidator.ValidationResult.Valid)
    }

    @Test
    fun testValidateZeroShares() {
        // Critical test: 0 shares
        val people = listOf(
            BillPerson("1", "Alex", 0L),
            BillPerson("2", "Sam", 1L)
        )
        val result = BillSplitValidator.validateShares(people)
        assertTrue(result is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Shares must be greater than zero.", (result as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidateNegativeShares() {
        val people = listOf(
            BillPerson("1", "Alex", -1L),
            BillPerson("2", "Sam", 1L)
        )
        val result = BillSplitValidator.validateShares(people)
        assertTrue(result is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Shares must be greater than zero.", (result as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidatePercentagesExact100() {
        // 50 / 50
        val p50_50 = listOf(BillPerson("1", "A", 5000L), BillPerson("2", "B", 5000L))
        assertTrue(BillSplitValidator.validatePercentages(p50_50) is BillSplitValidator.ValidationResult.Valid)

        // 25 / 75
        val p25_75 = listOf(BillPerson("1", "A", 2500L), BillPerson("2", "B", 7500L))
        assertTrue(BillSplitValidator.validatePercentages(p25_75) is BillSplitValidator.ValidationResult.Valid)

        // 25 / 25 / 50
        val p25_25_50 = listOf(BillPerson("1", "A", 2500L), BillPerson("2", "B", 2500L), BillPerson("3", "C", 5000L))
        assertTrue(BillSplitValidator.validatePercentages(p25_25_50) is BillSplitValidator.ValidationResult.Valid)

        // 20 / 30 / 50
        val p20_30_50 = listOf(BillPerson("1", "A", 2000L), BillPerson("2", "B", 3000L), BillPerson("3", "C", 5000L))
        assertTrue(BillSplitValidator.validatePercentages(p20_30_50) is BillSplitValidator.ValidationResult.Valid)

        // 33.33 / 33.33 / 33.34
        val p33_34 = listOf(BillPerson("1", "A", 3333L), BillPerson("2", "B", 3333L), BillPerson("3", "C", 3334L))
        assertTrue(BillSplitValidator.validatePercentages(p33_34) is BillSplitValidator.ValidationResult.Valid)

        // 0% / 100%
        val p0_100 = listOf(BillPerson("1", "A", 0L), BillPerson("2", "B", 10000L))
        assertTrue(BillSplitValidator.validatePercentages(p0_100) is BillSplitValidator.ValidationResult.Valid)
    }

    @Test
    fun testValidatePercentagesUnderAllocated() {
        // Critical Test: 50% / 30% -> 20% remaining
        val people = listOf(
            BillPerson("1", "A", 5000L),
            BillPerson("2", "B", 3000L)
        )
        val result = BillSplitValidator.validatePercentages(people)
        assertTrue(result is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Percentages must total 100%.", (result as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testValidatePercentagesOverAllocated() {
        // Critical Test: 60% / 50% -> 10% over
        val people = listOf(
            BillPerson("1", "A", 6000L),
            BillPerson("2", "B", 5000L)
        )
        val result = BillSplitValidator.validatePercentages(people)
        assertTrue(result is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Percentages must total 100%.", (result as BillSplitValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun testPercentageParsingPrecision() {
        assertEquals(5000L, BillSplitValidator.parsePercentageInput("50"))
        assertEquals(5000L, BillSplitValidator.parsePercentageInput("50%"))
        assertEquals(3333L, BillSplitValidator.parsePercentageInput("33.33"))
        assertEquals(3330L, BillSplitValidator.parsePercentageInput("33.3"))
        assertEquals(0L, BillSplitValidator.parsePercentageInput("0"))
        assertEquals(10000L, BillSplitValidator.parsePercentageInput("100"))

        // More than 2 decimal digits -> null (rejected)
        assertNull(BillSplitValidator.parsePercentageInput("33.333"))
        // Negative -> null
        assertNull(BillSplitValidator.parsePercentageInput("-10"))
        // > 100 -> null
        assertNull(BillSplitValidator.parsePercentageInput("101"))
    }
}
