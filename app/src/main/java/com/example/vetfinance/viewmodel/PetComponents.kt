package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentDialog(
    services: List<Product>,
    onDismiss: () -> Unit,
    // 👇 CAMBIO: La lambda onConfirm ahora incluye todos los nuevos campos
    onConfirm: (description: String, weight: Double?, temperature: Double?, symptoms: String?, diagnosis: String?, treatmentPlan: String?, nextDate: Long?) -> Unit,
    onAddNewServiceClick: () -> Unit
) {
    // --- Estados para el manejo del diálogo ---
    var expanded by remember { mutableStateOf(false) }
    var selectedServiceText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // --- Estados para los nuevos campos clínicos ---
    var weight by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatmentPlan by remember { mutableStateOf("") }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_treatment_dialog_title)) },
        text = {
            // Se añade un scroll vertical por si el contenido es muy largo
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedServiceText,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.add_treatment_service_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_treatment_add_new_service_option)) },
                            onClick = {
                                expanded = false
                                onAddNewServiceClick()
                            }
                        )
                        services.forEach { service ->
                            DropdownMenuItem(
                                text = { Text(service.name) },
                                onClick = {
                                    selectedServiceText = service.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 👇 CAMBIO: Se añaden los nuevos TextFields
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(stringResource(R.string.add_treatment_weight_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text(stringResource(R.string.add_treatment_temperature_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text(stringResource(R.string.add_treatment_symptoms_label)) })
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text(stringResource(R.string.add_treatment_diagnosis_label)) })
                OutlinedTextField(value = treatmentPlan, onValueChange = { treatmentPlan = it }, label = { Text(stringResource(R.string.add_treatment_plan_label)) })

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showDatePicker = true }) {
                    Text(
                        text = if (selectedDateMillis != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis!!))
                        } else {
                            stringResource(R.string.add_treatment_next_appointment_button_optional)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 👇 CAMBIO: Se pasan todos los nuevos datos a la lambda
                    onConfirm(
                        selectedServiceText,
                        weight.toDoubleOrNull(),
                        temperature.toDoubleOrNull(),
                        symptoms.ifBlank { null },
                        diagnosis.ifBlank { null },
                        treatmentPlan.ifBlank { null },
                        selectedDateMillis
                    )
                },
                enabled = selectedServiceText.isNotBlank()
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

