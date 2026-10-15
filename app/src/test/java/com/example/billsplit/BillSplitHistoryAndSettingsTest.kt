package com.example.billsplit

import android.content.SharedPreferences
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitHistory
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.AppSettings
import com.example.billsplit.repository.AppTheme
import com.example.billsplit.repository.BillSplitHistoryRepository
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

class BillSplitHistoryAndSettingsTest {

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
                "getString" -> {
                    val key = args[0] as String
                    val def = args[1] as String?
                    map[key] as? String ?: def
                }
                "getInt" -> {
                    val key = args[0] as String
                    val def = args[1] as Int
                    map[key] as? Int ?: def
                }
                "getBoolean" -> {
                    val key = args[0] as String
                    val def = args[1] as Boolean
                    map[key] as? Boolean ?: def
                }
                "getLong" -> {
                    val key = args[0] as String
                    val def = args[1] as Long
                    map[key] as? Long ?: def
                }
                "contains" -> {
                    val key = args[0] as String
                    map.containsKey(key)
                }
                else -> null
            }
        }

        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            prefsHandler
        ) as SharedPreferences
    }

    // 1. History save & load
    @Test
    fun testHistorySaveAndLoad() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("history"))
        val entry1 = BillSplitHistory(
            id = "1",
            totalMinorUnits = 12000L,
            currencyCode = "USD",
            currencySymbol = "$",
            method = SplitMethod.EQUAL,
            people = listOf(BillPerson("p1", "Alex", 1L), BillPerson("p2", "Sam", 1L)),
            calculatedAt = 1000L
        )

        repo.saveCalculation(entry1)
        val loaded = repo.loadHistory()
        assertEquals(1, loaded.size)
        assertEquals(entry1.id, loaded[0].id)
        assertEquals(12000L, loaded[0].totalMinorUnits)
        assertEquals(2, loaded[0].people.size)
    }

    // 2. History limit: 10 items, 11th removes oldest
    @Test
    fun testHistoryLimit10Items() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("history"))
        for (i in 1..15) {
            val entry = BillSplitHistory(
                id = "id_$i",
                totalMinorUnits = i * 1000L,
                currencyCode = "USD",
                currencySymbol = "$",
                method = SplitMethod.EQUAL,
                people = listOf(BillPerson("p1", "Person 1", 1L), BillPerson("p2", "Person 2", 1L)),
                calculatedAt = i * 1000L
            )
            repo.saveCalculation(entry)
        }

        val loaded = repo.loadHistory()
        assertEquals(10, loaded.size)
        // Newest should be id_15, oldest in list should be id_6
        assertEquals("id_15", loaded.first().id)
        assertEquals("id_6", loaded.last().id)
    }

    // 3. History deletion
    @Test
    fun testHistorySingleItemDeletion() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("history"))
        val e1 = BillSplitHistory("1", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 100L)
        val e2 = BillSplitHistory("2", 2000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 200L)

        repo.saveCalculation(e1)
        repo.saveCalculation(e2)

        assertEquals(2, repo.loadHistory().size)
        repo.deleteCalculation("1")

        val loaded = repo.loadHistory()
        assertEquals(1, loaded.size)
        assertEquals("2", loaded[0].id)
    }

    // 4. History clear
    @Test
    fun testHistoryClear() {
        val repo = BillSplitHistoryRepository(createFakeSharedPreferences("history"))
        val e1 = BillSplitHistory("1", 1000L, "USD", "$", SplitMethod.EQUAL, emptyList(), 100L)
        repo.saveCalculation(e1)
        assertEquals(1, repo.loadHistory().size)

        repo.clearHistory()
        assertTrue(repo.loadHistory().isEmpty())
    }

    // 5. Settings persistence
    @Test
    fun testSettingsPersistence() {
        val repo = SettingsRepository(createFakeSharedPreferences("settings"))
        val customSettings = AppSettings(
            theme = AppTheme.DARK,
            dynamicColor = false,
            hapticFeedback = false,
            animations = false,
            defaultCurrencyCode = "PKR",
            defaultSplitMethod = SplitMethod.SHARES,
            confirmHistoryDelete = false,
            preserveLastSplit = false
        )

        repo.saveSettings(customSettings)
        val loaded = repo.getSettings()

        assertEquals(AppTheme.DARK, loaded.theme)
        assertFalse(loaded.dynamicColor)
        assertFalse(loaded.hapticFeedback)
        assertFalse(loaded.animations)
        assertEquals("PKR", loaded.defaultCurrencyCode)
        assertEquals(SplitMethod.SHARES, loaded.defaultSplitMethod)
        assertFalse(loaded.confirmHistoryDelete)
        assertFalse(loaded.preserveLastSplit)
    }

    // 6. Settings Reset
    @Test
    fun testSettingsReset() {
        val repo = SettingsRepository(createFakeSharedPreferences("settings"))
        val custom = AppSettings(theme = AppTheme.DARK, defaultCurrencyCode = "EUR")
        repo.saveSettings(custom)

        val reset = repo.resetSettings()
        assertEquals(AppTheme.SYSTEM, reset.theme)
        assertEquals("USD", reset.defaultCurrencyCode)
        assertTrue(reset.dynamicColor)
    }

    // 7. Last-Split persistence
    @Test
    fun testLastSplitPersistence() {
        val repo = SettingsRepository(createFakeSharedPreferences("settings"))
        val state = LastSplitState(
            billInput = "250.75",
            currencyCode = "EUR",
            customSymbol = "€",
            customDecimals = 2,
            method = SplitMethod.PERCENTAGES,
            peopleCount = 3,
            people = listOf(
                BillPerson("1", "Alice", 5000L),
                BillPerson("2", "Bob", 3000L),
                BillPerson("3", "Charlie", 2000L)
            ),
            percentageInputs = listOf("50", "30", "20")
        )

        repo.saveLastSplit(state)
        val loaded = repo.getLastSplit()

        assertNotNull(loaded)
        assertEquals("250.75", loaded!!.billInput)
        assertEquals("EUR", loaded.currencyCode)
        assertEquals(SplitMethod.PERCENTAGES, loaded.method)
        assertEquals(3, loaded.people.size)
        assertEquals("Alice", loaded.people[0].name)
        assertEquals(5000L, loaded.people[0].share)

        repo.clearLastSplit()
        assertNull(repo.getLastSplit())
    }
}
