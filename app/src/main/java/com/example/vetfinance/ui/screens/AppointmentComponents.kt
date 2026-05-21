package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import com.example.vetfinance.data.Appointment
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Pet
import com.example.vetfinance.data.PetWithOwner
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    clients: List<Client>,
    petsWithOwners: List<PetWithOwner>,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var description by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("09") }
    var minute by remember { mutableStateOf("00") }

    var clientMenuExpanded by remember { mutableStateOf(false) }
    var petMenuExpanded by remember { mutableStateOf(false) }

    // Filtra la lista de mascotas basándose en el cliente seleccionado
    val petsOfSelectedClient = remember(selectedClient) {
        petsWithOwners.filter { it.owner.clientId == selectedClient?.clientId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_appointment_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Menú desplegable para Clientes
                ExposedDropdownMenuBox(
                    expanded = clientMenuExpanded,
                    onExpandedChange = { clientMenuExpanded = !clientMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: stringResource(R.string.add_appointment_select_client_placeholder),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.add_appointment_client_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = clientMenuExpanded,
                        onDismissRequest = { clientMenuExpanded = false }
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.name) },
                                onClick = {
                                    selectedClient = client
                                    selectedPet = null // Resetea la mascota al cambiar de dueño
                                    clientMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Menú desplegable para Mascotas (habilitado solo si se ha seleccionado un cliente)
                ExposedDropdownMenuBox(
                    expanded = petMenuExpanded,
                    onExpandedChange = { if (selectedClient != null) petMenuExpanded = !petMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPet?.name ?: stringResource(R.string.add_appointment_select_pet_placeholder),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.add_appointment_pet_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = petMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = selectedClient != null
                    )
                    ExposedDropdownMenu(
                        expanded = petMenuExpanded,
                        onDismissRequest = { petMenuExpanded = false }
                    ) {
                        petsOfSelectedClient.forEach { petWithOwner ->
                            DropdownMenuItem(
                                text = { Text("${petWithOwner.pet.name} - ${petWithOwner.owner.name}") },
                                onClick = {
                                    selectedPet = petWithOwner.pet
                                    petMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campo de texto para la descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.add_appointment_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { char -> char.isDigit() }.take(2) },
                        label = { Text("Hora") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter { char -> char.isDigit() }.take(2) },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalClient = selectedClient
                    val finalPet = selectedPet
                    if (finalClient != null && finalPet != null && description.isNotBlank()) {
                        val safeHour = (hour.toIntOrNull() ?: 9).coerceIn(0, 23)
                        val safeMinute = (minute.toIntOrNull() ?: 0).coerceIn(0, 59)
                        val appointment = Appointment(
                            clientIdFk = finalClient.clientId,
                            petIdFk = finalPet.petId,
                            appointmentDate = selectedDate.atTime(safeHour, safeMinute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            description = description
                        )
                        onConfirm(appointment)
                    }
                },
                enabled = selectedClient != null && selectedPet != null && description.isNotBlank()
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
