package com.example.billsplit.domain

import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitResult
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.model.PersonShareResult
import com.example.billsplit.model.SplitMethod
import java.math.BigInteger

object BillSplitCalculator {

    fun splitEqually(
        totalMinorUnits: Long,
        peopleCount: Int,
        currency: CurrencyOption,
        personNames: List<String> = emptyList()
    ): BillSplitResult {
        require(peopleCount in BillSplitValidator.MIN_PEOPLE..BillSplitValidator.MAX_PEOPLE) {
            "People count must be between ${BillSplitValidator.MIN_PEOPLE} and ${BillSplitValidator.MAX_PEOPLE}"
        }
        require(totalMinorUnits >= 0L) { "Total minor units must be non-negative" }

        val base = totalMinorUnits / peopleCount
        val remainder = (totalMinorUnits % peopleCount).toInt()

        val results = ArrayList<PersonShareResult>(peopleCount)
        var allocatedTotal = 0L

        for (i in 0 until peopleCount) {
            val extra = if (i < remainder) 1L else 0L
            val allocated = base + extra
            allocatedTotal += allocated

            val rawName = personNames.getOrNull(i)?.trim().orEmpty()
            val displayName = rawName.ifEmpty { "Person ${i + 1}" }

            val effectivePct = if (totalMinorUnits > 0L) {
                (allocated.toDouble() * 100.0) / totalMinorUnits.toDouble()
            } else {
                100.0 / peopleCount.toDouble()
            }

            results.add(
                PersonShareResult(
                    id = "person_$i",
                    name = displayName,
                    allocationUnits = 1L,
                    allocatedMinorUnits = allocated,
                    formattedAmount = MoneyFormatter.format(allocated, currency),
                    effectivePercentage = effectivePct,
                    shareRatioText = "Equal share"
                )
            )
        }

        // Core Invariant Check
        if (allocatedTotal != totalMinorUnits) {
            throw IllegalStateException("Allocation invariant failed: $allocatedTotal != $totalMinorUnits")
        }

        val isExact = remainder == 0
        val baseFormatted = MoneyFormatter.format(base, currency)
        val summaryAmount = if (isExact) "$baseFormatted EACH" else "ABOUT $baseFormatted EACH"

        val roundingNote = if (isExact) {
            null
        } else {
            val unitName = when (currency.decimalPlaces) {
                2 -> if (currency.code == "USD" || currency.code == "CAD" || currency.code == "AUD") "¢" else " cents"
                0 -> " unit"
                else -> " minor units"
            }
            if (remainder == 1) {
                if (currency.decimalPlaces == 2 && unitName == "¢") {
                    "1¢ added to ${results[0].name} to keep the total exact."
                } else {
                    "1$unitName added to ${results[0].name} to keep the total exact."
                }
            } else {
                "Rounded so the total stays ${MoneyFormatter.format(totalMinorUnits, currency)}."
            }
        }

        return BillSplitResult(
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = allocatedTotal,
            remainderMinorUnits = 0L,
            method = SplitMethod.EQUAL,
            currency = currency,
            roundingAdjustmentNote = roundingNote,
            isExactPerPerson = isExact,
            summaryPerPersonAmount = summaryAmount
        )
    }

    fun splitByShares(
        totalMinorUnits: Long,
        people: List<BillPerson>,
        currency: CurrencyOption
    ): BillSplitResult {
        require(people.size in BillSplitValidator.MIN_PEOPLE..BillSplitValidator.MAX_PEOPLE) {
            "People count must be between ${BillSplitValidator.MIN_PEOPLE} and ${BillSplitValidator.MAX_PEOPLE}"
        }
        require(totalMinorUnits >= 0L) { "Total minor units must be non-negative" }

        val totalShares = people.sumOf { it.share }
        require(totalShares > 0L) { "Total shares must be greater than zero" }

        val count = people.size
        val floorAmounts = LongArray(count)
        val remainders = LongArray(count)
        val bigTotal = BigInteger.valueOf(totalMinorUnits)
        val bigTotalShares = BigInteger.valueOf(totalShares)

        var sumFloors = 0L
        for (i in 0 until count) {
            val person = people[i]
            require(person.share > 0L) { "Shares must be greater than zero" }
            val bigShare = BigInteger.valueOf(person.share)
            val product = bigTotal.multiply(bigShare)
            val quotient = product.divide(bigTotalShares).toLong()
            val rem = product.remainder(bigTotalShares).toLong()
            floorAmounts[i] = quotient
            remainders[i] = rem
            sumFloors += quotient
        }

        val remainingUnits = (totalMinorUnits - sumFloors).toInt()

        // Sort indices by largest remainder descending, then original index ascending
        val indices = Array(count) { it }
        indices.sortWith { a, b ->
            val cmp = remainders[b].compareTo(remainders[a])
            if (cmp != 0) cmp else a.compareTo(b)
        }

        val allocatedAmounts = floorAmounts.clone()
        for (k in 0 until remainingUnits) {
            val targetIdx = indices[k]
            allocatedAmounts[targetIdx] += 1L
        }

        var allocatedTotal = 0L
        val results = ArrayList<PersonShareResult>(count)

        for (i in 0 until count) {
            val person = people[i]
            val allocated = allocatedAmounts[i]
            allocatedTotal += allocated

            val effectivePct = if (totalMinorUnits > 0L) {
                (allocated.toDouble() * 100.0) / totalMinorUnits.toDouble()
            } else {
                (person.share.toDouble() * 100.0) / totalShares.toDouble()
            }

            val shareUnitLabel = if (person.share == 1L) "1 share" else "${person.share} shares"

            results.add(
                PersonShareResult(
                    id = person.id,
                    name = person.name.ifEmpty { "Person ${i + 1}" },
                    allocationUnits = person.share,
                    allocatedMinorUnits = allocated,
                    formattedAmount = MoneyFormatter.format(allocated, currency),
                    effectivePercentage = effectivePct,
                    shareRatioText = shareUnitLabel
                )
            )
        }

        // Core Invariant Check
        if (allocatedTotal != totalMinorUnits) {
            throw IllegalStateException("Allocation invariant failed: $allocatedTotal != $totalMinorUnits")
        }

        return BillSplitResult(
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = allocatedTotal,
            remainderMinorUnits = 0L,
            method = SplitMethod.SHARES,
            currency = currency,
            roundingAdjustmentNote = null,
            isExactPerPerson = false,
            summaryPerPersonAmount = ""
        )
    }

    fun splitByPercentages(
        totalMinorUnits: Long,
        people: List<BillPerson>,
        currency: CurrencyOption
    ): BillSplitResult {
        require(people.size in BillSplitValidator.MIN_PEOPLE..BillSplitValidator.MAX_PEOPLE) {
            "People count must be between ${BillSplitValidator.MIN_PEOPLE} and ${BillSplitValidator.MAX_PEOPLE}"
        }
        require(totalMinorUnits >= 0L) { "Total minor units must be non-negative" }

        val totalPercentageUnits = people.sumOf { it.share }
        require(totalPercentageUnits == BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS) {
            "Percentages must total 100%"
        }

        val count = people.size
        val floorAmounts = LongArray(count)
        val remainders = LongArray(count)
        val divisor = BigInteger.valueOf(BillSplitValidator.TOTAL_PERCENTAGE_BASIS_POINTS)
        val bigTotal = BigInteger.valueOf(totalMinorUnits)

        var sumFloors = 0L
        for (i in 0 until count) {
            val person = people[i]
            require(person.share >= 0L) { "Percentage cannot be negative" }
            val bigShare = BigInteger.valueOf(person.share)
            val product = bigTotal.multiply(bigShare)
            val quotient = product.divide(divisor).toLong()
            val rem = product.remainder(divisor).toLong()
            floorAmounts[i] = quotient
            remainders[i] = rem
            sumFloors += quotient
        }

        val remainingUnits = (totalMinorUnits - sumFloors).toInt()

        // Sort indices by largest remainder descending, then original index ascending
        val indices = Array(count) { it }
        indices.sortWith { a, b ->
            val cmp = remainders[b].compareTo(remainders[a])
            if (cmp != 0) cmp else a.compareTo(b)
        }

        val allocatedAmounts = floorAmounts.clone()
        for (k in 0 until remainingUnits) {
            val targetIdx = indices[k]
            allocatedAmounts[targetIdx] += 1L
        }

        var allocatedTotal = 0L
        val results = ArrayList<PersonShareResult>(count)

        for (i in 0 until count) {
            val person = people[i]
            val allocated = allocatedAmounts[i]
            allocatedTotal += allocated

            val effectivePct = if (totalMinorUnits > 0L) {
                (allocated.toDouble() * 100.0) / totalMinorUnits.toDouble()
            } else {
                person.share.toDouble() / 100.0
            }

            val percentLabel = BillSplitValidator.formatBasisPointsToPercentString(person.share)

            results.add(
                PersonShareResult(
                    id = person.id,
                    name = person.name.ifEmpty { "Person ${i + 1}" },
                    allocationUnits = person.share,
                    allocatedMinorUnits = allocated,
                    formattedAmount = MoneyFormatter.format(allocated, currency),
                    effectivePercentage = effectivePct,
                    shareRatioText = percentLabel
                )
            )
        }

        // Core Invariant Check
        if (allocatedTotal != totalMinorUnits) {
            throw IllegalStateException("Allocation invariant failed: $allocatedTotal != $totalMinorUnits")
        }

        return BillSplitResult(
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = allocatedTotal,
            remainderMinorUnits = 0L,
            method = SplitMethod.PERCENTAGES,
            currency = currency,
            roundingAdjustmentNote = null,
            isExactPerPerson = false,
            summaryPerPersonAmount = ""
        )
    }
}
