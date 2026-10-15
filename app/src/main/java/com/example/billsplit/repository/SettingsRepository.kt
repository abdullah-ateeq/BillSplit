package com.example.billsplit.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.SplitMethod

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticFeedback: Boolean = true,
    val animations: Boolean = true,
    val defaultCurrencyCode: String = "USD",
    val defaultSplitMethod: SplitMethod = SplitMethod.EQUAL,
    val confirmHistoryDelete: Boolean = true,
    val preserveLastSplit: Boolean = true
)

data class LastSplitState(
    val billInput: String = "120.00",
    val currencyCode: String = "USD",
    val customSymbol: String = "$",
    val customDecimals: Int = 2,
    val method: SplitMethod = SplitMethod.EQUAL,
    val peopleCount: Int = 4,
    val people: List<BillPerson> = emptyList(),
    val percentageInputs: List<String> = emptyList()
)

class SettingsRepository(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("billsplit_settings", Context.MODE_PRIVATE)
    )

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_DEFAULT_METHOD = "default_method"
        private const val KEY_CONFIRM_DELETE = "confirm_history_delete"
        private const val KEY_PRESERVE_LAST = "preserve_last_split"

        private const val KEY_LAST_BILL = "last_bill"
        private const val KEY_LAST_CURRENCY_CODE = "last_currency_code"
        private const val KEY_LAST_CUSTOM_SYMBOL = "last_custom_symbol"
        private const val KEY_LAST_CUSTOM_DECIMALS = "last_custom_decimals"
        private const val KEY_LAST_METHOD = "last_method"
        private const val KEY_LAST_PEOPLE_COUNT = "last_people_count"
        private const val KEY_LAST_PEOPLE_STR = "last_people_str"
        private const val KEY_LAST_PERCENTAGES_STR = "last_percentages_str"
    }

    fun getSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val theme = try {
            AppTheme.valueOf(themeStr)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }

        val methodStr = prefs.getString(KEY_DEFAULT_METHOD, SplitMethod.EQUAL.name) ?: SplitMethod.EQUAL.name
        val method = try {
            SplitMethod.valueOf(methodStr)
        } catch (_: Exception) {
            SplitMethod.EQUAL
        }

        return AppSettings(
            theme = theme,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            animations = prefs.getBoolean(KEY_ANIMATIONS, true),
            defaultCurrencyCode = prefs.getString(KEY_DEFAULT_CURRENCY, "USD") ?: "USD",
            defaultSplitMethod = method,
            confirmHistoryDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true),
            preserveLastSplit = prefs.getBoolean(KEY_PRESERVE_LAST, true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, settings.theme.name)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            .putBoolean(KEY_ANIMATIONS, settings.animations)
            .putString(KEY_DEFAULT_CURRENCY, settings.defaultCurrencyCode)
            .putString(KEY_DEFAULT_METHOD, settings.defaultSplitMethod.name)
            .putBoolean(KEY_CONFIRM_DELETE, settings.confirmHistoryDelete)
            .putBoolean(KEY_PRESERVE_LAST, settings.preserveLastSplit)
            .apply()
    }

    fun resetSettings(): AppSettings {
        val defaultSettings = AppSettings()
        saveSettings(defaultSettings)
        return defaultSettings
    }

    fun saveLastSplit(state: LastSplitState) {
        val peopleStr = state.people.joinToString(";") { BillSplitHistoryRepository.serializePerson(it) }
        val pctStr = state.percentageInputs.joinToString(",")

        prefs.edit()
            .putString(KEY_LAST_BILL, state.billInput)
            .putString(KEY_LAST_CURRENCY_CODE, state.currencyCode)
            .putString(KEY_LAST_CUSTOM_SYMBOL, state.customSymbol)
            .putInt(KEY_LAST_CUSTOM_DECIMALS, state.customDecimals)
            .putString(KEY_LAST_METHOD, state.method.name)
            .putInt(KEY_LAST_PEOPLE_COUNT, state.peopleCount)
            .putString(KEY_LAST_PEOPLE_STR, peopleStr)
            .putString(KEY_LAST_PERCENTAGES_STR, pctStr)
            .apply()
    }

    fun getLastSplit(): LastSplitState? {
        if (!prefs.contains(KEY_LAST_BILL)) return null

        val bill = prefs.getString(KEY_LAST_BILL, "120.00") ?: "120.00"
        val currencyCode = prefs.getString(KEY_LAST_CURRENCY_CODE, "USD") ?: "USD"
        val customSymbol = prefs.getString(KEY_LAST_CUSTOM_SYMBOL, "$") ?: "$"
        val customDecimals = prefs.getInt(KEY_LAST_CUSTOM_DECIMALS, 2)
        val methodStr = prefs.getString(KEY_LAST_METHOD, SplitMethod.EQUAL.name) ?: SplitMethod.EQUAL.name
        val method = try {
            SplitMethod.valueOf(methodStr)
        } catch (_: Exception) {
            SplitMethod.EQUAL
        }
        val peopleCount = prefs.getInt(KEY_LAST_PEOPLE_COUNT, 4)

        val peopleList = ArrayList<BillPerson>()
        val peopleRaw = prefs.getString(KEY_LAST_PEOPLE_STR, null)
        if (!peopleRaw.isNullOrBlank()) {
            peopleList.addAll(peopleRaw.split(";").mapNotNull { BillSplitHistoryRepository.deserializePerson(it) })
        }

        val pctList = ArrayList<String>()
        val pctRaw = prefs.getString(KEY_LAST_PERCENTAGES_STR, null)
        if (!pctRaw.isNullOrBlank()) {
            pctList.addAll(pctRaw.split(","))
        }

        return LastSplitState(
            billInput = bill,
            currencyCode = currencyCode,
            customSymbol = customSymbol,
            customDecimals = customDecimals,
            method = method,
            peopleCount = peopleCount,
            people = peopleList,
            percentageInputs = pctList
        )
    }

    fun clearLastSplit() {
        prefs.edit()
            .remove(KEY_LAST_BILL)
            .remove(KEY_LAST_CURRENCY_CODE)
            .remove(KEY_LAST_CUSTOM_SYMBOL)
            .remove(KEY_LAST_CUSTOM_DECIMALS)
            .remove(KEY_LAST_METHOD)
            .remove(KEY_LAST_PEOPLE_COUNT)
            .remove(KEY_LAST_PEOPLE_STR)
            .remove(KEY_LAST_PERCENTAGES_STR)
            .apply()
    }
}
