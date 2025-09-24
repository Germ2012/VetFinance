// ruta: app/src/main/java/com/example/vetfinance/ui/screens/PetComponents.kt

package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentDialog(
    // 👇 CAMBIO: Se añaden nuevos parámetros
    services: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, nextDate: Long?) -> Unit,
    onAddNewServiceClick: () -> Unit
) {
    // --- Estados para el manejo del diálogo ---
    var expanded by remember { mutableStateOf(false) }
    var selectedServiceText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // --- Diálogo del selector de fecha (sin cambios) ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Diálogo principal para añadir el tratamiento ---
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Tratamiento") },
        text = {
            Column {
                // 👇 CAMBIO: Se reemplaza el OutlinedTextField por un Menú Desplegable
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedServiceText,
                        onValueChange = {},
                        label = { Text("Seleccionar Servicio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Opción para añadir un nuevo servicio
                        DropdownMenuItem(
                            text = { Text("➕ Añadir nuevo servicio...") },
                            onClick = {
                                expanded = false
                                onAddNewServiceClick()
                            }
                        )
                        // Lista de servicios existentes
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

                // Botón para abrir el selector de fecha (sin cambios)
                Button(onClick = { showDatePicker = true }) {
                    Text(
                        text = if (selectedDateMillis != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis!!))
                        } else {
                            "Seleccionar Próxima Fecha (Opcional)"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedServiceText, selectedDateMillis) },
                // Solo se activa si se ha seleccionado un servicio
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