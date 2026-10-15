package com.example.billsplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billsplit.model.CurrencyOption
import com.example.billsplit.repository.CurrencyRepository

@Composable
fun CurrencySelector(
    selectedCurrency: CurrencyOption,
    onCurrencySelected: (CurrencyOption) -> Unit,
    showCustomDialog: Boolean,
    onShowCustomDialog: (Boolean) -> Unit,
    onApplyCustomCurrency: (symbol: String, name: String, decimals: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedCurrency.code} (${selectedCurrency.symbol})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Currency",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            CurrencyRepository.standardCurrencies.forEach { currency ->
                val isSelected = selectedCurrency.code == currency.code
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currency.code} — ${currency.symbol}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${currency.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onCurrencySelected(currency)
                    }
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Other...",
                        fontWeight = if (selectedCurrency.code == "CUSTOM") FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                onClick = {
                    expanded = false
                    onShowCustomDialog(true)
                }
            )
        }
    }

    if (showCustomDialog) {
        CustomCurrencyDialog(
            initialSymbol = if (selectedCurrency.code == "CUSTOM") selectedCurrency.symbol else "$",
            initialName = if (selectedCurrency.code == "CUSTOM") selectedCurrency.name else "",
            initialDecimals = selectedCurrency.decimalPlaces,
            onDismiss = { onShowCustomDialog(false) },
            onConfirm = { symbol, name, decimals ->
                onApplyCustomCurrency(symbol, name, decimals)
                onShowCustomDialog(false)
            }
        )
    }
}

@Composable
fun CustomCurrencyDialog(
    initialSymbol: String,
    initialName: String,
    initialDecimals: Int,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, name: String, decimals: Int) -> Unit
) {
    var symbol by remember { mutableStateOf(initialSymbol) }
    var name by remember { mutableStateOf(initialName) }
    var decimals by remember { mutableIntStateOf(initialDecimals.coerceIn(0, 4)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Custom Currency") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.take(6) },
                    label = { Text("Currency Symbol") },
                    placeholder = { Text("e.g. $, kr, CHF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Currency Name (optional)") },
                    placeholder = { Text("e.g. Krona") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Decimal Places: $decimals",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0, 2, 3, 4).forEach { dec ->
                        val isSelected = decimals == dec
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { decimals = dec },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = dec.toString(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeSymbol = symbol.trim().ifEmpty { "$" }
                    onConfirm(safeSymbol, name.trim(), decimals)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
