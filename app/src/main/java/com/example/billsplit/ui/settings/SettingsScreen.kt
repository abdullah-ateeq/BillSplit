package com.example.billsplit.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ColorLens
import java.util.Locale
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billsplit.model.SplitMethod
import com.example.billsplit.repository.AppSettings
import com.example.billsplit.repository.AppTheme
import com.example.billsplit.repository.CurrencyRepository
import com.example.billsplit.ui.home.BillSplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BillSplitViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    var themeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var methodMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader(title = "APPEARANCE")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Theme Setting
                    SettingsSelectorRow(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        subtitle = when (settings.theme) {
                            AppTheme.SYSTEM -> "System default"
                            AppTheme.LIGHT -> "Light"
                            AppTheme.DARK -> "Dark"
                        },
                        onClick = { themeMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            AppTheme.values().forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(theme = theme))
                                        themeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Dynamic Color Setting
                    SettingsSwitchRow(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Color",
                        subtitle = "Use colors inspired by your wallpaper",
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(dynamicColor = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Animations Setting
                    SettingsSwitchRow(
                        icon = Icons.Default.Animation,
                        title = "Animations",
                        subtitle = "Visual transitions and bar animations",
                        checked = settings.animations,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(animations = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Haptic Feedback Setting
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on button and preset taps",
                        checked = settings.hapticFeedback,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(hapticFeedback = it)) }
                    )
                }
            }

            // Defaults Section
            SettingsSectionHeader(title = "DEFAULTS & PREFERENCES")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Default Currency
                    SettingsSelectorRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Default Currency",
                        subtitle = "${settings.defaultCurrencyCode} (${CurrencyRepository.findByCode(settings.defaultCurrencyCode)?.symbol ?: "$"})",
                        onClick = { currencyMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            CurrencyRepository.standardCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.code} — ${curr.symbol} (${curr.name})") },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(defaultCurrencyCode = curr.code))
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Default Split Method
                    SettingsSelectorRow(
                        icon = Icons.AutoMirrored.Filled.CallSplit,
                        title = "Default Split Method",
                        subtitle = when (settings.defaultSplitMethod) {
                            SplitMethod.EQUAL -> "Equal split"
                            SplitMethod.SHARES -> "Custom shares"
                            SplitMethod.PERCENTAGES -> "Custom percentages"
                        },
                        onClick = { methodMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = methodMenuExpanded,
                            onDismissRequest = { methodMenuExpanded = false }
                        ) {
                            SplitMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(defaultSplitMethod = method))
                                        methodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Preserve Last Split
                    SettingsSwitchRow(
                        icon = Icons.Default.Save,
                        title = "Preserve Last Split",
                        subtitle = "Restore previous bill and people on launch",
                        checked = settings.preserveLastSplit,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(preserveLastSplit = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Confirm History Delete
                    SettingsSwitchRow(
                        icon = Icons.Default.Security,
                        title = "Confirm History Delete",
                        subtitle = "Show confirmation dialog before deleting",
                        checked = settings.confirmHistoryDelete,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(confirmHistoryDelete = it)) }
                    )
                }
            }

            // Data & Actions Section
            SettingsSectionHeader(title = "DATA & RESET")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear History",
                        subtitle = "Remove all saved split calculations",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearHistoryDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsActionRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Reset Settings",
                        subtitle = "Restore all preferences to default values",
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showResetSettingsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog: Clear History
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History") },
            text = { Text("Clear all recent bill splits?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Reset Settings
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Restore all application settings to their default values?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSettingsToDefault()
                        showResetSettingsDialog = false
                        Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSelectorRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dropdownContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        dropdownContent()
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
