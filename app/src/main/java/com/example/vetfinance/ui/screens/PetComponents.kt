package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentDialog(
    services: List<Product>,
    onDismiss: () -> Unit,
    // INTEGRADO: La firma ahora espera Strings para peso y temperatura.
    onConfirm: (description: String, weight: String, temperature: String, symptoms: String, diagnosis: String, treatmentPlan: String, nextDate: Long?) -> Unit,
    onAddNewServiceClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedServiceText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

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
                }) { Text(stringResource(id = R.string.accept_button)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(id = R.string.cancel_button)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_treatment_dialog_title)) },
        text = {
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
                        label = { Text(stringResource(id = R.string.add_treatment_service_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.add_treatment_add_new_service_option)) },
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

                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(stringResource(id = R.string.add_treatment_weight_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                // INTEGRADO: El tipo de teclado para temperatura ahora es de texto.
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text(stringResource(id = R.string.add_treatment_temperature_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text(stringResource(id = R.string.add_treatment_symptoms_label)) })
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text(stringResource(id = R.string.add_treatment_diagnosis_label)) })
                OutlinedTextField(value = treatmentPlan, onValueChange = { treatmentPlan = it }, label = { Text(stringResource(id = R.string.add_treatment_plan_label)) })

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showDatePicker = true }) {
                    Text(
                        text = if (selectedDateMillis != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis!!))
                        } else {
                            stringResource(id = R.string.add_treatment_next_appointment_button_optional)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // INTEGRADO: Se pasan los valores como String directamente.
                    onConfirm(
                        selectedServiceText,
                        weight,
                        temperature,
                        symptoms,
                        diagnosis,
                        treatmentPlan,
                        selectedDateMillis
                    )
                },
                enabled = selectedServiceText.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel_button)) }
        }
    )
}