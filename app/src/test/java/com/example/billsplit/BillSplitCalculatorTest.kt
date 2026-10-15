package com.example.billsplit

import com.example.billsplit.domain.BillSplitCalculator
import com.example.billsplit.domain.BillSplitFormatter
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.CurrencyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillSplitCalculatorTest {

    private val usd = CurrencyRepository.USD
    private val jpy = CurrencyRepository.JPY
    private val pkr = CurrencyRepository.PKR

    // 1. Equal Split & Critical Test Equal
    @Test
    fun testCriticalEqualSplit_120_4people() {
        val billMinorUnits = 12000L // $120.00
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 4, usd)

        assertEquals(4, result.people.size)
        assertEquals(12000L, result.totalAllocatedMinorUnits)
        assertEquals(0L, result.remainderMinorUnits)
        assertTrue(result.isExactPerPerson)
        assertEquals("$30.00 EACH", result.summaryPerPersonAmount)
        assertNull(result.roundingAdjustmentNote)

        for (person in result.people) {
            assertEquals(3000L, person.allocatedMinorUnits)
            assertEquals("$30.00", person.formattedAmount)
            assertEquals(25.0, person.effectivePercentage, 0.001)
        }
        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 2. Two people
    @Test
    fun testEqualSplit_TwoPeople() {
        val result = BillSplitCalculator.splitEqually(5000L, 2, usd)
        assertEquals(2, result.people.size)
        assertEquals(2500L, result.people[0].allocatedMinorUnits)
        assertEquals(2500L, result.people[1].allocatedMinorUnits)
        assertEquals(5000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 3. Three people & Uneven amount & One-cent remainder
    @Test
    fun testCriticalUnevenEqual_100_3people() {
        val billMinorUnits = 10000L // $100.00
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 3, usd)

        assertEquals(3, result.people.size)
        assertFalse(result.isExactPerPerson)
        assertEquals("ABOUT $33.33 EACH", result.summaryPerPersonAmount)
        assertNotNull(result.roundingAdjustmentNote)
        assertTrue(result.roundingAdjustmentNote!!.contains("1¢ added to Person 1"))

        assertEquals(3334L, result.people[0].allocatedMinorUnits)
        assertEquals("$33.34", result.people[0].formattedAmount)

        assertEquals(3333L, result.people[1].allocatedMinorUnits)
        assertEquals("$33.33", result.people[1].formattedAmount)

        assertEquals(3333L, result.people[2].allocatedMinorUnits)
        assertEquals("$33.33", result.people[2].formattedAmount)

        assertEquals(10000L, result.totalAllocatedMinorUnits)
        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 4. Critical Test One Cent ($10.00 / 3 people)
    @Test
    fun testCriticalOneCent_10_3people() {
        val billMinorUnits = 1000L // $10.00
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 3, usd)

        assertEquals(334L, result.people[0].allocatedMinorUnits)
        assertEquals("$3.34", result.people[0].formattedAmount)

        assertEquals(333L, result.people[1].allocatedMinorUnits)
        assertEquals("$3.33", result.people[1].formattedAmount)

        assertEquals(333L, result.people[2].allocatedMinorUnits)
        assertEquals("$3.33", result.people[2].formattedAmount)

        assertEquals(1000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 5. Multiple-cent remainder ($10.00 / 4 people vs $10.00 / 6 people)
    @Test
    fun testMultipleCentRemainder_10_6people() {
        val billMinorUnits = 1000L // $10.00 / 6 = 166.666... cents, remainder 4 cents
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 6, usd)

        assertEquals(6, result.people.size)
        assertEquals(167L, result.people[0].allocatedMinorUnits)
        assertEquals(167L, result.people[1].allocatedMinorUnits)
        assertEquals(167L, result.people[2].allocatedMinorUnits)
        assertEquals(167L, result.people[3].allocatedMinorUnits)
        assertEquals(166L, result.people[4].allocatedMinorUnits)
        assertEquals(166L, result.people[5].allocatedMinorUnits)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 6. Ten people
    @Test
    fun testEqualSplit_TenPeople() {
        val result = BillSplitCalculator.splitEqually(12345L, 10, usd)
        assertEquals(10, result.people.size)
        assertEquals(12345L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 7. Fifty people (Maximum people)
    @Test
    fun testEqualSplit_FiftyPeople() {
        val result = BillSplitCalculator.splitEqually(99999L, 50, usd)
        assertEquals(50, result.people.size)
        assertEquals(99999L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 8. Custom Shares Critical Test: $100.00, Shares 2 / 1 / 1
    @Test
    fun testCriticalCustomShares_100_2_1_1() {
        val billMinorUnits = 10000L
        val people = listOf(
            BillPerson("1", "Alex", 2L),
            BillPerson("2", "Sam", 1L),
            BillPerson("3", "John", 1L)
        )
        val result = BillSplitCalculator.splitByShares(billMinorUnits, people, usd)

        assertEquals(3, result.people.size)
        assertEquals(5000L, result.people[0].allocatedMinorUnits)
        assertEquals("$50.00", result.people[0].formattedAmount)
        assertEquals(50.0, result.people[0].effectivePercentage, 0.001)

        assertEquals(2500L, result.people[1].allocatedMinorUnits)
        assertEquals("$25.00", result.people[1].formattedAmount)
        assertEquals(25.0, result.people[1].effectivePercentage, 0.001)

        assertEquals(2500L, result.people[2].allocatedMinorUnits)
        assertEquals("$25.00", result.people[2].formattedAmount)
        assertEquals(25.0, result.people[2].effectivePercentage, 0.001)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 9. Custom Shares Rounding Critical Test: $10.00, Shares 1 / 1 / 1
    @Test
    fun testCriticalCustomShares_10_1_1_1() {
        val billMinorUnits = 1000L
        val people = listOf(
            BillPerson("1", "Alex", 1L),
            BillPerson("2", "Sam", 1L),
            BillPerson("3", "John", 1L)
        )
        val result = BillSplitCalculator.splitByShares(billMinorUnits, people, usd)

        assertEquals(334L, result.people[0].allocatedMinorUnits)
        assertEquals("$3.34", result.people[0].formattedAmount)

        assertEquals(333L, result.people[1].allocatedMinorUnits)
        assertEquals("$3.33", result.people[1].formattedAmount)

        assertEquals(333L, result.people[2].allocatedMinorUnits)
        assertEquals("$3.33", result.people[2].formattedAmount)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 10. Percentages Critical Test: $100.00, 50%, 30%, 20%
    @Test
    fun testCriticalPercentages_50_30_20() {
        val billMinorUnits = 10000L
        val people = listOf(
            BillPerson("1", "Alex", 5000L),
            BillPerson("2", "Sam", 3000L),
            BillPerson("3", "John", 2000L)
        )
        val result = BillSplitCalculator.splitByPercentages(billMinorUnits, people, usd)

        assertEquals(5000L, result.people[0].allocatedMinorUnits)
        assertEquals("$50.00", result.people[0].formattedAmount)

        assertEquals(3000L, result.people[1].allocatedMinorUnits)
        assertEquals("$30.00", result.people[1].formattedAmount)

        assertEquals(2000L, result.people[2].allocatedMinorUnits)
        assertEquals("$20.00", result.people[2].formattedAmount)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 11. Decimal Percentages Critical Test: $100.00, 33.33%, 33.33%, 33.34%
    @Test
    fun testCriticalDecimalPercentages_3333_3333_3334() {
        val billMinorUnits = 10000L
        val people = listOf(
            BillPerson("1", "Alex", 3333L),
            BillPerson("2", "Sam", 3333L),
            BillPerson("3", "John", 3334L)
        )
        val result = BillSplitCalculator.splitByPercentages(billMinorUnits, people, usd)

        assertEquals(3333L, result.people[0].allocatedMinorUnits)
        assertEquals("$33.33", result.people[0].formattedAmount)

        assertEquals(3333L, result.people[1].allocatedMinorUnits)
        assertEquals("$33.33", result.people[1].formattedAmount)

        assertEquals(3334L, result.people[2].allocatedMinorUnits)
        assertEquals("$33.34", result.people[2].formattedAmount)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 12. Zero Percent Critical Test: $100.00, 0%, 100%
    @Test
    fun testCriticalZeroPercent_0_100() {
        val billMinorUnits = 10000L
        val people = listOf(
            BillPerson("1", "Alex", 0L),
            BillPerson("2", "Sam", 10000L)
        )
        val result = BillSplitCalculator.splitByPercentages(billMinorUnits, people, usd)

        assertEquals(0L, result.people[0].allocatedMinorUnits)
        assertEquals("$0.00", result.people[0].formattedAmount)

        assertEquals(10000L, result.people[1].allocatedMinorUnits)
        assertEquals("$100.00", result.people[1].formattedAmount)

        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 13. JPY Zero Decimal Currency Critical Test: ¥1000, 3 people -> ¥334, ¥333, ¥333
    @Test
    fun testCriticalJPY_1000_3people() {
        val billMinorUnits = 1000L
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 3, jpy)

        assertEquals(334L, result.people[0].allocatedMinorUnits)
        assertEquals("¥334", result.people[0].formattedAmount)

        assertEquals(333L, result.people[1].allocatedMinorUnits)
        assertEquals("¥333", result.people[1].formattedAmount)

        assertEquals(333L, result.people[2].allocatedMinorUnits)
        assertEquals("¥333", result.people[2].formattedAmount)

        assertEquals(1000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 14. PKR Currency Critical Test: Rs. 1000.00, 4 people -> Rs. 250.00 each
    @Test
    fun testCriticalPKR_1000_4people() {
        val billMinorUnits = 100000L // Rs. 1000.00
        val result = BillSplitCalculator.splitEqually(billMinorUnits, 4, pkr)

        for (person in result.people) {
            assertEquals(25000L, person.allocatedMinorUnits)
            assertEquals("Rs. 250.00", person.formattedAmount)
        }
        assertEquals(100000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 15. Large Value & Overflow Protection Critical Test
    @Test
    fun testCriticalLargeBillWithoutOverflow() {
        val billMinorUnits = 1_000_000_000_000L // 1 Trillion minor units
        val people = listOf(
            BillPerson("1", "Alex", 1_000_000_000L),
            BillPerson("2", "Sam", 1_000_000_000L)
        )
        val result = BillSplitCalculator.splitByShares(billMinorUnits, people, usd)

        assertEquals(500_000_000_000L, result.people[0].allocatedMinorUnits)
        assertEquals(500_000_000_000L, result.people[1].allocatedMinorUnits)
        assertEquals(billMinorUnits, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 16. Total Invariant Independent Test
    @Test
    fun testTotalInvariantAcrossVariousCombinations() {
        val testBills = listOf(1L, 2L, 7L, 100L, 999L, 10000L, 77777L, 123456789L)
        val testCounts = listOf(2, 3, 4, 7, 13, 25, 50)

        for (bill in testBills) {
            for (count in testCounts) {
                val equalResult = BillSplitCalculator.splitEqually(bill, count, usd)
                assertEquals("Failed equal invariant for bill=$bill count=$count", bill, equalResult.people.sumOf { it.allocatedMinorUnits })
            }
        }
    }

    // 17. Deterministic Rounding Critical Test
    @Test
    fun testCriticalDeterministicRounding() {
        val bill = 10000L
        val people = listOf(
            BillPerson("1", "Alex", 3333L),
            BillPerson("2", "Sam", 3333L),
            BillPerson("3", "John", 3334L)
        )

        val firstRun = BillSplitCalculator.splitByPercentages(bill, people, usd)
        for (i in 0 until 10) {
            val nextRun = BillSplitCalculator.splitByPercentages(bill, people, usd)
            assertEquals(firstRun.people.map { it.allocatedMinorUnits }, nextRun.people.map { it.allocatedMinorUnits })
        }
    }

    // 18. Custom Currency Test
    @Test
    fun testCustomCurrencyOption() {
        val customCurrency = CurrencyOption("KRW", "₩", "Korean Won", 0)
        val result = BillSplitCalculator.splitEqually(15000L, 3, customCurrency)
        assertEquals("₩5,000", result.people[0].formattedAmount)
    }

    // 19. Custom 3-decimal Currency Test
    @Test
    fun testThreeDecimalCurrency() {
        val kwd = CurrencyOption("KWD", "KD", "Kuwaiti Dinar", 3)
        val result = BillSplitCalculator.splitEqually(10000L, 3, kwd) // 10.000 KD / 3 = 3.334, 3.333, 3.333
        assertEquals(3334L, result.people[0].allocatedMinorUnits)
        assertEquals("KD 3.334", result.people[0].formattedAmount)
        assertEquals(10000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 20. Person Names & Empty Names fallback
    @Test
    fun testPersonNamesHandling() {
        val names = listOf("Alice", "", "   ", "David")
        val result = BillSplitCalculator.splitEqually(4000L, 4, usd, names)
        assertEquals("Alice", result.people[0].name)
        assertEquals("Person 2", result.people[1].name)
        assertEquals("Person 3", result.people[2].name)
        assertEquals("David", result.people[3].name)
    }

    // 21. Copy and Share Formatting Tests
    @Test
    fun testCopyAndShareFormatting() {
        val result = BillSplitCalculator.splitEqually(12000L, 4, usd, listOf("Alex", "Sam", "John", "Sarah"))
        val copyText = BillSplitFormatter.formatCopyText(result)
        assertTrue(copyText.contains("Total bill: $120.00"))
        assertTrue(copyText.contains("People: 4"))
        assertTrue(copyText.contains("Method: Equal"))
        assertTrue(copyText.contains("Alex: $30.00"))
        assertTrue(copyText.contains("Total allocated: $120.00"))
        assertTrue(copyText.contains("Remaining: $0.00"))

        val shareText = BillSplitFormatter.formatShareText(result)
        assertTrue(shareText.contains("BillSplit"))
        assertTrue(shareText.contains("Total: $120.00"))
        assertTrue(shareText.contains("Each person pays $30.00."))
    }
}
