package com.example.vetfinance.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SupplierDebtWithSupplier
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.DebtCollectionRow
import com.example.vetfinance.viewmodel.GlobalSearchResult
import com.example.vetfinance.viewmodel.VetViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import ui.utils.formatCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DashboardCardShape = RoundedCornerShape(8.dp)

@Composable
fun DashboardScreen(viewModel: VetViewModel, navController: NavController) {
    val salesToday by viewModel.salesSummaryToday.collectAsState()
    val upcomingTreatments by viewModel.upcomingTreatments.collectAsState()
    val upcomingAppointments by viewModel.upcomingAppointments.collectAsState()
    val upcomingSupplierDebts by viewModel.upcomingSupplierDebts.collectAsState()
    val petsWithOwners by viewModel.petsWithOwners.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val pendingCollectionRows by viewModel.pendingCollectionRows.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()
    val globalSearchQuery by viewModel.globalSearchQuery.collectAsState()
    val globalSearchResults by viewModel.globalSearchResults.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var treatmentForNextDialog by remember { mutableStateOf<Treatment?>(null) }
    val sortedLowStockProducts = remember(lowStockProducts) {
        lowStockProducts.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    val pngExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        uri?.let {
            scope.launch {
                val exported = exportLowStockPng(context, it, sortedLowStockProducts)
                Toast.makeText(context, if (exported) "Lista PNG guardada." else "No se pudo guardar el PNG.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            scope.launch {
                val exported = exportLowStockPdf(context, it, sortedLowStockProducts)
                Toast.makeText(context, if (exported) "Lista PDF guardada." else "No se pudo guardar el PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val services = remember(inventory) { inventory.filter { it.isService } }
    val petIdToNameMap = remember(petsWithOwners) { petsWithOwners.associate { it.pet.petId to it.pet.name } }
    val petForDialog = remember(treatmentForNextDialog, petsWithOwners) {
        treatmentForNextDialog?.let { treatment ->
            petsWithOwners.find { it.pet.petId == treatment.petIdFk }
        }
    }
    val pendingItemsCount = lowStockProducts.size +
        upcomingAppointments.size +
        upcomingSupplierDebts.size +
        upcomingTreatments.size +
        pendingCollectionRows.size
    val defaultClinicName = stringResource(R.string.dashboard_summary_of_the_day)
    val clinicName = appSettings.clinicName.ifBlank { defaultClinicName }

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
            onConfirm = { newProduct -> viewModel.insertOrUpdateProduct(newProduct) },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            suppliers = suppliers
        )
    }

    val currentClientForPayment = clientForPayment
    if (showPaymentDialog && currentClientForPayment != null) {
        PaymentDialog(
            client = currentClientForPayment,
            onDismiss = { viewModel.onDismissPaymentDialog() },
            onConfirm = { amount -> viewModel.makePayment(amount) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.vet_background_logo),
            contentDescription = stringResource(R.string.dashboard_background_content_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.10f
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DashboardHeader(
                    clinicName = clinicName,
                    pendingItemsCount = pendingItemsCount,
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            item {
                DashboardSearch(
                    query = globalSearchQuery,
                    onQueryChange = { viewModel.onGlobalSearchQueryChange(it) }
                )
            }

            if (globalSearchResults.isNotEmpty()) {
                item {
                    DashboardSearchResults(
                        results = globalSearchResults,
                        onResultClick = { result ->
                            when (result.type) {
                                "client" -> navController.navigate("client_detail/${result.id}")
                                "pet" -> navController.navigate("pet_detail/${result.id}")
                                "product" -> navController.navigate(Screen.Inventory.route)
                            }
                            viewModel.clearGlobalSearchQuery()
                        }
                    )
                }
            }

            item {
                DashboardMetricsRow(
                    salesToday = salesToday,
                    pendingItemsCount = pendingItemsCount
                )
            }

            item {
                DashboardQuickActions(
                    onAddSaleClick = { navController.navigate(Screen.AddSale.route) },
                    onInventoryClick = { navController.navigate(Screen.Inventory.route) },
                    onRestockClick = { navController.navigate(Screen.Restock.route) }
                )
            }

            if (pendingCollectionRows.isNotEmpty()) {
                item {
                    DashboardCollectionPreview(
                        rows = pendingCollectionRows.sortedByDescending { it.balance }.take(3),
                        totalPending = pendingCollectionRows.sumOf { it.balance },
                        onCollect = { row -> viewModel.onShowPaymentDialog(row.client) },
                        onViewAll = { navController.navigate(Screen.DebtClients.route) }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (pendingItemsCount == 0) {
                    item { DashboardEmptyState() }
                }

                if (lowStockProducts.isNotEmpty()) {
                    item {
                        LowStockAlert(
                            lowStockProducts = sortedLowStockProducts,
                            onExportPng = {
                                pngExportLauncher.launch("stock_bajo_${LocalDate.now()}.png")
                            },
                            onExportPdf = {
                                pdfExportLauncher.launch("stock_bajo_${LocalDate.now()}.pdf")
                            }
                        )
                    }
                }

                if (upcomingAppointments.isNotEmpty()) {
                    item {
                        DashboardSectionHeader(
                            title = "Pr\u00f3ximas citas",
                            count = upcomingAppointments.size,
                            icon = Icons.Default.CalendarMonth
                        )
                    }
                    items(upcomingAppointments) { appointmentDetails ->
                        AppointmentReminderItem(details = appointmentDetails)
                    }
                }

                if (upcomingSupplierDebts.isNotEmpty()) {
                    item {
                        DashboardSectionHeader(
                            title = stringResource(R.string.dashboard_supplier_debts_title),
                            count = upcomingSupplierDebts.size,
                            icon = Icons.Default.LocalShipping
                        )
                    }
                    items(upcomingSupplierDebts, key = { it.debtId }) { debt ->
                        SupplierDebtReminderItem(
                            debt = debt,
                            onMarkPaid = { viewModel.markSupplierDebtAsPaid(debt.debtId) }
                        )
                    }
                }

                if (upcomingTreatments.isNotEmpty()) {
                    item {
                        DashboardSectionHeader(
                            title = stringResource(R.string.dashboard_upcoming_treatments_title),
                            count = upcomingTreatments.size,
                            icon = Icons.Default.MedicalServices
                        )
                    }
                    items(upcomingTreatments) { treatment ->
                        TreatmentReminderItem(
                            treatment = treatment,
                            petName = petIdToNameMap[treatment.petIdFk]
                                ?: stringResource(R.string.dashboard_unknown_pet),
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

@Composable
private fun DashboardHeader(
    clinicName: String,
    pendingItemsCount: Int,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel operativo",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = clinicName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                AssistChip(
                    onClick = {},
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    ),
                    label = {
                        Text(
                            text = if (pendingItemsCount == 0) "D\u00eda al corriente" else "$pendingItemsCount pendientes"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (pendingItemsCount == 0) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun DashboardSearch(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        label = { Text("Buscar cliente, tel\u00e9fono, mascota o producto") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = DashboardCardShape
    )
}

@Composable
private fun DashboardSearchResults(
    results: List<GlobalSearchResult>,
    onResultClick: (GlobalSearchResult) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            results.forEachIndexed { index, result ->
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = result.iconForType(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text(
                            text = result.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(
                            text = result.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onResultClick(result) }
                )
                if (index < results.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricsRow(
    salesToday: Double,
    pendingItemsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardMetricCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.dashboard_sales_today_title),
            value = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(salesToday),
            icon = Icons.Default.Payments,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        DashboardMetricCard(
            modifier = Modifier.weight(1f),
            title = "Pendientes",
            value = pendingItemsCount.toString(),
            icon = Icons.Default.WarningAmber,
            containerColor = if (pendingItemsCount == 0) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (pendingItemsCount == 0) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = DashboardCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardQuickActions(
    onAddSaleClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRestockClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAddSaleClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = DashboardCardShape,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Venta", maxLines = 1)
        }
        FilledTonalButton(
            onClick = onInventoryClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = DashboardCardShape,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Stock", maxLines = 1)
        }
        FilledTonalButton(
            onClick = onRestockClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = DashboardCardShape,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.restock_button), maxLines = 1)
        }
    }
}

@Composable
private fun DashboardCollectionPreview(
    rows: List<DebtCollectionRow>,
    totalPending: Double,
    onCollect: (DebtCollectionRow) -> Unit,
    onViewAll: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Cobros pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Total Gs. ${formatCurrency(totalPending)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onViewAll) {
                    Text("Ver todo")
                }
            }
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.client.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Debe Gs. ${formatCurrency(row.balance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = { onCollect(row) },
                        shape = DashboardCardShape,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cobrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        AssistChip(
            onClick = {},
            label = { Text(count.toString()) }
        )
    }
}

@Composable
private fun DashboardEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Todo al d\u00eda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No hay alertas ni recordatorios pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SupplierDebtReminderItem(
    debt: SupplierDebtWithSupplier,
    onMarkPaid: () -> Unit
) {
    val dueDate = Instant.ofEpochMilli(debt.dueDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
    val urgency = urgencyColors(daysUntil)
    val dateText = when {
        daysUntil < 0 -> "Vencida por ${-daysUntil} d\u00edas"
        daysUntil == 0L -> "Vence hoy"
        daysUntil == 1L -> "Vence manana"
        else -> "Vence en $daysUntil d\u00edas"
    }

    ReminderCard(
        icon = Icons.Default.LocalShipping,
        title = debt.supplierName ?: stringResource(R.string.label_no_supplier),
        status = dateText,
        statusColor = urgency.statusColor,
        containerColor = urgency.containerColor,
        contentColor = urgency.contentColor
    ) {
        Text(
            text = debt.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.supplier_debt_amount_label, formatCurrency(debt.amount)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onMarkPaid,
            shape = DashboardCardShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.supplier_debt_mark_paid_button))
        }
    }
}

@Composable
fun AppointmentReminderItem(details: AppointmentWithDetails) {
    val appointmentDate = Instant.ofEpochMilli(details.appointment.appointmentDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate)
    val urgency = urgencyColors(daysUntil)
    val dateText = when {
        daysUntil < 0 -> "Vencida"
        daysUntil == 0L -> "Hoy"
        daysUntil == 1L -> "Ma\u00f1ana"
        else -> "En $daysUntil d\u00edas (${appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM"))})"
    }

    ReminderCard(
        icon = Icons.Default.CalendarMonth,
        title = details.pet.name,
        status = dateText,
        statusColor = urgency.statusColor,
        containerColor = urgency.containerColor,
        contentColor = urgency.contentColor
    ) {
        Text(
            text = "Due\u00f1o: ${details.client.name}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Motivo: ${details.appointment.description ?: "No especificado"}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LowStockAlert(
    lowStockProducts: List<Product>,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ReminderCard(
        icon = Icons.Default.WarningAmber,
        title = stringResource(R.string.dashboard_low_stock_alert_title),
        status = lowStockProducts.size.toString(),
        statusColor = MaterialTheme.colorScheme.error,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .animateContentSize()
            .clickable { expanded = !expanded },
        trailingIcon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    ) {
        Text(
            text = if (expanded) "Lista ordenada por nombre. Puedes guardarla como imagen o PDF." else "Toca para ver la lista y exportarla.",
            style = MaterialTheme.typography.bodyMedium
        )
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onExportPng,
                        modifier = Modifier.weight(1f),
                        shape = DashboardCardShape,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PNG")
                    }
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f),
                        shape = DashboardCardShape,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF")
                    }
                }
                lowStockProducts.forEach { product ->
                    LowStockDashboardRow(product = product)
                }
            }
        }
    }
}

@Composable
private fun LowStockDashboardRow(product: Product) {
    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    val threshold = product.lowStockThreshold ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Minimo: ${formatCurrency(threshold).replace(",00", "")}$unit",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = "${formatCurrency(product.stock).replace(",00", "")}$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TreatmentReminderItem(
    treatment: Treatment,
    petName: String,
    onMarkAsCompleted: () -> Unit
) {
    val daysUntilNext = treatment.nextTreatmentDate?.let {
        TimeUnit.MILLISECONDS.toDays(it - System.currentTimeMillis())
    }
    val urgency = urgencyColors(daysUntilNext)
    val nextDateText = when {
        daysUntilNext == null -> stringResource(R.string.dashboard_next_appointment_not_defined)
        daysUntilNext < 0 -> stringResource(R.string.dashboard_appointment_overdue_days, -daysUntilNext)
        daysUntilNext == 0L -> stringResource(R.string.dashboard_appointment_today)
        else -> stringResource(R.string.dashboard_appointment_in_days, daysUntilNext)
    }

    ReminderCard(
        icon = Icons.Default.MedicalServices,
        title = petName,
        status = nextDateText,
        statusColor = urgency.statusColor,
        containerColor = urgency.containerColor,
        contentColor = urgency.contentColor
    ) {
        treatment.description?.let {
            Text(
                text = stringResource(R.string.dashboard_treatment_label, it),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = stringResource(R.string.dashboard_next_appointment_label, nextDateText),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onMarkAsCompleted,
            shape = DashboardCardShape
        ) {
            Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.dashboard_register_new_visit_button))
        }
    }
}

@Composable
private fun ReminderCard(
    icon: ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = status, color = statusColor)
                    trailingIcon?.let {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(it, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
    )
}

@Composable
private fun urgencyColors(daysUntil: Long?): ReminderColors {
    return when {
        daysUntil == null -> ReminderColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            statusColor = MaterialTheme.colorScheme.outline
        )
        daysUntil < 0 -> ReminderColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            statusColor = MaterialTheme.colorScheme.error
        )
        daysUntil <= 1 -> ReminderColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            statusColor = MaterialTheme.colorScheme.tertiary
        )
        daysUntil <= 3 -> ReminderColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            statusColor = MaterialTheme.colorScheme.secondary
        )
        else -> ReminderColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            statusColor = MaterialTheme.colorScheme.primary
        )
    }
}

private data class ReminderColors(
    val containerColor: Color,
    val contentColor: Color,
    val statusColor: Color
)

private fun GlobalSearchResult.iconForType(): ImageVector {
    return when (type) {
        "client" -> Icons.Default.People
        "pet" -> Icons.Default.Pets
        "product" -> Icons.Default.Inventory
        else -> Icons.Default.Search
    }
}

private suspend fun exportLowStockPng(
    context: Context,
    uri: Uri,
    products: List<Product>
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val sortedProducts = products.sortedBy { it.name.lowercase(Locale.getDefault()) }
        val width = 1200
        val rowHeight = 72
        val headerHeight = 180
        val footerHeight = 56
        val height = (headerHeight + sortedProducts.size * rowHeight + footerHeight).coerceAtLeast(360)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawLowStockPage(
            canvas = canvas,
            width = width,
            height = height,
            products = sortedProducts,
            startIndex = 0,
            maxItems = sortedProducts.size,
            pageNumber = 1,
            totalPages = 1
        )
        context.contentResolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: error("No se pudo abrir el destino del PNG.")
        bitmap.recycle()
    }.isSuccess
}

private suspend fun exportLowStockPdf(
    context: Context,
    uri: Uri,
    products: List<Product>
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val sortedProducts = products.sortedBy { it.name.lowercase(Locale.getDefault()) }
        val pageWidth = 595
        val pageHeight = 842
        val itemsPerPage = 12
        val totalPages = maxOf(1, kotlin.math.ceil(sortedProducts.size / itemsPerPage.toDouble()).toInt())
        val document = PdfDocument()

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            drawLowStockPage(
                canvas = page.canvas,
                width = pageWidth,
                height = pageHeight,
                products = sortedProducts,
                startIndex = pageIndex * itemsPerPage,
                maxItems = itemsPerPage,
                pageNumber = pageIndex + 1,
                totalPages = totalPages
            )
            document.finishPage(page)
        }

        context.contentResolver.openOutputStream(uri)?.use { output ->
            document.writeTo(output)
        } ?: error("No se pudo abrir el destino del PDF.")
        document.close()
    }.isSuccess
}

private fun drawLowStockPage(
    canvas: Canvas,
    width: Int,
    height: Int,
    products: List<Product>,
    startIndex: Int,
    maxItems: Int,
    pageNumber: Int,
    totalPages: Int
) {
    val background = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.rgb(250, 252, 250)
    }
    val primary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(23, 106, 87)
        textSize = if (width > 700) 42f else 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(85, 99, 94)
        textSize = if (width > 700) 24f else 14f
    }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(224, 244, 235)
        style = Paint.Style.FILL
    }
    val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(186, 26, 26)
        textSize = if (width > 700) 23f else 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(31, 41, 37)
        textSize = if (width > 700) 25f else 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(87, 99, 95)
        textSize = if (width > 700) 19f else 11f
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(218, 226, 221)
        strokeWidth = 1f
    }

    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
    canvas.drawRoundRect(24f, 24f, width - 24f, 132f, 18f, 18f, headerPaint)
    canvas.drawText("Alerta de stock bajo", 48f, 72f, primary)
    canvas.drawText("${products.size} productos ordenados por nombre", 48f, 108f, subtitle)
    canvas.drawText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), width - 170f, 72f, subtitle)

    val rows = products.drop(startIndex).take(maxItems)
    var y = 170f
    val rowHeight = if (width > 700) 72f else 50f
    rows.forEachIndexed { index, product ->
        val top = y + index * rowHeight
        canvas.drawRoundRect(24f, top - 34f, width - 24f, top + 22f, 10f, 10f, rowPaint)
        canvas.drawLine(40f, top + 26f, width - 40f, top + 26f, linePaint)
        val name = product.name.take(if (width > 700) 38 else 28)
        val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        val threshold = product.lowStockThreshold ?: 0.0
        canvas.drawText(name, 48f, top, textPaint)
        canvas.drawText(
            "Minimo ${formatCurrency(threshold).replace(",00", "")}$unit",
            48f,
            top + 24f,
            smallPaint
        )
        canvas.drawText(
            "Stock ${formatCurrency(product.stock).replace(",00", "")}$unit",
            width - 230f,
            top + 6f,
            warningPaint
        )
    }

    canvas.drawText("Pagina $pageNumber de $totalPages", 48f, height - 32f, subtitle)
    canvas.drawText("VetFinance", width - 150f, height - 32f, subtitle)
}
