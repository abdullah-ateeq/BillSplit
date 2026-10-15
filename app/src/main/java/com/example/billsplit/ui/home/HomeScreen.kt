package com.example.billsplit.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billsplit.domain.BillSplitFormatter
import com.example.billsplit.domain.BillSplitValidator
import com.example.billsplit.domain.MoneyFormatter
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.ui.components.CurrencySelector
import com.example.billsplit.ui.components.ProportionalSplitBar
import com.example.billsplit.ui.theme.ErrorRed
import com.example.billsplit.ui.theme.SegmentColors
import com.example.billsplit.ui.theme.SuccessGreen
import com.example.billsplit.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BillSplitViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val scrollState = rememberScrollState()

    fun performHaptic() {
        if (uiState.settings.hapticFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BillSplit",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Split a bill equally or by custom shares.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    performHaptic()
                    viewModel.reset()
                    Toast.makeText(context, "Calculator reset", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Total Bill Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL BILL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )

                    CurrencySelector(
                        selectedCurrency = uiState.currency,
                        onCurrencySelected = {
                            performHaptic()
                            viewModel.onCurrencyChanged(it)
                        },
                        showCustomDialog = uiState.showCustomCurrencyDialog,
                        onShowCustomDialog = { viewModel.showCustomCurrencyDialog(it) },
                        onApplyCustomCurrency = { s, n, d ->
                            performHaptic()
                            viewModel.applyCustomCurrency(s, n, d)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.billInput,
                    onValueChange = { viewModel.onBillChanged(it) },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = uiState.currency.symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split Method Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SPLIT METHOD",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Method Selection: EQUAL vs CUSTOM
                val isCustom = uiState.method != SplitMethod.EQUAL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val equalSelected = !isCustom
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (equalSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                performHaptic()
                                viewModel.onMethodChanged(SplitMethod.EQUAL)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EQUAL",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (equalSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (equalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustom) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                performHaptic()
                                if (uiState.method == SplitMethod.EQUAL) {
                                    viewModel.onMethodChanged(SplitMethod.SHARES)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If CUSTOM, sub-toggle: SHARES vs PERCENTAGES
                if (isCustom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(3.dp)
                    ) {
                        val isShares = uiState.method == SplitMethod.SHARES
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isShares) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable {
                                    performHaptic()
                                    viewModel.onMethodChanged(SplitMethod.SHARES)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SHARES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isShares) FontWeight.Bold else FontWeight.Medium,
                                color = if (isShares) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isShares) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable {
                                    performHaptic()
                                    viewModel.onMethodChanged(SplitMethod.PERCENTAGES)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PERCENTAGES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isShares) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isShares) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // EQUAL MODE SECTION
        if (uiState.method == SplitMethod.EQUAL) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NUMBER OF PEOPLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stepper row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable(enabled = uiState.peopleCount > BillSplitValidator.MIN_PEOPLE) {
                                    performHaptic()
                                    viewModel.onPeopleCountChanged(uiState.peopleCount - 1)
                                },
                            color = if (uiState.peopleCount > BillSplitValidator.MIN_PEOPLE)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease people",
                                    tint = if (uiState.peopleCount > BillSplitValidator.MIN_PEOPLE)
                                        MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Text(
                            text = "${uiState.peopleCount}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable(enabled = uiState.peopleCount < BillSplitValidator.MAX_PEOPLE) {
                                    performHaptic()
                                    viewModel.onPeopleCountChanged(uiState.peopleCount + 1)
                                },
                            color = if (uiState.peopleCount < BillSplitValidator.MAX_PEOPLE)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase people",
                                    tint = if (uiState.peopleCount < BillSplitValidator.MAX_PEOPLE)
                                        MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick presets: 2, 3, 4, 5, 6, 8, 10
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(2, 3, 4, 5, 6, 8, 10)) { count ->
                            val isSelected = uiState.peopleCount == count
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        performHaptic()
                                        viewModel.applyPeoplePreset(count)
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional: EDIT NAMES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleEditNames() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "EDIT NAMES (OPTIONAL)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            imageVector = if (uiState.isEditNamesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Names",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (uiState.isEditNamesExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        for (i in 0 until uiState.peopleCount) {
                            val currentName = uiState.equalNames.getOrNull(i).orEmpty()
                            OutlinedTextField(
                                value = currentName,
                                onValueChange = { viewModel.onPersonNameChanged(i, it) },
                                placeholder = { Text("Person ${i + 1}") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // CUSTOM SHARES SECTION
        if (uiState.method == SplitMethod.SHARES) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PEOPLE & SHARES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )

                        val totalShares = uiState.people.sumOf { it.share }
                        Text(
                            text = "Total: $totalShares ${if (totalShares == 1L) "share" else "shares"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.people.forEachIndexed { index, person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = person.name,
                                onValueChange = { viewModel.onPersonNameChanged(index, it) },
                                placeholder = { Text("Person ${index + 1}") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = if (person.share == 0L) "" else person.share.toString(),
                                onValueChange = { viewModel.onPersonShareChanged(index, it) },
                                placeholder = { Text("1") },
                                trailingIcon = { Text("shares", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 6.dp)) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp)
                            )

                            IconButton(
                                onClick = {
                                    performHaptic()
                                    viewModel.onRemovePerson(index)
                                },
                                enabled = uiState.people.size > BillSplitValidator.MIN_PEOPLE,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Person",
                                    tint = if (uiState.people.size > BillSplitValidator.MIN_PEOPLE)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            performHaptic()
                            viewModel.onAddPerson()
                        },
                        enabled = uiState.people.size < BillSplitValidator.MAX_PEOPLE,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ ADD PERSON (${uiState.people.size}/${BillSplitValidator.MAX_PEOPLE})")
                    }
                }
            }
        }

        // CUSTOM PERCENTAGES SECTION
        if (uiState.method == SplitMethod.PERCENTAGES) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "QUICK PERCENTAGE PRESETS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            "50 / 50" to listOf(50.0, 50.0),
                            "25 / 75" to listOf(25.0, 75.0),
                            "25 / 25 / 50" to listOf(25.0, 25.0, 50.0),
                            "20 / 30 / 50" to listOf(20.0, 30.0, 50.0),
                            "25 / 25 / 25 / 25" to listOf(25.0, 25.0, 25.0, 25.0)
                        )
                        items(presets) { (label, preset) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        performHaptic()
                                        viewModel.applyPercentagePreset(preset)
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // People Percentage inputs
                    uiState.people.forEachIndexed { index, person ->
                        val inputVal = uiState.percentageInputs.getOrNull(index).orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = person.name,
                                onValueChange = { viewModel.onPersonNameChanged(index, it) },
                                placeholder = { Text("Person ${index + 1}") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = inputVal,
                                onValueChange = { viewModel.onPersonPercentageChanged(index, it) },
                                placeholder = { Text("0") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp)
                            )

                            IconButton(
                                onClick = {
                                    performHaptic()
                                    viewModel.onRemovePerson(index)
                                },
                                enabled = uiState.people.size > BillSplitValidator.MIN_PEOPLE,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Person",
                                    tint = if (uiState.people.size > BillSplitValidator.MIN_PEOPLE)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            performHaptic()
                            viewModel.onAddPerson()
                        },
                        enabled = uiState.people.size < BillSplitValidator.MAX_PEOPLE,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ ADD PERSON (${uiState.people.size}/${BillSplitValidator.MAX_PEOPLE})")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Status Banner
                    val status = uiState.percentageStatus
                    val statusColor = when (status.type) {
                        PercentageStatusType.BALANCED -> SuccessGreen
                        PercentageStatusType.REMAINING -> WarningAmber
                        PercentageStatusType.OVER -> ErrorRed
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (status.type) {
                                        PercentageStatusType.BALANCED -> Icons.Default.CheckCircle
                                        PercentageStatusType.REMAINING -> Icons.Default.Info
                                        PercentageStatusType.OVER -> Icons.Default.Warning
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TOTAL: ${String.format(java.util.Locale.US, "%.1f%%", uiState.totalPercentage)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            Text(
                                text = status.displayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }

        // Error message if any
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CALCULATE BUTTON
        val isCalculateEnabled = when (uiState.method) {
            SplitMethod.EQUAL -> uiState.billInput.isNotBlank() && uiState.peopleCount >= 2
            SplitMethod.SHARES -> uiState.billInput.isNotBlank() && uiState.people.all { it.share > 0L }
            SplitMethod.PERCENTAGES -> uiState.billInput.isNotBlank() && uiState.percentageStatus.type == PercentageStatusType.BALANCED
        }

        Button(
            onClick = {
                performHaptic()
                viewModel.calculateSplit()
            },
            enabled = isCalculateEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "CALCULATE SPLIT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // RESULT SECTION (revealed when calculated)
        AnimatedVisibility(
            visible = uiState.result != null,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
        ) {
            val result = uiState.result
            if (result != null) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    // Result Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${MoneyFormatter.format(result.totalMinorUnits, result.currency)} TOTAL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (result.method == SplitMethod.EQUAL) {
                                Text(
                                    text = result.summaryPerPersonAmount,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "${result.people.size} PEOPLE",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Rounding Adjustment Note if any
                            if (result.roundingAdjustmentNote != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = result.roundingAdjustmentNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visual Proportional Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "VISUAL SPLIT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ProportionalSplitBar(
                                people = result.people,
                                totalMinorUnits = result.totalMinorUnits,
                                animationsEnabled = uiState.settings.animations
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Individual People Cards
                    Text(
                        text = "INDIVIDUAL SHARES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    result.people.forEachIndexed { index, person ->
                        val segmentColor = SegmentColors[index % SegmentColors.size]
                        val pctFormatted = if (person.effectivePercentage % 1.0 == 0.0) {
                            "${person.effectivePercentage.toInt()}%"
                        } else {
                            String.format(java.util.Locale.US, "%.2f%%", person.effectivePercentage)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(segmentColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = person.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$pctFormatted · ${person.shareRatioText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = person.formattedAmount,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Result Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "CALCULATION SUMMARY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SummaryRow("TOTAL BILL", MoneyFormatter.format(result.totalMinorUnits, result.currency))
                            SummaryRow("PEOPLE", result.people.size.toString())
                            SummaryRow("METHOD", when (result.method) {
                                SplitMethod.EQUAL -> "Equal"
                                SplitMethod.SHARES -> "Custom Shares"
                                SplitMethod.PERCENTAGES -> "Custom Percentages"
                            })
                            SummaryRow("ALLOCATED", MoneyFormatter.format(result.totalAllocatedMinorUnits, result.currency))
                            SummaryRow("REMAINING", MoneyFormatter.format(result.remainderMinorUnits, result.currency))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: COPY, SHARE, NEW SPLIT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                performHaptic()
                                val copyText = BillSplitFormatter.formatCopyText(result)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("BillSplit Result", copyText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPY")
                        }

                        OutlinedButton(
                            onClick = {
                                performHaptic()
                                val shareText = BillSplitFormatter.formatShareText(result)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share BillSplit Result")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHARE")
                        }

                        Button(
                            onClick = {
                                performHaptic()
                                viewModel.newSplit()
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("NEW SPLIT")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
