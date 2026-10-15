package com.example.billsplit.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.billsplit.model.BillPerson
import com.example.billsplit.model.BillSplitHistory
import com.example.billsplit.model.SplitMethod

class BillSplitHistoryRepository(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("billsplit_history", Context.MODE_PRIVATE)
    )

    companion object {
        private const val KEY_HISTORY = "history_entries"
        const val MAX_HISTORY_ITEMS = 10

        fun serializePerson(person: BillPerson): String {
            val safeName = person.name.replace("%", "%25").replace(",", "%2C").replace(";", "%3B").replace("|", "%7C")
            return "${person.id},$safeName,${person.share}"
        }

        fun deserializePerson(str: String): BillPerson? {
            val parts = str.split(",")
            if (parts.size < 3) return null
            val id = parts[0]
            val name = parts[1].replace("%7C", "|").replace("%3B", ";").replace("%2C", ",").replace("%25", "%")
            val share = parts[2].toLongOrNull() ?: 1L
            return BillPerson(id = id, name = name, share = share)
        }

        fun serializeEntry(entry: BillSplitHistory): String {
            val peopleStr = entry.people.joinToString(";") { serializePerson(it) }
            val safeSymbol = entry.currencySymbol.replace("%", "%25").replace("|", "%7C")
            return "${entry.id}|${entry.totalMinorUnits}|${entry.currencyCode}|$safeSymbol|${entry.method.name}|${entry.calculatedAt}|$peopleStr"
        }

        fun deserializeEntry(line: String): BillSplitHistory? {
            val parts = line.split("|")
            if (parts.size < 7) return null
            val id = parts[0]
            val total = parts[1].toLongOrNull() ?: return null
            val code = parts[2]
            val symbol = parts[3].replace("%7C", "|").replace("%25", "%")
            val method = try {
                SplitMethod.valueOf(parts[4])
            } catch (_: Exception) {
                SplitMethod.EQUAL
            }
            val time = parts[5].toLongOrNull() ?: 0L
            val people = if (parts[6].isEmpty()) emptyList() else {
                parts[6].split(";").mapNotNull { deserializePerson(it) }
            }
            return BillSplitHistory(
                id = id,
                totalMinorUnits = total,
                currencyCode = code,
                currencySymbol = symbol,
                method = method,
                people = people,
                calculatedAt = time
            )
        }
    }

    fun loadHistory(): List<BillSplitHistory> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { deserializeEntry(it) }
    }

    fun saveCalculation(entry: BillSplitHistory): List<BillSplitHistory> {
        val current = loadHistory().toMutableList()
        // Deduplicate if identical to top item
        if (current.isNotEmpty()) {
            val top = current.first()
            if (top.totalMinorUnits == entry.totalMinorUnits &&
                top.currencyCode == entry.currencyCode &&
                top.method == entry.method &&
                top.people == entry.people
            ) {
                return current
            }
        }

        // Add to front (newest first)
        current.add(0, entry)

        // Enforce maximum 10 items
        while (current.size > MAX_HISTORY_ITEMS) {
            current.removeAt(current.lastIndex)
        }

        persistList(current)
        return current
    }

    fun deleteCalculation(id: String): List<BillSplitHistory> {
        val current = loadHistory().toMutableList()
        current.removeAll { it.id == id }
        persistList(current)
        return current
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun persistList(list: List<BillSplitHistory>) {
        val raw = list.joinToString("\n") { serializeEntry(it) }
        prefs.edit().putString(KEY_HISTORY, raw).apply()
    }
}
