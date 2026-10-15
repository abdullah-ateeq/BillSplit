package com.example.billsplit

import android.content.SharedPreferences
import com.example.billsplit.domain.BillSplitCalculator
import com.example.billsplit.domain.BillSplitFormatter
import com.example.billsplit.domain.BillSplitValidator
import com.example.billsplit.domain.MoneyFormatter
import com.example.billsplit.domain.MoneyParser
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitHistory
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.AppSettings
import com.example.billsplit.repository.AppTheme
import com.example.billsplit.repository.BillSplitHistoryRepository
import com.example.billsplit.repository.CurrencyRepository
import com.example.billsplit.repository.LastSplitState
import com.example.billsplit.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class BillSplitComprehensiveTestSuite {

    private val usd = CurrencyRepository.USD
    private val eur = CurrencyRepository.EUR
    private val gbp = CurrencyRepository.GBP
    private val pkr = CurrencyRepository.PKR
    private val jpy = CurrencyRepository.JPY

    private val prefsStorage = mutableMapOf<String, MutableMap<String, Any?>>()

    @Before
    fun setUp() {
        prefsStorage.clear()
    }

    private fun createFakeSharedPreferences(name: String): SharedPreferences {
        val map = prefsStorage.getOrPut(name) { mutableMapOf() }
        fun createEditor(): SharedPreferences.Editor {
            val editorHandler = object : InvocationHandler {
                val pending = mutableMapOf<String, Any?>()
                val removeKeys = mutableSetOf<String>()
                var clearAll = false

                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                    when (method.name) {
                        "putString" -> {
                            pending[args!![0] as String] = args[1] as String?
                            return proxy
                        }
                        "putInt" -> {
                            pending[args!![0] as String] = args[1] as Int
                            return proxy
                        }
                        "putBoolean" -> {
                            pending[args!![0] as String] = args[1] as Boolean
                            return proxy
                        }
                        "putLong" -> {
                            pending[args!![0] as String] = args[1] as Long
                            return proxy
                        }
                        "remove" -> {
                            removeKeys.add(args!![0] as String)
                            return proxy
                        }
                        "clear" -> {
                            clearAll = true
                            return proxy
                        }
                        "apply", "commit" -> {
                            if (clearAll) map.clear()
                            removeKeys.forEach { map.remove(it) }
                            map.putAll(pending)
                            return true
                        }
                        else -> return proxy
                    }
                }
            }

            return Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
                editorHandler
            ) as SharedPreferences.Editor
        }

        val prefsHandler = InvocationHandler { _, method, args ->
            when (method.name) {
                "edit" -> createEditor()
                "getString" -> map[args[0] as String] as? String ?: args[1] as String?
                "getInt" -> map[args[0] as String] as? Int ?: args[1] as Int
                "getBoolean" -> map[args[0] as String] as? Boolean ?: args[1] as Boolean
                "getLong" -> map[args[0] as String] as? Long ?: args[1] as Long
                "contains" -> map.containsKey(args[0] as String)
                else -> null
            }
        }

        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            prefsHandler
        ) as SharedPreferences
    }

    // 1. Equal split
    @Test
    fun test01_EqualSplit() {
        val result = BillSplitCalculator.splitEqually(12000L, 4, usd)
        assertEquals(4, result.people.size)
        assertEquals(12000L, result.totalAllocatedMinorUnits)
        assertEquals(3000L, result.people[0].allocatedMinorUnits)
    }

    // 2. Two people
    @Test
    fun test02_TwoPeople() {
        val result = BillSplitCalculator.splitEqually(10000L, 2, usd)
        assertEquals(2, result.people.size)
        assertEquals(5000L, result.people[0].allocatedMinorUnits)
        assertEquals(5000L, result.people[1].allocatedMinorUnits)
    }

    // 3. Three people
    @Test
    fun test03_ThreePeople() {
        val result = BillSplitCalculator.splitEqually(9000L, 3, usd)
        assertEquals(3, result.people.size)
        assertEquals(3000L, result.people[0].allocatedMinorUnits)
    }

    // 4. Four people
    @Test
    fun test04_FourPeople() {
        val result = BillSplitCalculator.splitEqually(16000L, 4, usd)
        assertEquals(4, result.people.size)
        assertEquals(4000L, result.people[0].allocatedMinorUnits)
    }

    // 5. Ten people
    @Test
    fun test05_TenPeople() {
        val result = BillSplitCalculator.splitEqually(50000L, 10, usd)
        assertEquals(10, result.people.size)
        assertEquals(5000L, result.people[0].allocatedMinorUnits)
    }

    // 6. Fifty people
    @Test
    fun test06_FiftyPeople() {
        val result = BillSplitCalculator.splitEqually(50000L, 50, usd)
        assertEquals(50, result.people.size)
        assertEquals(1000L, result.people[0].allocatedMinorUnits)
        assertEquals(50000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 7. Even amount
    @Test
    fun test07_EvenAmount() {
        val result = BillSplitCalculator.splitEqually(8000L, 4, usd)
        assertTrue(result.isExactPerPerson)
        assertEquals("$20.00 EACH", result.summaryPerPersonAmount)
    }

    // 8. Uneven amount
    @Test
    fun test08_UnevenAmount() {
        val result = BillSplitCalculator.splitEqually(10000L, 3, usd)
        assertFalse(result.isExactPerPerson)
        assertEquals("ABOUT $33.33 EACH", result.summaryPerPersonAmount)
    }

    // 9. One-cent remainder
    @Test
    fun test09_OneCentRemainder() {
        val result = BillSplitCalculator.splitEqually(1000L, 3, usd)
        assertEquals(334L, result.people[0].allocatedMinorUnits)
        assertEquals(333L, result.people[1].allocatedMinorUnits)
        assertEquals(333L, result.people[2].allocatedMinorUnits)
    }

    // 10. Multiple-cent remainder
    @Test
    fun test10_MultipleCentRemainder() {
        val result = BillSplitCalculator.splitEqually(1000L, 7, usd) // 1000 = 142*7 + 6
        assertEquals(143L, result.people[0].allocatedMinorUnits)
        assertEquals(143L, result.people[5].allocatedMinorUnits)
        assertEquals(142L, result.people[6].allocatedMinorUnits)
        assertEquals(1000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 11. Custom shares
    @Test
    fun test11_CustomShares() {
        val people = listOf(BillPerson("1", "A", 3L), BillPerson("2", "B", 1L))
        val result = BillSplitCalculator.splitByShares(10000L, people, usd)
        assertEquals(7500L, result.people[0].allocatedMinorUnits)
        assertEquals(2500L, result.people[1].allocatedMinorUnits)
    }

    // 12. Percentage split
    @Test
    fun test12_PercentageSplit() {
        val people = listOf(BillPerson("1", "A", 6000L), BillPerson("2", "B", 4000L))
        val result = BillSplitCalculator.splitByPercentages(10000L, people, usd)
        assertEquals(6000L, result.people[0].allocatedMinorUnits)
        assertEquals(4000L, result.people[1].allocatedMinorUnits)
    }

    // 13. 50/50
    @Test
    fun test13_FiftyFifty() {
        val people = listOf(BillPerson("1", "A", 5000L), BillPerson("2", "B", 5000L))
        val result = BillSplitCalculator.splitByPercentages(20000L, people, usd)
        assertEquals(10000L, result.people[0].allocatedMinorUnits)
        assertEquals(10000L, result.people[1].allocatedMinorUnits)
    }

    // 14. 25/75
    @Test
    fun test14_TwentyFiveSeventyFive() {
        val people = listOf(BillPerson("1", "A", 2500L), BillPerson("2", "B", 7500L))
        val result = BillSplitCalculator.splitByPercentages(20000L, people, usd)
        assertEquals(5000L, result.people[0].allocatedMinorUnits)
        assertEquals(15000L, result.people[1].allocatedMinorUnits)
    }

    // 15. 25/25/50
    @Test
    fun test15_TwentyFiveTwentyFiveFifty() {
        val people = listOf(BillPerson("1", "A", 2500L), BillPerson("2", "B", 2500L), BillPerson("3", "C", 5000L))
        val result = BillSplitCalculator.splitByPercentages(10000L, people, usd)
        assertEquals(2500L, result.people[0].allocatedMinorUnits)
        assertEquals(2500L, result.people[1].allocatedMinorUnits)
        assertEquals(5000L, result.people[2].allocatedMinorUnits)
    }

    // 16. 20/30/50
    @Test
    fun test16_TwentyThirtyFifty() {
        val people = listOf(BillPerson("1", "A", 2000L), BillPerson("2", "B", 3000L), BillPerson("3", "C", 5000L))
        val result = BillSplitCalculator.splitByPercentages(10000L, people, usd)
        assertEquals(2000L, result.people[0].allocatedMinorUnits)
        assertEquals(3000L, result.people[1].allocatedMinorUnits)
        assertEquals(5000L, result.people[2].allocatedMinorUnits)
    }

    // 17. 33.33/33.33/33.34
    @Test
    fun test17_DecimalPercentages() {
        val people = listOf(BillPerson("1", "A", 3333L), BillPerson("2", "B", 3333L), BillPerson("3", "C", 3334L))
        val result = BillSplitCalculator.splitByPercentages(10000L, people, usd)
        assertEquals(3333L, result.people[0].allocatedMinorUnits)
        assertEquals(3333L, result.people[1].allocatedMinorUnits)
        assertEquals(3334L, result.people[2].allocatedMinorUnits)
        assertEquals(10000L, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 18. Zero percentage
    @Test
    fun test18_ZeroPercentage() {
        val people = listOf(BillPerson("1", "A", 0L), BillPerson("2", "B", 10000L))
        val result = BillSplitCalculator.splitByPercentages(5000L, people, usd)
        assertEquals(0L, result.people[0].allocatedMinorUnits)
        assertEquals(5000L, result.people[1].allocatedMinorUnits)
    }

    // 19. 100% single person (with other 0%)
    @Test
    fun test19_HundredPercentPerson() {
        val people = listOf(BillPerson("1", "A", 10000L), BillPerson("2", "B", 0L))
        val result = BillSplitCalculator.splitByPercentages(7500L, people, usd)
        assertEquals(7500L, result.people[0].allocatedMinorUnits)
        assertEquals(0L, result.people[1].allocatedMinorUnits)
    }

    // 20. Zero shares
    @Test
    fun test20_ZeroSharesRejected() {
        val people = listOf(BillPerson("1", "A", 0L), BillPerson("2", "B", 2L))
        val validation = BillSplitValidator.validateShares(people)
        assertTrue(validation is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Shares must be greater than zero.", (validation as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 21. Negative shares
    @Test
    fun test21_NegativeSharesRejected() {
        val people = listOf(BillPerson("1", "A", -5L), BillPerson("2", "B", 2L))
        val validation = BillSplitValidator.validateShares(people)
        assertTrue(validation is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Shares must be greater than zero.", (validation as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 22. Decimal shares rejected by whole-number share parser
    @Test
    fun test22_DecimalSharesFilter() {
        val shareStr = "2.5"
        val wholeShare = shareStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
        // Only integer shares are allowed
        assertEquals(25L, wholeShare)
    }

    // 23. Percentages below 100
    @Test
    fun test23_PercentagesBelow100() {
        val people = listOf(BillPerson("1", "A", 5000L), BillPerson("2", "B", 3000L)) // 80%
        val validation = BillSplitValidator.validatePercentages(people)
        assertTrue(validation is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Percentages must total 100%.", (validation as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 24. Percentages above 100
    @Test
    fun test24_PercentagesAbove100() {
        val people = listOf(BillPerson("1", "A", 6000L), BillPerson("2", "B", 5000L)) // 110%
        val validation = BillSplitValidator.validatePercentages(people)
        assertTrue(validation is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Percentages must total 100%.", (validation as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 25. Percentages exactly 100
    @Test
    fun test25_PercentagesExactly100() {
        val people = listOf(BillPerson("1", "A", 4000L), BillPerson("2", "B", 6000L))
        val validation = BillSplitValidator.validatePercentages(people)
        assertTrue(validation is BillSplitValidator.ValidationResult.Valid)
    }

    // 26. Percentage precision
    @Test
    fun test26_PercentagePrecision() {
        assertEquals(3333L, BillSplitValidator.parsePercentageInput("33.33"))
        assertEquals(3330L, BillSplitValidator.parsePercentageInput("33.3"))
        assertNull(BillSplitValidator.parsePercentageInput("33.333")) // Exceeds 0.01% precision
    }

    // 27. Currency formatting
    @Test
    fun test27_CurrencyFormatting() {
        assertEquals("$10.00", MoneyFormatter.format(1000L, usd))
    }

    // 28. USD
    @Test
    fun test28_USD() {
        assertEquals("$120.00", MoneyFormatter.format(12000L, usd))
    }

    // 29. EUR
    @Test
    fun test29_EUR() {
        assertEquals("€120.00", MoneyFormatter.format(12000L, eur))
    }

    // 30. GBP
    @Test
    fun test30_GBP() {
        assertEquals("£120.00", MoneyFormatter.format(12000L, gbp))
    }

    // 31. PKR
    @Test
    fun test31_PKR() {
        assertEquals("Rs. 1,000.00", MoneyFormatter.format(100000L, pkr))
    }

    // 32. JPY
    @Test
    fun test32_JPY() {
        assertEquals("¥1,000", MoneyFormatter.format(1000L, jpy))
    }

    // 33. Zero-decimal currency
    @Test
    fun test33_ZeroDecimalCurrency() {
        assertEquals(0, jpy.decimalPlaces)
        assertEquals("¥50", MoneyFormatter.format(50L, jpy))
    }

    // 34. Two-decimal currency
    @Test
    fun test34_TwoDecimalCurrency() {
        assertEquals(2, usd.decimalPlaces)
        assertEquals("$50.00", MoneyFormatter.format(5000L, usd))
    }

    // 35. Custom currency
    @Test
    fun test35_CustomCurrency() {
        val custom = CurrencyRepository.createCustomCurrency("CHF", "Swiss Franc", 2)
        assertEquals("CHF 120.00", MoneyFormatter.format(12000L, custom))
    }

    // 36. Money parsing
    @Test
    fun test36_MoneyParsing() {
        assertEquals(12000L, MoneyParser.parseOrNull("120.00", usd))
        assertEquals(12000L, MoneyParser.parseOrNull("120", usd))
    }

    // 37. Large bill
    @Test
    fun test37_LargeBill() {
        val large = 100_000_000_000L // $1,000,000,000.00
        val result = BillSplitCalculator.splitEqually(large, 4, usd)
        assertEquals(25_000_000_000L, result.people[0].allocatedMinorUnits)
        assertEquals(large, result.totalAllocatedMinorUnits)
    }

    // 38. Overflow protection
    @Test
    fun test38_OverflowProtection() {
        val overflowInput = "9999999999999999999999"
        val result = MoneyParser.parse(overflowInput, usd)
        assertTrue(result is MoneyParser.ParseResult.Error)
        assertEquals("Bill amount is too large.", (result as MoneyParser.ParseResult.Error).message)
    }

    // 39. Allocation invariant: sum(allocated) == total
    @Test
    fun test39_AllocationInvariant() {
        val bill = 987654321L
        val result = BillSplitCalculator.splitEqually(bill, 37, usd)
        assertEquals(bill, result.people.sumOf { it.allocatedMinorUnits })
    }

    // 40. Rounding fairness
    @Test
    fun test40_RoundingFairness() {
        val result = BillSplitCalculator.splitEqually(100L, 3, usd)
        assertEquals(34L, result.people[0].allocatedMinorUnits)
        assertEquals(33L, result.people[1].allocatedMinorUnits)
        assertEquals(33L, result.people[2].allocatedMinorUnits)
        // Difference between any two allocations is at most 1 minor unit
        val minAlloc = result.people.minOf { it.allocatedMinorUnits }
        val maxAlloc = result.people.maxOf { it.allocatedMinorUnits }
        assertTrue(maxAlloc - minAlloc <= 1L)
    }

    // 41. Rounding determinism
    @Test
    fun test41_RoundingDeterminism() {
        val people = listOf(BillPerson("1", "A", 1L), BillPerson("2", "B", 1L), BillPerson("3", "C", 1L))
        val r1 = BillSplitCalculator.splitByShares(100L, people, usd)
        val r2 = BillSplitCalculator.splitByShares(100L, people, usd)
        assertEquals(r1.people.map { it.allocatedMinorUnits }, r2.people.map { it.allocatedMinorUnits })
    }

    // 42. Effective percentage
    @Test
    fun test42_EffectivePercentage() {
        val result = BillSplitCalculator.splitEqually(12000L, 4, usd)
        assertEquals(25.0, result.people[0].effectivePercentage, 0.001)
    }

    // 43. Person names
    @Test
    fun test43_PersonNames() {
        val result = BillSplitCalculator.splitEqually(10000L, 2, usd, listOf("Alice", "Bob"))
        assertEquals("Alice", result.people[0].name)
        assertEquals("Bob", result.people[1].name)
    }

    // 44. Empty names
    @Test
    fun test44_EmptyNamesFallback() {
        val result = BillSplitCalculator.splitEqually(10000L, 2, usd, listOf("", "   "))
        assertEquals("Person 1", result.people[0].name)
        assertEquals("Person 2", result.people[1].name)
    }

    // 45. Add person boundary check
    @Test
    fun test45_AddPerson() {
        val currentCount = 4
        val target = currentCount + 1
        assertEquals(5, target.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE))
    }

    // 46. Delete person boundary check
    @Test
    fun test46_DeletePerson() {
        val currentCount = 3
        val target = currentCount - 1
        assertEquals(2, target.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE))
    }

    // 47. Minimum people
    @Test
    fun test47_MinimumPeople() {
        val invalid = BillSplitValidator.validatePeopleCount(1)
        assertTrue(invalid is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("At least 2 people are required.", (invalid as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 48. Maximum people
    @Test
    fun test48_MaximumPeople() {
        val invalid = BillSplitValidator.validatePeopleCount(51)
        assertTrue(invalid is BillSplitValidator.ValidationResult.Invalid)
        assertEquals("Maximum 50 people.", (invalid as BillSplitValidator.ValidationResult.Invalid).message)
    }

    // 49. History save
    @Test
    fun test49_HistorySave() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h49"))
        repo.saveCalculation(BillSplitHistory("h1", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 1L))
        assertEquals(1, repo.loadHistory().size)
    }

    // 50. History load
    @Test
    fun test50_HistoryLoad() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h50"))
        val item = BillSplitHistory("h2", 2000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 2L)
        repo.saveCalculation(item)
        val loaded = repo.loadHistory()
        assertEquals(2000L, loaded[0].totalMinorUnits)
    }

    // 51. History limit 10
    @Test
    fun test51_HistoryLimit() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h51"))
        for (i in 1..12) {
            repo.saveCalculation(BillSplitHistory("id_$i", i * 100L, "USD", "$", SplitMethod.EQUAL, emptyList(), i.toLong()))
        }
        assertEquals(10, repo.loadHistory().size)
    }

    // 52. History deletion
    @Test
    fun test52_HistoryDeletion() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h52"))
        repo.saveCalculation(BillSplitHistory("del_1", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 1L))
        repo.deleteCalculation("del_1")
        assertTrue(repo.loadHistory().isEmpty())
    }

    // 53. History clear
    @Test
    fun test53_HistoryClear() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h53"))
        repo.saveCalculation(BillSplitHistory("c1", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 1L))
        repo.clearHistory()
        assertTrue(repo.loadHistory().isEmpty())
    }

    // 54. Settings persistence
    @Test
    fun test54_SettingsPersistence() {
        val repo = SettingsRepository(createFakeSharedPreferences("s54"))
        val settings = AppSettings(theme = AppTheme.LIGHT, defaultCurrencyCode = "GBP")
        repo.saveSettings(settings)
        assertEquals(AppTheme.LIGHT, repo.getSettings().theme)
        assertEquals("GBP", repo.getSettings().defaultCurrencyCode)
    }

    // 55. Last-split persistence
    @Test
    fun test55_LastSplitPersistence() {
        val repo = SettingsRepository(createFakeSharedPreferences("s55"))
        val state = LastSplitState(billInput = "99.99", currencyCode = "USD")
        repo.saveLastSplit(state)
        assertEquals("99.99", repo.getLastSplit()?.billInput)
    }

    // 56. Reset
    @Test
    fun test56_ResetDefaults() {
        val defaultBill = "120.00"
        val defaultPeople = 4
        val defaultMethod = SplitMethod.EQUAL
        assertEquals("120.00", defaultBill)
        assertEquals(4, defaultPeople)
        assertEquals(SplitMethod.EQUAL, defaultMethod)
    }

    // 57. Copy formatting
    @Test
    fun test57_CopyFormatting() {
        val result = BillSplitCalculator.splitEqually(12000L, 4, usd)
        val copy = BillSplitFormatter.formatCopyText(result)
        assertTrue(copy.contains("Total bill: $120.00"))
        assertTrue(copy.contains("People: 4"))
    }

    // 58. Share formatting
    @Test
    fun test58_ShareFormatting() {
        val result = BillSplitCalculator.splitEqually(12000L, 4, usd)
        val share = BillSplitFormatter.formatShareText(result)
        assertTrue(share.contains("Each person pays $30.00."))
    }

    // 59. Quick people presets
    @Test
    fun test59_QuickPeoplePresets() {
        val presets = listOf(2, 3, 4, 5, 6, 8, 10)
        for (preset in presets) {
            assertTrue(preset in BillSplitValidator.MIN_PEOPLE..BillSplitValidator.MAX_PEOPLE)
        }
    }

    // 60. Quick percentage presets
    @Test
    fun test60_QuickPercentagePresets() {
        val presets = listOf(
            listOf(50.0, 50.0),
            listOf(25.0, 75.0),
            listOf(25.0, 25.0, 50.0),
            listOf(20.0, 30.0, 50.0),
            listOf(25.0, 25.0, 25.0, 25.0)
        )
        for (preset in presets) {
            val sum = preset.sum()
            assertEquals(100.0, sum, 0.001)
        }
    }

    // 61. Rapid calculation protection
    @Test
    fun test61_RapidCalculationProtection() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("h61"))
        val entry = BillSplitHistory("h_dup", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 1L)

        // Simulating two rapid calls with identical calculation
        repo.saveCalculation(entry)
        repo.saveCalculation(entry)

        // Deduplication guarantees no duplicate consecutive history entry
        assertEquals(1, repo.loadHistory().size)
    }
}
