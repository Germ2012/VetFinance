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
    val suppliers by viewModel.suppliers.collectAsState() // Collect suppliers
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

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

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            allProducts = inventory, 
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.insertOrUpdateProduct(newProduct) // Pass the whole product object
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            suppliers = suppliers // Pass suppliers
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
        stringResource(R.string.treatment_item_next_appointment_prefix, dateFormat.format(Date(it)))
    } ?: stringResource(R.string.treatment_item_finished)

    Card(modifier = Modifier.fillMaxWidth()) {
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
    }
}