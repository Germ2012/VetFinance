// ruta: app/src/main/java/com/example/vetfinance/ui/screens/PetsScreen.kt

package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.PetWithOwner
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun PetsScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    // Se obtienen las mascotas filtradas y la consulta de búsqueda del ViewModel
    val petsWithOwners by viewModel.filteredPetsWithOwners.collectAsState()
    val searchQuery by viewModel.petSearchQuery.collectAsState()

    // Limpia la búsqueda al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPetSearchQuery()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_pet_screen") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Mascota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Mascotas Registradas",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Barra de búsqueda para filtrar por nombre de mascota o dueño
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onPetSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por mascota o dueño...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearPetSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Muestra la lista de mascotas o un mensaje si no hay resultados
            if (petsWithOwners.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ninguna mascota coincide con la búsqueda.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(petsWithOwners) { petWithOwner ->
                        PetItem(
                            petWithOwner = petWithOwner,
                            onItemClick = {
                                navController.navigate("pet_detail/${petWithOwner.pet.petId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Un elemento visual que representa una mascota y su dueño en una tarjeta.
 */
@Composable
fun PetItem(petWithOwner: PetWithOwner, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = petWithOwner.pet.name,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Dueño: ${petWithOwner.owner.name}")
        }
    }
}