// ruta: app/src/main/java/com/example/vetfinance/ui/screens/PetDetailScreen.kt

package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.viewmodel.VetViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(viewModel: VetViewModel, petId: String, navController: NavController) {
    LaunchedEffect(key1 = petId) {
        viewModel.loadTreatmentsForPet(petId)
    }

    // --- Observa los datos del ViewModel ---
    val pets by viewModel.petsWithOwners.collectAsState()
    val petWithOwner = pets.find { it.pet.petId == petId }
    val history by viewModel.treatmentHistory.collectAsState()
    var showTreatmentDialog by remember { mutableStateOf(false) }

    // 👇 CAMBIO: Se obtiene la lista de inventario y se filtra para obtener solo los servicios
    val inventory by viewModel.inventory.collectAsState()
    val services = remember(inventory) { inventory.filter { it.isService } }

    // 👇 CAMBIO: Se observa el estado del diálogo para añadir productos/servicios del ViewModel
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()

    // --- Diálogo para SELECCIONAR un tratamiento ---
    if (showTreatmentDialog && petWithOwner != null) {
        AddTreatmentDialog(
            services = services,
            onDismiss = { showTreatmentDialog = false },
            onConfirm = { description, nextDateMillis ->
                viewModel.addTreatment(petWithOwner.pet, description, nextDateMillis)
                showTreatmentDialog = false
            },
            onAddNewServiceClick = {
                // Cierra el diálogo actual y le pide al ViewModel que abra el otro
                showTreatmentDialog = false
                viewModel.onShowAddProductDialog()
            }
        )
    }

    // --- Diálogo para AÑADIR un nuevo servicio al inventario ---
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { name, price, stock, isService ->
                // El ViewModel se encarga de añadir el producto y cerrar el diálogo
                viewModel.addProduct(name, price, stock, isService)
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(petWithOwner?.pet?.name ?: "Detalle de Mascota") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTreatmentDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Tratamiento")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Historial de Tratamientos", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { treatment ->
                    TreatmentHistoryItem(treatment)
                }
            }
        }
    }
}

// Composable para mostrar un item del historial (sin cambios)
@Composable
fun TreatmentHistoryItem(treatment: Treatment) {
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.Builder().setLanguage("es").setRegion("PY").build())
    val treatmentDate = Date(treatment.treatmentDate)

    val daysText = treatment.nextTreatmentDate?.let { nextDate ->
        if (!treatment.isNextTreatmentCompleted) {
            val remainingMillis = nextDate - System.currentTimeMillis()
            val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMillis)
            when {
                remainingDays < 0 -> "Próximo: Vencido"
                remainingDays == 0L -> "Próximo: Hoy"
                remainingDays == 1L -> "Próximo: Mañana"
                else -> "Próximo: En $remainingDays días"
            }
        } else {
            null
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sdf.format(treatmentDate),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = treatment.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            daysText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}