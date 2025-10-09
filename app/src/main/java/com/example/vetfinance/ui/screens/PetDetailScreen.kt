package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product // Ensure Product is imported if not already
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
    val suppliers by viewModel.suppliers.collectAsState()
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

    // Estados para manejar el diálogo de edición y eliminación
    var treatmentToEdit by remember { mutableStateOf<Treatment?>(null) }
    var treatmentToDelete by remember { mutableStateOf<Treatment?>(null) }

    if (showTreatmentDialog && petWithOwner != null) {
        AddTreatmentDialog(
            services = services,
            onDismiss = { showTreatmentDialog = false },
            onConfirm = { description, weight, temperature, symptoms, diagnosis, treatmentPlan, nextDateMillis ->
                viewModel.addTreatment(
                    pet = petWithOwner.pet,
                    description = description,
                    weight = weight.toDoubleOrNull(),
                    temperature = temperature.ifBlank { null },
                    symptoms = symptoms.ifBlank { null },
                    diagnosis = diagnosis.ifBlank { null },
                    treatmentPlan = treatmentPlan.ifBlank { null },
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

    // Diálogo de edición (muy similar al de añadir)
    treatmentToEdit?.let { treatment ->
        AddTreatmentDialog(
            // Pasa los datos del tratamiento para pre-rellenar el diálogo
            initialTreatment = treatment, 
            services = services,
            onDismiss = { treatmentToEdit = null },
            onConfirm = { description, weight, temperature, symptoms, diagnosis, treatmentPlan, nextDateMillis ->
                val updatedTreatment = treatment.copy(
                    description = description,
                    weight = weight.toDoubleOrNull(),
                    temperature = temperature.ifBlank { null },
                    symptoms = symptoms.ifBlank { null },
                    diagnosis = diagnosis.ifBlank { null },
                    treatmentPlan = treatmentPlan.ifBlank { null },
                    nextTreatmentDate = nextDateMillis
                )
                viewModel.updateTreatment(updatedTreatment)
                treatmentToEdit = null
            },
            onAddNewServiceClick = {
                treatmentToEdit = null
                viewModel.onShowAddProductDialog()
            }
        )
    }

    // Diálogo de confirmación para eliminar
    treatmentToDelete?.let { treatment ->
        AlertDialog(
            onDismissRequest = { treatmentToDelete = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta entrada del historial?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTreatment(treatment)
                        treatmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { treatmentToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            allProducts = inventory, 
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.insertOrUpdateProduct(newProduct)
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            suppliers = suppliers
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(petWithOwner?.pet?.name ?: stringResource(R.string.pet_detail_screen_title_default)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTreatmentDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_treatment))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.pet_detail_clinical_history_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { treatment ->
                    TreatmentHistoryItem(
                        treatment = treatment,
                        onEdit = { treatmentToEdit = treatment },
                        onDelete = { treatmentToDelete = treatment }
                    )
                }
            }
        }
    }
}

@Composable
fun TreatmentHistoryItem(
    treatment: Treatment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val nextDateString = treatment.nextTreatmentDate?.let {
        stringResource(R.string.treatment_item_next_appointment_prefix, dateFormat.format(Date(it)))
    } ?: stringResource(R.string.treatment_item_finished)
    
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Box {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.treatment_item_date_prefix, dateFormat.format(Date(treatment.treatmentDate))),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.treatment_item_description_prefix, treatment.description ?: ""))
                treatment.symptoms?.takeIf { it.isNotBlank() }?.let { Text(text = stringResource(R.string.treatment_item_symptoms_prefix, it)) }
                treatment.diagnosis?.takeIf { it.isNotBlank() }?.let { Text(text = stringResource(R.string.treatment_item_diagnosis_prefix, it)) }
                treatment.treatmentPlan?.takeIf { it.isNotBlank() }?.let { Text(text = stringResource(R.string.treatment_item_plan_prefix, it)) }
                treatment.weight?.let { Text(text = stringResource(R.string.treatment_item_weight_prefix, it.toString())) }
                treatment.temperature?.let { Text(text = stringResource(R.string.treatment_item_temperature_prefix, it)) }
                Text(text = nextDateString, style = MaterialTheme.typography.bodySmall)
            }
            // Menú de opciones
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { onEdit(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { onDelete(); menuExpanded = false })
                }
            }
        }
    }
}
