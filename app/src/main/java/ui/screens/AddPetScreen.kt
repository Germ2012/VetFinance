package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Pet
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    var petName by remember { mutableStateOf("") }
    var ownerMenuExpanded by remember { mutableStateOf(false) }
    var selectedOwner by remember { mutableStateOf<Client?>(null) }
    var showAddOwnerDialog by remember { mutableStateOf(false) }

    val clients by viewModel.clients.collectAsState()

    // --- DIÁLOGO PARA AÑADIR DUEÑO (CLIENTE) ---
    if (showAddOwnerDialog) {
        AddOrEditClientDialog(
            onDismiss = { showAddOwnerDialog = false },
            onConfirm = { name, phone, _ -> // La deuda se ignora aquí
                viewModel.addClient(name, phone, 0.0)
                showAddOwnerDialog = false
            },
            showDebtField = false // No se muestra el campo de deuda al añadir un dueño desde aquí
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Añadir Nueva Mascota") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Nombre de la mascota") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = ownerMenuExpanded,
                    onExpandedChange = { ownerMenuExpanded = !ownerMenuExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedOwner?.name ?: "Seleccionar Dueño",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerMenuExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = ownerMenuExpanded,
                        onDismissRequest = { ownerMenuExpanded = false }
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.name) },
                                onClick = {
                                    selectedOwner = client
                                    ownerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { showAddOwnerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Nuevo Dueño")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedOwner?.let {
                        val newPet = Pet(name = petName, ownerIdFk = it.clientId)
                        viewModel.addPet(newPet)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = petName.isNotBlank() && selectedOwner != null
            ) {
                Text("Guardar Mascota")
            }
        }
    }
}
