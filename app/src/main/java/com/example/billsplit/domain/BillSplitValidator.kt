package com.example.billsplit.domain

import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.CurrencyOption
import java.util.Locale

object BillSplitValidator {

    const val MIN_PEOPLE = 2
    const val MAX_PEOPLE = 50
    const val MAX_SHARE_VALUE = 1_000_000_000L
    const val TOTAL_PERCENTAGE_BASIS_POINTS = 10000L // 100.00%

    sealed interface ValidationResult<out T> {
        data class Valid<T>(val value: T) : ValidationResult<T>
        data class Invalid(val message: String) : ValidationResult<Nothing>
    }

    fun validateBill(input: String, currency: CurrencyOption): ValidationResult<Long> {
        when (val parseResult = MoneyParser.parse(input, currency)) {
            is MoneyParser.ParseResult.Error -> return ValidationResult.Invalid(parseResult.message)
            is MoneyParser.ParseResult.Success -> {
                if (parseResult.minorUnits <= 0L) {
                    return ValidationResult.Invalid("Enter a valid bill amount.")
                }
                return ValidationResult.Valid(parseResult.minorUnits)
            }
        }
    }

    fun validatePeopleCount(count: Int): ValidationResult<Int> {
        return when {
            count < MIN_PEOPLE -> ValidationResult.Invalid("At least 2 people are required.")
            count > MAX_PEOPLE -> ValidationResult.Invalid("Maximum 50 people.")
            else -> ValidationResult.Valid(count)
        }
    }

    fun validateShares(people: List<BillPerson>): ValidationResult<List<BillPerson>> {
        if (people.size < MIN_PEOPLE) {
            return ValidationResult.Invalid("At least 2 people are required.")
        }
        if (people.size > MAX_PEOPLE) {
            return ValidationResult.Invalid("Maximum 50 people.")
        }

        var totalShares = 0L
        for (person in people) {
            if (person.share <= 0L) {
                return ValidationResult.Invalid("Shares must be greater than zero.")
            }
            if (person.share > MAX_SHARE_VALUE) {
                return ValidationResult.Invalid("Shares must not exceed 1,000,000,000.")
            }
            try {
                totalShares = Math.addExact(totalShares, person.share)
            } catch (_: ArithmeticException) {
                return ValidationResult.Invalid("Unable to calculate the split.")
            }
        }

        if (totalShares <= 0L) {
            return ValidationResult.Invalid("Shares must be greater than zero.")
        }

        return ValidationResult.Valid(people)
    }

    fun parsePercentageInput(input: String): Long? {
        val trimmed = input.trim().replace("%", "").replace(",", "")
        if (trimmed.isEmpty()) return null
        if (trimmed.contains("-")) return null

        val parts = trimmed.split(".")
        if (parts.size > 2) return null

        val wholePartStr = parts[0]
        val wholePart = if (wholePartStr.isEmpty()) 0L else {
            wholePartStr.toLongOrNull() ?: return null
        }

        var fractionUnits = 0L
        if (parts.size == 2) {
            val fractionPartStr = parts[1]
            if (!fractionPartStr.all { it.isDigit() }) return null
            if (fractionPartStr.length > 2) return null
            val paddedFraction = fractionPartStr.padEnd(2, '0')
            fractionUnits = paddedFraction.toLong()
        }

        val totalUnits = wholePart * 100L + fractionUnits
        if (totalUnits < 0L || totalUnits > TOTAL_PERCENTAGE_BASIS_POINTS) return null
        return totalUnits
    }

    fun validatePercentages(people: List<BillPerson>): ValidationResult<List<BillPerson>> {
        if (people.size < MIN_PEOPLE) {
            return ValidationResult.Invalid("At least 2 people are required.")
        }
        if (people.size > MAX_PEOPLE) {
            return ValidationResult.Invalid("Maximum 50 people.")
        }

        var totalPercentage = 0L
        for (person in people) {
            if (person.share < 0L) {
                return ValidationResult.Invalid("Enter a valid percentage.")
            }
            if (person.share > TOTAL_PERCENTAGE_BASIS_POINTS) {
                return ValidationResult.Invalid("Enter a valid percentage.")
            }
            totalPercentage += person.share
        }

        if (totalPercentage != TOTAL_PERCENTAGE_BASIS_POINTS) {
            return ValidationResult.Invalid("Percentages must total 100%.")
        }

        return ValidationResult.Valid(people)
    }

    fun formatBasisPointsToPercentString(basisPoints: Long): String {
        val whole = basisPoints / 100L
        val fraction = basisPoints % 100L
        return if (fraction == 0L) {
            "$whole%"
        } else if (fraction % 10L == 0L) {
            String.format(Locale.US, "%d.%d%%", whole, fraction / 10L)
        } else {
            String.format(Locale.US, "%d.%02d%%", whole, fraction)
        }
    }
}
