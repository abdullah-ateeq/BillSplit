package com.example.billsplit.model

data class BillSplitHistory(
    val id: String,
    val totalMinorUnits: Long,
    val currencyCode: String,
    val currencySymbol: String,
    val method: SplitMethod,
    val people: List<BillPerson>,
    val calculatedAt: Long
)
