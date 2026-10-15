package com.example.billsplit.domain

import com.example.billsplit.model.BillSplitResult
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.SplitMethod

object BillSplitFormatter {

    fun formatMoney(minorUnits: Long, currency: CurrencyOption): String {
        return MoneyFormatter.format(minorUnits, currency)
    }

    fun formatPercentage(value: Int): String {
        return "$value%"
    }

    fun formatPercentage(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f%%", value)
        } else {
            String.format(java.util.Locale.US, "%.2f%%", value)
        }
    }

    fun formatShareCount(shares: Long): String {
        return if (shares == 1L) "1 share" else "$shares shares"
    }

    fun formatCopyText(result: BillSplitResult): String {
        val totalFormatted = formatMoney(result.totalMinorUnits, result.currency)
        val allocatedFormatted = formatMoney(result.totalAllocatedMinorUnits, result.currency)
        val remainingFormatted = formatMoney(result.remainderMinorUnits, result.currency)

        val sb = StringBuilder()
        sb.append("BillSplit Result\n\n")
        sb.append("Total bill: ").append(totalFormatted).append("\n")

        when (result.method) {
            SplitMethod.EQUAL -> {
                sb.append("People: ").append(result.people.size).append("\n")
                sb.append("Method: Equal\n\n")
                for (person in result.people) {
                    sb.append(person.name).append(": ").append(person.formattedAmount).append("\n")
                }
                sb.append("\nTotal allocated: ").append(allocatedFormatted).append("\n")
                sb.append("Remaining: ").append(remainingFormatted)
            }
            SplitMethod.SHARES -> {
                sb.append("Method: Custom Shares\n\n")
                for (person in result.people) {
                    val shareStr = formatShareCount(person.allocationUnits)
                    sb.append(person.name).append(" — ").append(shareStr).append(" — ").append(person.formattedAmount).append("\n")
                }
                sb.append("\nTotal allocated: ").append(allocatedFormatted)
            }
            SplitMethod.PERCENTAGES -> {
                sb.append("Method: Custom Percentages\n\n")
                for (person in result.people) {
                    val pctStr = BillSplitValidator.formatBasisPointsToPercentString(person.allocationUnits)
                    sb.append(person.name).append(" — ").append(pctStr).append(" — ").append(person.formattedAmount).append("\n")
                }
                sb.append("\nTotal allocated: ").append(allocatedFormatted)
            }
        }

        return sb.toString()
    }

    fun formatShareText(result: BillSplitResult): String {
        val totalFormatted = formatMoney(result.totalMinorUnits, result.currency)
        val sb = StringBuilder()
        sb.append("BillSplit\n\n")
        sb.append("Total: ").append(totalFormatted).append("\n\n")

        when (result.method) {
            SplitMethod.EQUAL -> {
                for (person in result.people) {
                    sb.append(person.name).append(": ").append(person.formattedAmount).append("\n")
                }
                sb.append("\n")
                if (result.isExactPerPerson) {
                    val eachAmount = if (result.people.isNotEmpty()) result.people[0].formattedAmount else ""
                    sb.append("Each person pays ").append(eachAmount).append(".")
                } else {
                    sb.append("Everyone pays about ").append(MoneyFormatter.format(result.totalMinorUnits / result.people.size, result.currency)).append(" each.")
                }
            }
            SplitMethod.SHARES -> {
                for (person in result.people) {
                    val shareStr = formatShareCount(person.allocationUnits)
                    sb.append(person.name).append(" (").append(shareStr).append("): ").append(person.formattedAmount).append("\n")
                }
            }
            SplitMethod.PERCENTAGES -> {
                for (person in result.people) {
                    val pctStr = BillSplitValidator.formatBasisPointsToPercentString(person.allocationUnits)
                    sb.append(person.name).append(" (").append(pctStr).append("): ").append(person.formattedAmount).append("\n")
                }
            }
        }

        return sb.toString()
    }
}
