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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(viewModel: VetViewModel, petId: String, navController: NavController) {
    LaunchedEffect(key1 = petId) {
        viewModel.loadTreatmentsForPet(petId)
    }

    val pets by viewModel.petsWithOwners.collectAsState()
    val petWithOwner = pets.find { it.pet.petId == petId }
    val history by viewModel.treatmentHistory.collectAsState()
    var showTreatmentDialog by remember { mutableStateOf(false) }
    val inventory by viewModel.inventory.collectAsState()
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()

    if (showTreatmentDialog && petWithOwner != null) {
        AddTreatmentDialog(
            services = services,
            onDismiss = { showTreatmentDialog = false },
            onConfirm = { description, weight, temperature, symptoms, diagnosis, treatmentPlan, nextDateMillis ->
                viewModel.addTreatment(
                    pet = petWithOwner.pet,
                    description = description,
                    weight = weight,
                    temperature = temperature,
                    symptoms = symptoms,
                    diagnosis = diagnosis,
                    treatmentPlan = treatmentPlan,
                    nextDate = nextDateMillis
                )
                showTreatmentDialog = false
            },
            onAddNewServiceClick = {
                showTreatmentDialog = false
                viewModel.onShowAddProductDialog()
            }
        )
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService)
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
            Text("Historial Clínico", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { treatment ->
                    TreatmentHistoryItem(treatment)
                }
            }
        }
    }
}

@Composable
fun TreatmentHistoryItem(treatment: Treatment) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val nextDateString = treatment.nextTreatmentDate?.let {
        "Próxima cita: ${dateFormat.format(Date(it))}"
    } ?: "Tratamiento finalizado"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Fecha: ${dateFormat.format(Date(treatment.treatmentDate))}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Descripción: ${treatment.description}")
            treatment.symptoms?.takeIf { it.isNotBlank() }?.let { Text(text = "Síntomas: $it") }
            treatment.diagnosis?.takeIf { it.isNotBlank() }?.let { Text(text = "Diagnóstico: $it") }
            treatment.treatmentPlan?.takeIf { it.isNotBlank() }?.let { Text(text = "Plan: $it") }
            treatment.weight?.let { Text(text = "Peso: $it kg") }
            treatment.temperature?.let { Text(text = "Temp: $it°C") }
            Text(text = nextDateString, style = MaterialTheme.typography.bodySmall)
        }
    }
}