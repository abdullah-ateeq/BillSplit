package com.example.billsplit.ui.home

import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitHistory
import com.example.billsplit.model.BillSplitResult
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.AppSettings
import com.example.billsplit.repository.CurrencyRepository

enum class PercentageStatusType {
    BALANCED,
    REMAINING,
    OVER
}

data class PercentageStatus(
    val type: PercentageStatusType,
    val differencePercent: Double,
    val displayLabel: String
)

data class BillSplitUiState(
    val billInput: String = "120.00",
    val currency: CurrencyOption = CurrencyRepository.USD,
    val method: SplitMethod = SplitMethod.EQUAL,
    val peopleCount: Int = 4,
    val people: List<BillPerson> = emptyList(),
    val percentageInputs: List<String> = emptyList(),
    val totalPercentage: Double = 100.0,
    val percentageStatus: PercentageStatus = PercentageStatus(PercentageStatusType.BALANCED, 0.0, "BALANCED"),
    val result: BillSplitResult? = null,
    val history: List<BillSplitHistory> = emptyList(),
    val errorMessage: String? = null,
    val isCalculating: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    val isEditNamesExpanded: Boolean = false,
    val equalNames: List<String> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val showCustomCurrencyDialog: Boolean = false,
    val customCurrencySymbolInput: String = "",
    val customCurrencyNameInput: String = "",
    val customCurrencyDecimalsInput: Int = 2
)
