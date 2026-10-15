package com.example.billsplit.model

data class BillSplitResult(
    val totalMinorUnits: Long,
    val people: List<PersonShareResult>,
    val totalAllocatedMinorUnits: Long,
    val remainderMinorUnits: Long,
    val method: SplitMethod,
    val currency: CurrencyOption,
    val roundingAdjustmentNote: String? = null,
    val isExactPerPerson: Boolean = true,
    val summaryPerPersonAmount: String = ""
)
