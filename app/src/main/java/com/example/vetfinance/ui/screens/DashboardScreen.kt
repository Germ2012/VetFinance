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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod // Import SellingMethod
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(viewModel: VetViewModel, navController: NavController) {
    val salesToday by viewModel.salesSummaryToday.collectAsState()
    val upcomingTreatments by viewModel.upcomingTreatments.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()
    var treatmentForNextDialog by remember { mutableStateOf<Treatment?>(null) }
    val petForDialog = treatmentForNextDialog?.let { treatment ->
        petsWithOwners.find { it.pet.petId == treatment.petIdFk }
    }
    val inventory by viewModel.inventory.collectAsState()
    val services = remember(inventory) { inventory.filter { it.isService } }
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    var showManagementDialog by remember { mutableStateOf(false) }
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val petIdToNameMap = petsWithOwners.associate { it.pet.petId to it.pet.name }

    // --- DIÁLOGOS ---
    if (treatmentForNextDialog != null && petForDialog != null) {
        AddTreatmentDialog(
            services = services,
            onDismiss = { treatmentForNextDialog = null },
            onConfirm = { description, weight, temperature, symptoms, diagnosis, treatmentPlan, nextDateMillis ->
                viewModel.addTreatment(
                    pet = petForDialog.pet,
                    description = description,
                    weight = weight,
                    temperature = temperature,
                    symptoms = symptoms,
                    diagnosis = diagnosis,
                    treatmentPlan = treatmentPlan,
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
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService, newProduct.selling_method) // Added selling_method
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
        )
    }

    if (showManagementDialog) {
        AlertDialog(
            onDismissRequest = { showManagementDialog = false },
            title = { Text("Seleccionar Lista") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.DebtClients.route)
                            showManagementDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Lista de Clientes")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Pets.route)
                            showManagementDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Lista de Mascotas")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManagementDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.vet_background_logo),
            contentDescription = "Fondo del dashboard",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- Static "Resumen del Día" Section ---
            Text("Resumen del Día", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            val formattedSales = "Gs. ${formatCurrency(salesToday)}"
            ReportSummaryCard("Ventas de Hoy", formattedSales)
            Spacer(modifier = Modifier.height(16.dp)) // Space after summary, before dynamic content

            // --- Dynamic Content Section (Loading or List) ---
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

                    if (upcomingTreatments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Próximos Tratamientos", style = MaterialTheme.typography.headlineSmall)
                        }
                        items(upcomingTreatments) { treatment ->
                            TreatmentReminderItem(
                                treatment = treatment,
                                petName = petIdToNameMap[treatment.petIdFk] ?: "Mascota desconocida",
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
                text = "Alerta de Stock Bajo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            lowStockProducts.forEach { product ->
                Text(
                    text = "- ${product.name} (Stock: ${formatCurrency(product.stock)})".replace(",00", ""), // Usar formatCurrency para el stock también y quitar decimales si son cero
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
        daysUntilNext == null -> MaterialTheme.colorScheme.surfaceVariant // Tratamiento sin próxima fecha (posiblemente finalizado)
        daysUntilNext < 0 -> MaterialTheme.colorScheme.errorContainer // Vencido
        daysUntilNext == 0L -> MaterialTheme.colorScheme.tertiaryContainer // Hoy
        daysUntilNext < 3 -> MaterialTheme.colorScheme.secondaryContainer // Próximamente (menos de 3 días)
        else -> MaterialTheme.colorScheme.surfaceVariant // Futuro (3 días o más)
    }
    val nextDateText = when {
        daysUntilNext == null -> "Próxima cita no definida"
        daysUntilNext < 0 -> "Vencido hace ${-daysUntilNext} día(s)"
        daysUntilNext == 0L -> "Hoy"
        else -> "En $daysUntilNext día(s)"
    }


    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Mascota: $petName", fontWeight = FontWeight.Bold)
            Text(text = "Tratamiento: ${treatment.description}")
            Text(text = "Próxima cita: $nextDateText")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onMarkAsCompleted) {
                Text("Registrar Nueva Visita")
            }
        }
    }
}
