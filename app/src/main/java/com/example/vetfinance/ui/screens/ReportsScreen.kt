package com.example.vetfinance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import com.example.vetfinance.R
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.TopSellingProduct
import com.example.vetfinance.viewmodel.HistoricalPeriod
import com.example.vetfinance.viewmodel.ReportPeriodType
import com.example.vetfinance.viewmodel.TopProductsPeriod
import com.example.vetfinance.viewmodel.VetViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import ui.utils.formatCurrency
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(viewModel: VetViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf(
        stringResource(R.string.tab_sales_and_backups),
        stringResource(R.string.tab_top_products),
        stringResource(R.string.tab_debts),
        stringResource(R.string.tab_inventory)
    )
    val tabIcons = listOf(
        Icons.Default.PointOfSale,
        Icons.Default.Assessment,
        Icons.Default.People,
        Icons.Default.Inventory
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportsHeader()
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        Icon(tabIcons[index], contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> SalesAndBackupTab(viewModel)
                1 -> TopProductsReportTab(viewModel)
                2 -> DebtsReportTab(viewModel)
                3 -> InventoryReportTab(viewModel)
            }
        }
    }
}

@Composable
private fun ReportsHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Reportes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Ventas, respaldo, deuda e inventario en una vista de control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesAndBackupTab(viewModel: VetViewModel) {
    val salesSummary by viewModel.salesSummary.collectAsState()
    val grossProfit by viewModel.grossProfitSummary.collectAsState()
    val selectedHistoricalPeriod by viewModel.selectedHistoricalPeriod.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val noDataToExportMsg = stringResource(R.string.toast_no_data_to_export)
    val exportCompletedMsg = stringResource(R.string.toast_export_completed)
    val exportErrorMsg = stringResource(R.string.toast_export_error)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val resultado = viewModel.importarDatosDesdeZIP(it, context)
                Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            scope.launch {
                val csvDataMap = viewModel.exportarDatosCompletos()
                if (csvDataMap.isEmpty()) {
                    Toast.makeText(context, noDataToExportMsg, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zos ->
                            csvDataMap.forEach { (fileName, content) ->
                                zos.putNextEntry(ZipEntry(fileName))
                                zos.write(content.toByteArray())
                                zos.closeEntry()
                            }
                        }
                    }
                    viewModel.markBackupCreated()
                    Toast.makeText(context, exportCompletedMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    val errorDetail = e.message ?: ""
                    Toast.makeText(context, "$exportErrorMsg: $errorDetail", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val formattedSales = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(salesSummary)
    val formattedProfit = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(grossProfit)
    val summaryTitle = selectedHistoricalPeriod?.displayName ?: stringResource(R.string.no_period_selected)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleHeader(
                icon = Icons.Default.PointOfSale,
                title = "Rendimiento de ventas",
                body = "Elige un periodo historico y revisa ingresos junto con margen bruto."
            )
        }
        item {
            PeriodSelector(viewModel)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = stringResource(R.string.summary_total_sales, summaryTitle),
                    value = formattedSales,
                    icon = Icons.Default.PointOfSale,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = stringResource(R.string.summary_gross_profit, summaryTitle),
                    value = formattedProfit,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            BackupPanel(
                onImport = { importLauncher.launch(arrayOf("application/zip")) },
                onExport = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "VetFinance_backup_$timestamp.zip"
                    exportLauncher.launch(fileName)
                }
            )
        }
        item {
            ReportInlineNote(
                icon = Icons.Default.CheckCircle,
                text = "Los respaldos se exportan como ZIP con los datos principales de VetFinance."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(viewModel: VetViewModel) {
    val periodType by viewModel.reportPeriodType.collectAsState()
    val availablePeriods by viewModel.availableHistoricalPeriods.collectAsState()
    val selectedPeriod by viewModel.selectedHistoricalPeriod.collectAsState()
    var periodTypeExpanded by remember { mutableStateOf(false) }
    var historicalPeriodExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        // Menú para seleccionar el TIPO de período (Día, Semana, Mes)
        ExposedDropdownMenuBox(
            expanded = periodTypeExpanded,
            onExpandedChange = { periodTypeExpanded = !periodTypeExpanded },
            modifier = Modifier.weight(1.0f)
        ) {
            OutlinedTextField(
                value = stringResource(id = periodType.displayResId),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_period_type)) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodTypeExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = periodTypeExpanded,
                onDismissRequest = { periodTypeExpanded = false }
            ) {
                ReportPeriodType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(stringResource(id = type.displayResId)) },
                        onClick = {
                            viewModel.onReportPeriodTypeChanged(type)
                            periodTypeExpanded = false
                        }
                    )
                }
            }
        }

        // Menú para seleccionar el período HISTÓRICO específico
        ExposedDropdownMenuBox(
            expanded = historicalPeriodExpanded,
            onExpandedChange = { historicalPeriodExpanded = !historicalPeriodExpanded },
            modifier = Modifier.weight(1.5f)
        ) {
            val selectionText = selectedPeriod?.displayName ?: stringResource(R.string.no_period_selected)
            OutlinedTextField(
                value = selectionText,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_select_period)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = historicalPeriodExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = historicalPeriodExpanded,
                onDismissRequest = { historicalPeriodExpanded = false }
            ) {
                if (availablePeriods.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_sales_data_period)) },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    availablePeriods.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period.displayName) },
                            onClick = {
                                viewModel.onHistoricalPeriodSelected(period)
                                historicalPeriodExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopProductsReportTab(viewModel: VetViewModel) {
    val topProducts by viewModel.topSellingProducts.collectAsState()
    val selectedProduct by viewModel.selectedTopProduct.collectAsState()
    val selectedPeriod by viewModel.topProductsPeriod.collectAsState()
    val selectedDate by viewModel.topProductsDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onTopProductsDateSelected(it) }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ReportModuleHeader(
            icon = Icons.Default.Assessment,
            title = stringResource(R.string.title_top_selling_products),
            body = "Detecta los productos que sostienen la venta y compara su participacion."
        )
        Spacer(modifier = Modifier.height(14.dp))

        TopProductsFilterControls(
            selectedPeriod = selectedPeriod,
            selectedDate = selectedDate,
            onPeriodSelected = { viewModel.onTopProductsPeriodSelected(it) },
            onDateSelectorClick = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (topProducts.isNotEmpty()) {
            val totalSold = topProducts.sumOf { it.totalSold }
            val chartColors = remember {
                listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFF5722), Color(0xFF009688), Color(0xFF795548), Color(0xFF607D8B), Color(0xFF3F51B5))
            }

            val barChartData = BarChartData(
                chartData = topProducts.mapIndexed { index, product ->
                    val isSelected = selectedProduct == product
                    BarData(
                        point = Point(index.toFloat(), product.totalSold.toFloat()),
                        label = "",
                        color = chartColors[index % chartColors.size].copy(alpha = if (isSelected) 1f else 0.4f)
                    )
                },
                xAxisData = AxisData.Builder().labelData { "" }.build(),
                yAxisData = AxisData.Builder()
                    .steps(5)
                    .labelAndAxisLinePadding(20.dp)
                    .labelData { value ->
                        // CORREGIDO: Se usa el operador módulo (%) que es más estándar y robusto.
                        if (value % 1.0f == 0f) {
                            value.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.2f", value)
                        }
                    }
                    .build(),
                barStyle = BarStyle(barWidth = 35.dp)
            )

            ReportChartPanel(title = "Unidades vendidas por posicion") {
                BarChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    barChartData = barChartData
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = selectedProduct != null) {
                selectedProduct?.let { product ->
                    val percentage = if(totalSold > 0) (product.totalSold / totalSold) * 100 else 0.0
                    SelectedTopProductCard(product = product, percentage = percentage)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(topProducts) { index, product ->
                    val isSelected = selectedProduct == product
                    LegendItem(
                        name = product.name,
                        color = chartColors[index % chartColors.size],
                        isSelected = isSelected,
                        onClick = { viewModel.onTopProductSelected(product) }
                    )
                }
            }

        } else {
            ReportEmptyState(
                icon = Icons.Default.Assessment,
                title = stringResource(R.string.text_no_sales_data_period),
                body = "Cuando registres ventas, este grafico mostrara los productos mas importantes."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopProductsFilterControls(
    selectedPeriod: TopProductsPeriod,
    selectedDate: java.time.LocalDate,
    onPeriodSelected: (TopProductsPeriod) -> Unit,
    onDateSelectorClick: () -> Unit
) {
    val wordDe = stringResource(R.string.word_de)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val options = TopProductsPeriod.values()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, period ->
                    SegmentedButton(
                        onClick = { onPeriodSelected(period) },
                        selected = period == selectedPeriod,
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(stringResource(period.displayResId))
                    }
                }
            }
            val formatter = when (selectedPeriod) {
                TopProductsPeriod.WEEK -> DateTimeFormatter.ofPattern("w '${wordDe}' YYYY", Locale("es", "ES"))
                TopProductsPeriod.MONTH -> DateTimeFormatter.ofPattern("MMMM '${wordDe}' yyyy", Locale("es", "ES"))
                TopProductsPeriod.YEAR -> DateTimeFormatter.ofPattern("yyyy", Locale("es", "ES"))
            }
            FilledTonalButton(onClick = onDateSelectorClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedDate.format(formatter).replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
fun LegendItem(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(14.dp).background(color, CircleShape))
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DebtsReportTab(viewModel: VetViewModel) {
    val totalDebt by viewModel.totalDebt.collectAsState()
    val formattedDebt = stringResource(R.string.label_client_debt_amount, formatCurrency(totalDebt ?: 0.0))
    val clients by viewModel.clients.collectAsState()
    val clientsWithDebt = remember(clients) { clients.filter { it.debtAmount > 0 }.sortedByDescending { it.debtAmount } }
    val averageDebt = remember(clientsWithDebt) {
        if (clientsWithDebt.isEmpty()) 0.0 else clientsWithDebt.sumOf { it.debtAmount } / clientsWithDebt.size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReportModuleHeader(
                icon = Icons.Default.People,
                title = stringResource(R.string.title_debt_details),
                body = "Prioriza cobros pendientes y revisa cuanto esta expuesto en clientes."
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.title_total_pending_debt),
                value = formattedDebt,
                icon = Icons.Default.Payments
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMiniMetric(
                    label = "Clientes con deuda",
                    value = clientsWithDebt.size.toString(),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniMetric(
                    label = "Promedio",
                    value = "Gs. ${formatCurrency(averageDebt)}",
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (clientsWithDebt.isEmpty()) {
            item {
                ReportEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = "Sin deudas pendientes",
                    body = "Los clientes estan al dia en este momento."
                )
            }
        } else {
            items(clientsWithDebt, key = { it.clientId }) { client ->
                DebtClientReportRow(client = client, maxDebt = clientsWithDebt.first().debtAmount)
            }
        }
    }
}

@Composable
fun InventoryReportTab(viewModel: VetViewModel) {
    val totalValue by viewModel.totalInventoryValue.collectAsState()
    val formattedValue = stringResource(R.string.label_client_debt_amount, formatCurrency(totalValue ?: 0.0))
    val inventory by viewModel.inventory.collectAsState()
    val productsOnly = remember(inventory) {
        inventory.filter { !it.isService }.sortedWith(
            compareBy<Product> { product ->
                val threshold = product.lowStockThreshold ?: 0.0
                !(threshold > 0 && product.stock < threshold)
            }.thenBy { it.name.lowercase(Locale.getDefault()) }
        )
    }
    val lowStockCount = remember(productsOnly) {
        productsOnly.count { product ->
            val threshold = product.lowStockThreshold ?: 0.0
            threshold > 0 && product.stock < threshold
        }
    }
    val totalUnits = remember(productsOnly) { productsOnly.sumOf { it.stock } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReportModuleHeader(
                icon = Icons.Default.Inventory,
                title = stringResource(R.string.title_stock_details_products),
                body = "Consulta valor, unidades y productos con riesgo de reposicion."
            )
        }
        item {
            SummaryCard(
                title = stringResource(R.string.title_total_inventory_value),
                value = formattedValue,
                icon = Icons.Default.Inventory
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMiniMetric(
                    label = "Productos",
                    value = productsOnly.size.toString(),
                    icon = Icons.Default.AddShoppingCart,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniMetric(
                    label = "Alertas stock",
                    value = lowStockCount.toString(),
                    icon = Icons.Default.WarningAmber,
                    modifier = Modifier.weight(1f),
                    isWarning = lowStockCount > 0
                )
                ReportMiniMetric(
                    label = "Unidades",
                    value = formatCurrency(totalUnits).replace(",00", ""),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (productsOnly.isEmpty()) {
            item {
                ReportEmptyState(
                    icon = Icons.Default.Inventory,
                    title = "Sin productos inventariables",
                    body = "Cuando agregues productos fisicos, apareceran aqui con su stock."
                )
            }
        } else {
            items(productsOnly, key = { it.productId }) { product ->
                InventoryStockReportRow(product = product)
            }
        }
    }
}

@Composable
private fun ReportModuleHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BackupPanel(
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Column {
                    Text("Respaldo de datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Importa o exporta un paquete ZIP completo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_import))
                }
                Button(onClick = onExport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_export))
                }
            }
        }
    }
}

@Composable
private fun ReportInlineNote(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportChartPanel(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SelectedTopProductCard(
    product: TopSellingProduct,
    percentage: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.text_quantity_sold, product.totalSold.toString()), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.text_represents_percentage_sales, percentage), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReportMiniMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReportEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DebtClientReportRow(client: Client, maxDebt: Double) {
    val ratio = if (maxDebt > 0) (client.debtAmount / maxDebt).coerceIn(0.0, 1.0).toFloat() else 0f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(client.phone ?: "Sin telefono", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Gs. ${formatCurrency(client.debtAmount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun InventoryStockReportRow(product: Product) {
    val threshold = product.lowStockThreshold ?: 0.0
    val isLowStock = threshold > 0 && product.stock < threshold
    val reference = (product.lowStockThreshold ?: product.stock.coerceAtLeast(1.0)).coerceAtLeast(1.0)
    val ratio = (product.stock / reference).coerceIn(0.0, 1.0).toFloat()
    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isLowStock) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Costo Gs. ${formatCurrency(product.cost)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Stock ${formatCurrency(product.stock).replace(",00", "")}$unit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            if (isLowStock) {
                Text("Revisar reposicion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
