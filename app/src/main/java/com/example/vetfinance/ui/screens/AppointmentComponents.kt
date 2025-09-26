package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    var clientMenuExpanded by remember { mutableStateOf(false) }
    var petMenuExpanded by remember { mutableStateOf(false) }

    // Filtra la lista de mascotas basándose en el cliente seleccionado
    val petsOfSelectedClient = remember(selectedClient) {
        petsWithOwners.filter { it.owner.clientId == selectedClient?.clientId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar Nueva Cita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Menú desplegable para Clientes
                ExposedDropdownMenuBox(
                    expanded = clientMenuExpanded,
                    onExpandedChange = { clientMenuExpanded = !clientMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: "Seleccionar Cliente",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
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
                        value = selectedPet?.name ?: "Seleccionar Mascota",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mascota") },
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
                                text = { Text(petWithOwner.pet.name) },
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
                    label = { Text("Descripción de la cita") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalClient = selectedClient
                    val finalPet = selectedPet
                    if (finalClient != null && finalPet != null && description.isNotBlank()) {
                        val appointment = Appointment(
                            clientIdFk = finalClient.clientId,
                            petIdFk = finalPet.petId,
                            appointmentDate = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            description = description
                        )
                        onConfirm(appointment)
                    }
                },
                enabled = selectedClient != null && selectedPet != null && description.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}