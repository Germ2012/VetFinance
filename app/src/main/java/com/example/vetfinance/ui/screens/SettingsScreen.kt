package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.AppSettings
import com.example.vetfinance.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var clinicName by remember(settings) { mutableStateOf(settings.clinicName) }
    var currency by remember(settings) { mutableStateOf(settings.currency) }
    var treatmentDays by remember(settings) { mutableStateOf(settings.treatmentAlertDays.toString()) }
    var supplierDebtDays by remember(settings) { mutableStateOf(settings.supplierDebtAlertDays.toString()) }
    var remindersEnabled by remember(settings) { mutableStateOf(settings.remindersEnabled) }
    var largeText by remember(settings) { mutableStateOf(settings.largeText) }
    var backupFrequencyDays by remember(settings) { mutableStateOf(settings.backupFrequencyDays.toString()) }
    var securityPin by remember(settings) { mutableStateOf(settings.securityPin) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = clinicName,
                onValueChange = { clinicName = it },
                label = { Text("Nombre de la clinica") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text("Moneda") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = treatmentDays,
                onValueChange = { treatmentDays = it.filter { char -> char.isDigit() } },
                label = { Text("Dias de alerta para tratamientos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = supplierDebtDays,
                onValueChange = { supplierDebtDays = it.filter { char -> char.isDigit() } },
                label = { Text("Dias de alerta para deuda de proveedor") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            SettingCheckboxRow(
                checked = remindersEnabled,
                label = "Recordatorios activos",
                onCheckedChange = { remindersEnabled = it }
            )
            SettingCheckboxRow(
                checked = largeText,
                label = "Texto grande",
                onCheckedChange = { largeText = it }
            )
            OutlinedTextField(
                value = backupFrequencyDays,
                onValueChange = { backupFrequencyDays = it.filter { char -> char.isDigit() } },
                label = { Text("Frecuencia sugerida de backup (dias)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = securityPin,
                onValueChange = { securityPin = it.filter { char -> char.isDigit() }.take(6) },
                label = { Text("PIN para acciones sensibles") },
                supportingText = { Text("Dejalo vacio si no queres bloquear eliminaciones y ajustes manuales.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BackupStatusCard(settings)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.updateAppSettings(
                        AppSettings(
                            clinicName = clinicName.trim(),
                            currency = currency.ifBlank { "Gs." },
                            treatmentAlertDays = treatmentDays.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                            supplierDebtAlertDays = supplierDebtDays.toIntOrNull()?.coerceAtLeast(1) ?: 7,
                            remindersEnabled = remindersEnabled,
                            largeText = largeText,
                            backupFrequencyDays = backupFrequencyDays.toIntOrNull()?.coerceAtLeast(1) ?: 7,
                            lastBackupAt = settings.lastBackupAt,
                            securityPin = securityPin
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar ajustes")
            }
        }
    }
}

@Composable
private fun SettingCheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun BackupStatusCard(settings: AppSettings) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val lastBackupText = settings.lastBackupAt?.let { sdf.format(Date(it)) } ?: "Sin respaldo registrado"
    val daysSinceBackup = settings.lastBackupAt?.let {
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now()
        )
    }
    val statusText = when {
        settings.lastBackupAt == null -> "Conviene crear un respaldo."
        daysSinceBackup != null && daysSinceBackup >= settings.backupFrequencyDays -> "Ya toca crear un nuevo respaldo."
        else -> "Respaldo al dia."
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Backup local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Ultimo respaldo: $lastBackupText")
            Text(statusText)
        }
    }
}
