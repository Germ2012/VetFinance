// ruta: app/src/main/java/com/example/vetfinance/ui/screens/DashboardScreen.kt

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
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.Period
import com.example.vetfinance.viewmodel.VetViewModel
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(viewModel: VetViewModel, navController: NavController) {
    // --- Recopila los estados desde el ViewModel (sin cambios) ---
    val salesToday = viewModel.getSalesSummary(Period.DAY)
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

    // --- Diálogo para AÑADIR el siguiente tratamiento ---
    if (treatmentForNextDialog != null && petForDialog != null) {
        // 👇 CORRECCIÓN: Se completa esta llamada con los parámetros que faltaban
        AddTreatmentDialog(
            services = services,
            onDismiss = { treatmentForNextDialog = null },
            onConfirm = { description, nextDateMillis ->
                viewModel.addTreatment(petForDialog.pet, description, nextDateMillis)
                treatmentForNextDialog = null
            },
            onAddNewServiceClick = {
                treatmentForNextDialog = null
                viewModel.onShowAddProductDialog()
            }
        )
    }

    // --- Diálogo para añadir nuevo servicio (sin cambios) ---
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { name, price, stock, isService ->
                viewModel.addProduct(name, price, stock, isService)
            }
        )
    }

    // --- Diálogo de gestión (sin cambios) ---
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

    val petIdToNameMap = petsWithOwners.associate { it.pet.petId to it.pet.name }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.vet_background_logo),
            contentDescription = "Fondo del dashboard",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Resumen del Día", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                val formattedSales = String.format("₲ %,.0f", salesToday).replace(",", ".")
                ReportSummaryCard("Ventas de Hoy", formattedSales)
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
                            viewModel.markTreatmentAsCompleted(it)
                            treatmentForNextDialog = it
                        }
                    )
                }
            }
        }
    }
}


// El resto de los composables de esta pantalla no necesitan cambios.
@Composable
fun ReportSummaryCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
fun TreatmentReminderItem(
    treatment: Treatment,
    petName: String,
    onMarkAsCompleted: (Treatment) -> Unit
) {
    val daysText = treatment.nextTreatmentDate?.let { nextDate ->
        val remainingMillis = nextDate - System.currentTimeMillis()
        val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMillis)

        when {
            remainingDays < 0 -> "Fecha pasada"
            remainingDays == 0L -> "Hoy"
            remainingDays == 1L -> "Mañana"
            else -> "En $remainingDays días"
        }
    } ?: "Sin fecha programada"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(petName, fontWeight = FontWeight.Bold)
                Text(treatment.description)
                if (treatment.nextTreatmentDate != null) {
                    Text(
                        text = daysText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onMarkAsCompleted(treatment) }
                )
                Text("Recibido", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}