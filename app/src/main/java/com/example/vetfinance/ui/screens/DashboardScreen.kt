package com.example.vetfinance.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
import java.util.concurrent.TimeUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.example.vetfinance.data.AppointmentWithDetails

@Composable
fun DashboardScreen(viewModel: VetViewModel, navController: NavController) {
    val salesToday by viewModel.salesSummaryToday.collectAsState()
    val upcomingTreatments by viewModel.upcomingTreatments.collectAsState()
    val upcomingAppointments by viewModel.upcomingAppointments.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()
    var treatmentForNextDialog by remember { mutableStateOf<Treatment?>(null) }
    val petForDialog = treatmentForNextDialog?.let { treatment ->
        petsWithOwners.find { it.pet.petId == treatment.petIdFk }
    }
    val inventory by viewModel.inventory.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState() // Collect suppliers
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    var showManagementDialog by remember { mutableStateOf(false) }
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val petIdToNameMap = petsWithOwners.associate { it.pet.petId to it.pet.name }

    if (treatmentForNextDialog != null && petForDialog != null) {
        AddTreatmentDialog(
            services = services,
            onDismiss = { treatmentForNextDialog = null },
            onConfirm = { description, weight, temperature, symptoms, diagnosis, treatmentPlan, nextDateMillis ->
                viewModel.addTreatment(
                    pet = petForDialog.pet,
                    description = description,
                    weight = weight.toDoubleOrNull(),
                    temperature = temperature.ifBlank { null },
                    symptoms = symptoms.ifBlank { null },
                    diagnosis = diagnosis.ifBlank { null },
                    treatmentPlan = treatmentPlan.ifBlank { null },
                    nextDate = nextDateMillis
                )
                treatmentForNextDialog = null
            },
            onAddNewServiceClick = {
                treatmentForNextDialog = null
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

    if (showManagementDialog) {
        AlertDialog(
            onDismissRequest = { showManagementDialog = false },
            title = { Text(stringResource(R.string.dashboard_select_list_dialog_title)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.DebtClients.route)
                            showManagementDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dashboard_view_client_list_button))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Pets.route)
                            showManagementDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dashboard_view_pet_list_button))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManagementDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.vet_background_logo),
            contentDescription = stringResource(R.string.dashboard_background_content_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.dashboard_summary_of_the_day), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            val formattedSales = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(salesToday)
            ReportSummaryCard(stringResource(R.string.dashboard_sales_today_title), formattedSales)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Screen.Restock.route) }) {
                Text(text = stringResource(R.string.restock_button))
            }


            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (lowStockProducts.isNotEmpty()) {
                        item {
                            LowStockAlert(lowStockProducts = lowStockProducts)
                        }
                    }
                    if (upcomingAppointments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Próximas Citas", style = MaterialTheme.typography.headlineSmall)
                        }
                        items(upcomingAppointments) { appointmentDetails ->
                            AppointmentReminderItem(details = appointmentDetails)
                        }
                    }
                    if (upcomingTreatments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.dashboard_upcoming_treatments_title), style = MaterialTheme.typography.headlineSmall)
                        }
                        items(upcomingTreatments) { treatment ->
                            TreatmentReminderItem(
                                treatment = treatment,
                                petName = petIdToNameMap[treatment.petIdFk] ?: stringResource(R.string.dashboard_unknown_pet),
                                onMarkAsCompleted = {
                                    viewModel.markTreatmentAsCompleted(treatment)
                                    treatmentForNextDialog = treatment
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentReminderItem(details: AppointmentWithDetails) {
    val appointmentDate = Instant.ofEpochMilli(details.appointment.appointmentDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, appointmentDate)

    val cardColor = when {
        daysUntil < 0 -> MaterialTheme.colorScheme.errorContainer // Vencido
        daysUntil == 0L -> MaterialTheme.colorScheme.tertiaryContainer // Hoy
        daysUntil <= 2L -> MaterialTheme.colorScheme.secondaryContainer // Próximamente
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val dateText = when {
        daysUntil < 0 -> "Vencida"
        daysUntil == 0L -> "Hoy"
        daysUntil == 1L -> "Mañana"
        else -> "En $daysUntil días (${appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM"))})"
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Cita: $dateText", fontWeight = FontWeight.Bold)
            Text(text = "Mascota: ${details.pet.name} (Dueño: ${details.client.name})")
            Text(text = "Motivo: ${details.appointment.description ?: "No especificado"}")
        }
    }
}
@Composable
fun ReportSummaryCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LowStockAlert(lowStockProducts: List<Product>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_low_stock_alert_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            lowStockProducts.forEach { product ->
                Text(
                    text = stringResource(
                        R.string.dashboard_low_stock_product_item,
                        product.name,
                        formatCurrency(product.stock).replace(",00", "")
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun TreatmentReminderItem(
    treatment: Treatment,
    petName: String,
    onMarkAsCompleted: () -> Unit
) {
    val daysUntilNext = treatment.nextTreatmentDate?.let { TimeUnit.MILLISECONDS.toDays(it - System.currentTimeMillis()) }
    val cardColor = when {
        daysUntilNext == null -> MaterialTheme.colorScheme.surfaceVariant
        daysUntilNext < 0 -> MaterialTheme.colorScheme.errorContainer
        daysUntilNext == 0L -> MaterialTheme.colorScheme.tertiaryContainer
        daysUntilNext < 3 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val nextDateText = when {
        daysUntilNext == null -> stringResource(R.string.dashboard_next_appointment_not_defined)
        daysUntilNext < 0 -> stringResource(R.string.dashboard_appointment_overdue_days, -daysUntilNext)
        daysUntilNext == 0L -> stringResource(R.string.dashboard_appointment_today)
        else -> stringResource(R.string.dashboard_appointment_in_days, daysUntilNext)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.dashboard_pet_label, petName), fontWeight = FontWeight.Bold)
            treatment.description?.let { Text(text = stringResource(R.string.dashboard_treatment_label, it)) }
            Text(text = stringResource(R.string.dashboard_next_appointment_label, nextDateText))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onMarkAsCompleted) {
                Text(stringResource(R.string.dashboard_register_new_visit_button))
            }
        }
    }
}