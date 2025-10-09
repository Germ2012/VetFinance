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
import com.example.vetfinance.data.Treatment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentDialog(
    initialTreatment: Treatment? = null, // <- AÑADIR: Parámetro opcional
    services: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, weight: String, temperature: String, symptoms: String, diagnosis: String, treatmentPlan: String, nextDate: Long?) -> Unit,
    onAddNewServiceClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // --- INICIO CÓDIGO A MODIFICAR ---
    // Si hay un tratamiento inicial, usa sus datos, si no, usa valores vacíos.
    var selectedServiceText by remember(initialTreatment) { mutableStateOf(initialTreatment?.description ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTreatment?.nextTreatmentDate)
    var selectedDateMillis by remember(initialTreatment) { mutableStateOf(initialTreatment?.nextTreatmentDate) }

    var weight by remember(initialTreatment) { mutableStateOf(initialTreatment?.weight?.toString() ?: "") }
    var temperature by remember(initialTreatment) { mutableStateOf(initialTreatment?.temperature ?: "") }
    var symptoms by remember(initialTreatment) { mutableStateOf(initialTreatment?.symptoms ?: "") }
    var diagnosis by remember(initialTreatment) { mutableStateOf(initialTreatment?.diagnosis ?: "") }
    var treatmentPlan by remember(initialTreatment) { mutableStateOf(initialTreatment?.treatmentPlan ?: "") }
    // --- FIN CÓDIGO A MODIFICAR ---

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
