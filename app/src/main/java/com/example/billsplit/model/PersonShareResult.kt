package com.example.billsplit.model

data class PersonShareResult(
    val id: String,
    val name: String,
    val allocationUnits: Long,
    val allocatedMinorUnits: Long,
    val formattedAmount: String,
    val effectivePercentage: Double = 0.0,
    val shareRatioText: String = ""
)
