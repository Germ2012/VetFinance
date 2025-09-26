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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Entrada Clínica") },
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
                        label = { Text("Servicio/Motivo Principal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("➕ Añadir nuevo servicio...") },
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
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (ej: 15.5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temperatura (ej: 38.5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text("Síntomas") })
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Diagnóstico") })
                OutlinedTextField(value = treatmentPlan, onValueChange = { treatmentPlan = it }, label = { Text("Plan de Tratamiento") })

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showDatePicker = true }) {
                    Text(
                        text = if (selectedDateMillis != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis!!))
                        } else {
                            "Próxima Cita (Opcional)"
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
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ProductSelectionItem(
    product: Product,
    quantity: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("Precio: Gs ${product.price}", fontSize = 14.sp)
                if (product.selling_method != SellingMethod.DOSE_ONLY && !product.isService) {
                     Text("Stock: ${product.stock}", fontSize = 12.sp) // product.stock is Double
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, enabled = quantity > 0) {
                    Icon(Icons.Default.Clear, contentDescription = "Quitar")
                }
                Text(
                    text = if (quantity > 0) "%.2f".format(Locale.US, quantity) else "0", // Displays Double quantity
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        }
    }
}
