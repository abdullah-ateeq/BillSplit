package com.example.billsplit.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billsplit.domain.BillSplitCalculator
import com.example.billsplit.domain.BillSplitValidator
import com.example.billsplit.domain.MoneyParser
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitHistory
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.AppSettings
import com.example.billsplit.repository.BillSplitHistoryRepository
import com.example.billsplit.repository.CurrencyRepository
import com.example.billsplit.repository.LastSplitState
import com.example.billsplit.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BillSplitViewModel(
    application: Application,
    private val historyRepository: BillSplitHistoryRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        historyRepository = BillSplitHistoryRepository(application),
        settingsRepository = SettingsRepository(application)
    )

    private val _uiState = MutableStateFlow(BillSplitUiState())
    val uiState: StateFlow<BillSplitUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val settings = settingsRepository.getSettings()
        val history = historyRepository.loadHistory()
        val defaultCurrency = CurrencyRepository.findByCode(settings.defaultCurrencyCode)
            ?: CurrencyRepository.DEFAULT_CURRENCY

        if (settings.preserveLastSplit) {
            val lastSplit = settingsRepository.getLastSplit()
            if (lastSplit != null) {
                val currency = if (lastSplit.currencyCode == "CUSTOM") {
                    CurrencyRepository.createCustomCurrency(
                        symbol = lastSplit.customSymbol,
                        decimalPlaces = lastSplit.customDecimals
                    )
                } else {
                    CurrencyRepository.findByCode(lastSplit.currencyCode) ?: defaultCurrency
                }

                val people = if (lastSplit.people.isNotEmpty()) {
                    lastSplit.people
                } else {
                    createDefaultPeople(lastSplit.peopleCount, lastSplit.method)
                }

                val pctInputs = if (lastSplit.percentageInputs.isNotEmpty()) {
                    lastSplit.percentageInputs
                } else {
                    people.map { BillSplitValidator.formatBasisPointsToPercentString(it.share).replace("%", "") }
                }

                _uiState.update {
                    it.copy(
                        billInput = lastSplit.billInput,
                        currency = currency,
                        method = lastSplit.method,
                        peopleCount = lastSplit.peopleCount.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE),
                        people = people,
                        percentageInputs = pctInputs,
                        history = history,
                        settings = settings
                    )
                }
                recalculatePercentageStatus()
                return
            }
        }

        // Default state
        val initialCount = 4
        val defaultPeople = createDefaultPeople(initialCount, settings.defaultSplitMethod)
        val defaultPercentages = listOf("25", "25", "25", "25")

        _uiState.update {
            it.copy(
                billInput = "120.00",
                currency = defaultCurrency,
                method = settings.defaultSplitMethod,
                peopleCount = initialCount,
                people = defaultPeople,
                percentageInputs = defaultPercentages,
                history = history,
                settings = settings
            )
        }
        recalculatePercentageStatus()
    }

    private fun createDefaultPeople(count: Int, method: SplitMethod): List<BillPerson> {
        val safeCount = count.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE)
        val defaultNames = listOf("Alex", "Sam", "John", "Sarah", "Emily", "Michael", "David", "Jessica", "Daniel", "Lisa")

        val list = ArrayList<BillPerson>(safeCount)
        val equalBasisShare = (10000L / safeCount)

        for (i in 0 until safeCount) {
            val name = defaultNames.getOrElse(i) { "Person ${i + 1}" }
            val share = when (method) {
                SplitMethod.EQUAL -> 1L
                SplitMethod.SHARES -> 1L
                SplitMethod.PERCENTAGES -> if (i == safeCount - 1) 10000L - equalBasisShare * (safeCount - 1) else equalBasisShare
            }
            list.add(BillPerson(id = "person_${UUID.randomUUID()}", name = name, share = share))
        }
        return list
    }

    private fun persistLastStateIfEnabled() {
        val current = _uiState.value
        if (current.settings.preserveLastSplit) {
            settingsRepository.saveLastSplit(
                LastSplitState(
                    billInput = current.billInput,
                    currencyCode = current.currency.code,
                    customSymbol = current.currency.symbol,
                    customDecimals = current.currency.decimalPlaces,
                    method = current.method,
                    peopleCount = current.peopleCount,
                    people = current.people,
                    percentageInputs = current.percentageInputs
                )
            )
        }
    }

    // Input handlers
    fun onBillChanged(newInput: String) {
        _uiState.update {
            it.copy(
                billInput = newInput,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        persistLastStateIfEnabled()
    }

    fun onCurrencyChanged(newCurrency: CurrencyOption) {
        _uiState.update {
            it.copy(
                currency = newCurrency,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null,
                showCustomCurrencyDialog = false
            )
        }
        persistLastStateIfEnabled()
    }

    fun showCustomCurrencyDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomCurrencyDialog = show) }
    }

    fun applyCustomCurrency(symbol: String, name: String, decimalPlaces: Int) {
        val custom = CurrencyRepository.createCustomCurrency(symbol, name, decimalPlaces)
        onCurrencyChanged(custom)
    }

    fun onMethodChanged(newMethod: SplitMethod) {
        if (_uiState.value.method == newMethod) return

        val currentPeople = _uiState.value.people
        val count = _uiState.value.peopleCount
        val updatedPeople = ArrayList<BillPerson>(count)
        val defaultPercentages = ArrayList<String>(count)
        val equalBasis = 10000L / count

        for (i in 0 until count) {
            val existing = currentPeople.getOrNull(i)
            val name = existing?.name ?: "Person ${i + 1}"
            val id = existing?.id ?: "person_${UUID.randomUUID()}"
            val share = when (newMethod) {
                SplitMethod.EQUAL -> 1L
                SplitMethod.SHARES -> 1L
                SplitMethod.PERCENTAGES -> if (i == count - 1) 10000L - equalBasis * (count - 1) else equalBasis
            }
            updatedPeople.add(BillPerson(id = id, name = name, share = share))
            val pctStr = BillSplitValidator.formatBasisPointsToPercentString(share).replace("%", "")
            defaultPercentages.add(pctStr)
        }

        _uiState.update {
            it.copy(
                method = newMethod,
                people = updatedPeople,
                percentageInputs = defaultPercentages,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun onPeopleCountChanged(newCount: Int) {
        val target = newCount.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE)
        val current = _uiState.value
        if (current.peopleCount == target && current.people.size == target) return

        val updatedPeople = ArrayList(current.people)
        val updatedPctInputs = ArrayList(current.percentageInputs)

        while (updatedPeople.size < target) {
            val idx = updatedPeople.size
            updatedPeople.add(
                BillPerson(
                    id = "person_${UUID.randomUUID()}",
                    name = "Person ${idx + 1}",
                    share = if (current.method == SplitMethod.SHARES) 1L else 0L
                )
            )
            updatedPctInputs.add("0")
        }

        while (updatedPeople.size > target) {
            updatedPeople.removeAt(updatedPeople.lastIndex)
            if (updatedPctInputs.isNotEmpty()) {
                updatedPctInputs.removeAt(updatedPctInputs.lastIndex)
            }
        }

        _uiState.update {
            it.copy(
                peopleCount = target,
                people = updatedPeople,
                percentageInputs = updatedPctInputs,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun applyPeoplePreset(count: Int) {
        onPeopleCountChanged(count)
    }

    fun applyPercentagePreset(preset: List<Double>) {
        val targetCount = preset.size.coerceIn(BillSplitValidator.MIN_PEOPLE, BillSplitValidator.MAX_PEOPLE)
        val updatedPeople = ArrayList<BillPerson>(targetCount)
        val updatedPctInputs = ArrayList<String>(targetCount)

        for (i in 0 until targetCount) {
            val pct = preset[i]
            val basisPoints = Math.round(pct * 100.0)
            val currentPerson = _uiState.value.people.getOrNull(i)
            val name = currentPerson?.name ?: "Person ${i + 1}"
            val id = currentPerson?.id ?: "person_${UUID.randomUUID()}"
            updatedPeople.add(BillPerson(id = id, name = name, share = basisPoints))
            val pctStr = if (pct % 1.0 == 0.0) pct.toInt().toString() else pct.toString()
            updatedPctInputs.add(pctStr)
        }

        _uiState.update {
            it.copy(
                peopleCount = targetCount,
                people = updatedPeople,
                percentageInputs = updatedPctInputs,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun onAddPerson() {
        val current = _uiState.value
        if (current.peopleCount >= BillSplitValidator.MAX_PEOPLE) return
        onPeopleCountChanged(current.peopleCount + 1)
    }

    fun onRemovePerson(index: Int) {
        val current = _uiState.value
        if (current.peopleCount <= BillSplitValidator.MIN_PEOPLE) return
        if (index !in current.people.indices) return

        val updatedPeople = current.people.toMutableList()
        val updatedPctInputs = current.percentageInputs.toMutableList()

        updatedPeople.removeAt(index)
        if (index in updatedPctInputs.indices) {
            updatedPctInputs.removeAt(index)
        }

        _uiState.update {
            it.copy(
                peopleCount = updatedPeople.size,
                people = updatedPeople,
                percentageInputs = updatedPctInputs,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun onPersonNameChanged(index: Int, newName: String) {
        val current = _uiState.value
        if (index !in current.people.indices) return

        val updatedPeople = current.people.toMutableList()
        val old = updatedPeople[index]
        updatedPeople[index] = old.copy(name = newName)

        val updatedEqualNames = current.equalNames.toMutableList()
        while (updatedEqualNames.size <= index) {
            updatedEqualNames.add("")
        }
        updatedEqualNames[index] = newName

        _uiState.update {
            it.copy(
                people = updatedPeople,
                equalNames = updatedEqualNames,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        persistLastStateIfEnabled()
    }

    fun onPersonShareChanged(index: Int, newShareStr: String) {
        val current = _uiState.value
        if (index !in current.people.indices) return

        val shareVal = newShareStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val updatedPeople = current.people.toMutableList()
        val old = updatedPeople[index]
        updatedPeople[index] = old.copy(share = shareVal)

        _uiState.update {
            it.copy(
                people = updatedPeople,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        persistLastStateIfEnabled()
    }

    fun onPersonPercentageChanged(index: Int, newPctStr: String) {
        val current = _uiState.value
        if (index !in current.people.indices) return

        val updatedInputs = current.percentageInputs.toMutableList()
        while (updatedInputs.size <= index) {
            updatedInputs.add("0")
        }
        updatedInputs[index] = newPctStr

        val basisPoints = BillSplitValidator.parsePercentageInput(newPctStr) ?: 0L
        val updatedPeople = current.people.toMutableList()
        val old = updatedPeople[index]
        updatedPeople[index] = old.copy(share = basisPoints)

        _uiState.update {
            it.copy(
                percentageInputs = updatedInputs,
                people = updatedPeople,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun toggleEditNames(expanded: Boolean? = null) {
        _uiState.update {
            it.copy(isEditNamesExpanded = expanded ?: !it.isEditNamesExpanded)
        }
    }

    private fun recalculatePercentageStatus() {
        val people = _uiState.value.people
        val totalBasis = people.sumOf { it.share }
        val totalPct = totalBasis.toDouble() / 100.0

        val status = when {
            totalBasis == BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS -> {
                PercentageStatus(PercentageStatusType.BALANCED, 0.0, "BALANCED")
            }
            totalBasis < BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS -> {
                val rem = (BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS - totalBasis).toDouble() / 100.0
                PercentageStatus(PercentageStatusType.REMAINING, rem, "${formatPctVal(rem)} REMAINING")
            }
            else -> {
                val over = (totalBasis - BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS).toDouble() / 100.0
                PercentageStatus(PercentageStatusType.OVER, over, "${formatPctVal(over)} OVER")
            }
        }

        _uiState.update {
            it.copy(
                totalPercentage = totalPct,
                percentageStatus = status
            )
        }
    }

    private fun formatPctVal(v: Double): String {
        return if (v % 1.0 == 0.0) {
            "${v.toInt()}%"
        } else {
            String.format(java.util.Locale.US, "%.2f%%", v)
        }
    }

    // Calculation execution
    fun calculateSplit() {
        val current = _uiState.value
        if (current.isCalculating) return

        _uiState.update { it.copy(isCalculating = true, errorMessage = null) }

        // 1. Validate bill
        val billValidation = BillSplitValidator.validateBill(current.billInput, current.currency)
        if (billValidation is BillSplitValidator.ValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    isCalculating = false,
                    errorMessage = billValidation.message,
                    calculationState = CalculationState.Error(billValidation.message)
                )
            }
            return
        }
        val billUnits = (billValidation as BillSplitValidator.ValidationResult.Valid).value

        // 2. Validate people count
        val countValidation = BillSplitValidator.validatePeopleCount(current.peopleCount)
        if (countValidation is BillSplitValidator.ValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    isCalculating = false,
                    errorMessage = countValidation.message,
                    calculationState = CalculationState.Error(countValidation.message)
                )
            }
            return
        }

        val calculationResult = try {
            when (current.method) {
                SplitMethod.EQUAL -> {
                    val names = if (current.equalNames.isNotEmpty()) {
                        current.equalNames
                    } else {
                        current.people.map { it.name }
                    }
                    BillSplitCalculator.splitEqually(
                        totalMinorUnits = billUnits,
                        peopleCount = current.peopleCount,
                        currency = current.currency,
                        personNames = names
                    )
                }
                SplitMethod.SHARES -> {
                    val sharesValidation = BillSplitValidator.validateShares(current.people)
                    if (sharesValidation is BillSplitValidator.ValidationResult.Invalid) {
                        _uiState.update {
                            it.copy(
                                isCalculating = false,
                                errorMessage = sharesValidation.message,
                                calculationState = CalculationState.Error(sharesValidation.message)
                            )
                        }
                        return
                    }
                    BillSplitCalculator.splitByShares(
                        totalMinorUnits = billUnits,
                        people = current.people,
                        currency = current.currency
                    )
                }
                SplitMethod.PERCENTAGES -> {
                    val pctValidation = BillSplitValidator.validatePercentages(current.people)
                    if (pctValidation is BillSplitValidator.ValidationResult.Invalid) {
                        _uiState.update {
                            it.copy(
                                isCalculating = false,
                                errorMessage = pctValidation.message,
                                calculationState = CalculationState.Error(pctValidation.message)
                            )
                        }
                        return
                    }
                    BillSplitCalculator.splitByPercentages(
                        totalMinorUnits = billUnits,
                        people = current.people,
                        currency = current.currency
                    )
                }
            }
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isCalculating = false,
                    errorMessage = "Unable to calculate the split.",
                    calculationState = CalculationState.Error("Unable to calculate the split.")
                )
            }
            return
        }

        // Save to History
        val historyEntry = BillSplitHistory(
            id = UUID.randomUUID().toString(),
            totalMinorUnits = calculationResult.totalMinorUnits,
            currencyCode = current.currency.code,
            currencySymbol = current.currency.symbol,
            method = current.method,
            people = calculationResult.people.map {
                BillPerson(it.id, it.name, it.allocationUnits)
            },
            calculatedAt = System.currentTimeMillis()
        )
        val updatedHistory = historyRepository.saveCalculation(historyEntry)

        _uiState.update {
            it.copy(
                isCalculating = false,
                result = calculationResult,
                calculationState = CalculationState.Calculated,
                history = updatedHistory,
                errorMessage = null
            )
        }
        persistLastStateIfEnabled()
    }

    fun newSplit() {
        val current = _uiState.value
        val defaultPeople = createDefaultPeople(current.peopleCount, current.method)
        val pctInputs = defaultPeople.map {
            BillSplitValidator.formatBasisPointsToPercentString(it.share).replace("%", "")
        }

        _uiState.update {
            it.copy(
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null,
                people = defaultPeople,
                percentageInputs = pctInputs
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun reset() {
        val defaultCurrency = CurrencyRepository.DEFAULT_CURRENCY
        val count = 4
        val defaultPeople = createDefaultPeople(count, SplitMethod.EQUAL)

        _uiState.update {
            it.copy(
                billInput = "120.00",
                currency = defaultCurrency,
                method = SplitMethod.EQUAL,
                peopleCount = count,
                people = defaultPeople,
                percentageInputs = listOf("25", "25", "25", "25"),
                equalNames = emptyList(),
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null,
                isEditNamesExpanded = false
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
    }

    fun restoreFromHistory(item: BillSplitHistory) {
        val currency = CurrencyRepository.findByCode(item.currencyCode)
            ?: CurrencyRepository.createCustomCurrency(item.currencySymbol)

        val people = item.people
        val pctInputs = people.map {
            BillSplitValidator.formatBasisPointsToPercentString(it.share).replace("%", "")
        }

        val billFormatted = MoneyParser.parse(
            MoneyParser.MAX_MINOR_UNITS.toString(),
            currency
        ).let {
            // Convert minor units to input string
            val safeDec = currency.decimalPlaces.coerceIn(0, 4)
            if (safeDec == 0) {
                item.totalMinorUnits.toString()
            } else {
                var div = 1L
                repeat(safeDec) { div *= 10 }
                val whole = item.totalMinorUnits / div
                val frac = item.totalMinorUnits % div
                String.format(java.util.Locale.US, "%d.%0${safeDec}d", whole, frac)
            }
        }

        _uiState.update {
            it.copy(
                billInput = billFormatted,
                currency = currency,
                method = item.method,
                peopleCount = item.people.size,
                people = people,
                percentageInputs = pctInputs,
                result = null,
                calculationState = CalculationState.Idle,
                errorMessage = null
            )
        }
        recalculatePercentageStatus()
        persistLastStateIfEnabled()
        calculateSplit()
    }

    fun deleteHistoryItem(id: String) {
        val updated = historyRepository.deleteCalculation(id)
        _uiState.update { it.copy(history = updated) }
    }

    fun clearAllHistory() {
        historyRepository.clearHistory()
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun resetSettingsToDefault() {
        val reset = settingsRepository.resetSettings()
        _uiState.update { it.copy(settings = reset) }
    }
}
