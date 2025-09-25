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

    val pets by viewModel.petsWithOwners.collectAsState()
    val petWithOwner = pets.find { it.pet.petId == petId }
    val history by viewModel.treatmentHistory.collectAsState()
    var showTreatmentDialog by remember { mutableStateOf(false) }
    val inventory by viewModel.inventory.collectAsState()
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()

    if (showTreatmentDialog && petWithOwner != null) {
        // 👇 CORRECCIÓN: Se actualiza la llamada al diálogo para pasar los nuevos datos
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
        AddProductDialog(
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { name, price, stock, isService ->
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
            Text("Historial Clínico", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { treatment ->
                    // 👇 CORRECCIÓN: Se usa el nuevo Composable para el historial
                    TreatmentHistoryItem(treatment)
                }
            }
        }
    }
}

/**
 * Muestra una tarjeta con la información detallada de una entrada clínica (tratamiento).
 */
@Composable
fun TreatmentHistoryItem(treatment: Treatment) {
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.Builder().setLanguage("es").setRegion("PY").build())
    val treatmentDate = Date(treatment.treatmentDate)

    val daysText = treatment.nextTreatmentDate?.let {
        if (!treatment.isNextTreatmentCompleted) {
            val remainingMillis = it - System.currentTimeMillis()
            val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMillis)
            when {
                remainingDays < 0 -> "Próximo: Vencido"
                remainingDays == 0L -> "Próximo: Hoy"
                remainingDays == 1L -> "Próximo: Mañana"
                else -> "Próximo: En $remainingDays días"
            }
        } else null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = sdf.format(treatmentDate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = treatment.description,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Muestra los detalles clínicos solo si existen
            if (treatment.symptoms?.isNotBlank() == true) {
                ClinicalDetailItem("Síntomas", treatment.symptoms!!)
            }
            if (treatment.diagnosis?.isNotBlank() == true) {
                ClinicalDetailItem("Diagnóstico", treatment.diagnosis!!)
            }
            if (treatment.treatmentPlan?.isNotBlank() == true) {
                ClinicalDetailItem("Plan de Tratamiento", treatment.treatmentPlan!!)
            }

            // Muestra peso y temperatura si existen
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (treatment.weight != null) {
                    ClinicalDetailItem("Peso", "${treatment.weight} kg")
                }
                if (treatment.temperature != null) {
                    ClinicalDetailItem("Temperatura", "${treatment.temperature}°C")
                }
            }

            // Muestra la fecha del próximo tratamiento si existe
            daysText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Pequeño Composable para mostrar un par de título y valor para los detalles clínicos.
 */
@Composable
private fun ClinicalDetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}