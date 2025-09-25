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
    // Estados para los campos del formulario
    var petName by remember { mutableStateOf("") }
    var ownerMenuExpanded by remember { mutableStateOf(false) }
    var selectedOwner by remember { mutableStateOf<Client?>(null) }
    var showAddOwnerDialog by remember { mutableStateOf(false) }

    // Observa la lista de clientes del ViewModel
    val clients by viewModel.clients.collectAsState()

    // --- DIÁLOGO PARA AÑADIR DUEÑO ---
    if (showAddOwnerDialog) {
        // Ahora se usa la versión centralizada de AddOwnerDialog
        AddOwnerDialog(
            onDismiss = { showAddOwnerDialog = false },
            onConfirm = { name, phone ->
                viewModel.addClient(name, phone, 0.0) // Deuda inicial 0
                showAddOwnerDialog = false
            }
        )
    }

    // --- UI DE LA PANTALLA ---
    Scaffold(
        topBar = { TopAppBar(title = { Text("Añadir Nueva Mascota") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Campo para el nombre de la mascota
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Nombre de la mascota") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Selector de Dueño con Botón de Añadir ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Menú desplegable para seleccionar un dueño
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

                // Botón para abrir el diálogo de nuevo dueño
                IconButton(onClick = { showAddOwnerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Nuevo Dueño")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Botón para guardar la mascota
            Button(
                onClick = {
                    selectedOwner?.let {
                        val newPet = Pet(name = petName, ownerIdFk = it.clientId)
                        viewModel.addPet(newPet)
                        navController.popBackStack() // Regresa a la pantalla anterior
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

// La función AddOwnerDialog ha sido eliminada de aquí y movida a ClientComponents.kt