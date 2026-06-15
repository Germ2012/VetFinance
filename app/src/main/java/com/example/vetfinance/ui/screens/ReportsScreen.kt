package com.example.vetfinance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.vetfinance.domain.model.CategoryProfitReport
import com.example.vetfinance.domain.model.ClientPurchaseReport
import com.example.vetfinance.domain.model.ProductProfitReport
import com.example.vetfinance.domain.model.SalesTrendComparisonPoint
import com.example.vetfinance.domain.model.StockHealthSummary
import com.example.vetfinance.viewmodel.HistoricalPeriod
import com.example.vetfinance.viewmodel.ReportPeriodType
import com.example.vetfinance.viewmodel.TopProductsMetric
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
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf(
        "Caja",
        stringResource(R.string.tab_sales_and_backups),
        stringResource(R.string.tab_top_products),
        "Rentabilidad",
        stringResource(R.string.tab_debts),
        stringResource(R.string.tab_inventory)
    )
    val tabIcons = listOf(
        Icons.Default.Payments,
        Icons.Default.PointOfSale,
        Icons.Default.Assessment,
        Icons.Default.AddShoppingCart,
        Icons.Default.People,
        Icons.Default.Inventory
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportsHeader()
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp
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
                0 -> CashClosingTab(viewModel)
                1 -> SalesAndBackupTab(viewModel)
                2 -> TopProductsReportTab(viewModel)
                3 -> ProfitabilityReportTab(viewModel)
                4 -> DebtsReportTab(viewModel)
                5 -> InventoryReportTab(viewModel)
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
                    "Caja, ventas, rentabilidad, deuda e inventario en una vista de control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CashClosingTab(viewModel: VetViewModel) {
    val summary by viewModel.cashClosingSummary.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReportModuleHeader(
                icon = Icons.Default.Payments,
                title = "Cierre de caja diario",
                body = "Resume lo registrado hoy para revisar ventas, cobros y ajustes antes de cerrar el dia."
            )
        }
        item {
            SummaryCard(
                title = "Actividad registrada hoy",
                value = "Gs. ${formatCurrency(summary.operationalTotal)}",
                icon = Icons.Default.Payments
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMiniMetric(
                    label = "Ventas",
                    value = summary.salesCount.toString(),
                    icon = Icons.Default.PointOfSale,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniMetric(
                    label = "Facturado",
                    value = "Gs. ${formatCurrency(summary.salesTotal)}",
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMiniMetric(
                    label = "Cobros",
                    value = "Gs. ${formatCurrency(summary.paymentsTotal)}",
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                ReportMiniMetric(
                    label = "Aumentos deuda",
                    value = "Gs. ${formatCurrency(summary.debtIncreases)}",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    isWarning = summary.debtIncreases > 0.0
                )
                ReportMiniMetric(
                    label = "Ajustes deuda",
                    value = "Gs. ${formatCurrency(summary.debtAdjustments)}",
                    icon = Icons.Default.WarningAmber,
                    modifier = Modifier.weight(1f),
                    isWarning = summary.debtAdjustments != 0.0
                )
            }
        }
        item {
            ReportInlineNote(
                icon = Icons.Default.CheckCircle,
                text = "El cierre usa los registros del dia: ventas como facturacion, pagos como cobros y ajustes como movimientos de deuda."
            )
        }
        if (summary.salesCount == 0 && summary.paymentsTotal == 0.0 && summary.debtIncreases == 0.0) {
            item {
                ReportEmptyState(
                    icon = Icons.Default.PointOfSale,
                    title = "Sin actividad para cerrar",
                    body = "Cuando registres ventas, pagos o ajustes, el cierre se armara automaticamente."
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
    val salesTrendComparison by viewModel.salesTrendComparison.collectAsState()

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
            SalesTrendComparisonChart(points = salesTrendComparison)
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
    val selectedMetric by viewModel.topProductsMetric.collectAsState()
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
            selectedMetric = selectedMetric,
            selectedDate = selectedDate,
            onPeriodSelected = { viewModel.onTopProductsPeriodSelected(it) },
            onMetricSelected = { viewModel.onTopProductsMetricSelected(it) },
            onDateSelectorClick = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (topProducts.isNotEmpty()) {
            val totalMetricValue = topProducts.sumOf { it.metricValue(selectedMetric) }
            val chartTitle = if (selectedMetric == TopProductsMetric.QUANTITY) {
                "Unidades vendidas por posicion"
            } else {
                "Ingreso generado por posicion"
            }
            val chartColors = remember {
                listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFF5722), Color(0xFF009688), Color(0xFF795548), Color(0xFF607D8B), Color(0xFF3F51B5))
            }

            val barChartData = BarChartData(
                chartData = topProducts.mapIndexed { index, product ->
                    val isSelected = selectedProduct == product
                    BarData(
                        point = Point(index.toFloat(), product.metricValue(selectedMetric).toFloat()),
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
                        if (selectedMetric == TopProductsMetric.REVENUE) {
                            "Gs. ${formatCurrency(value.toDouble()).replace(",00", "")}"
                        } else if (value % 1.0f == 0f) {
                            value.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.2f", value)
                        }
                    }
                    .build(),
                barStyle = BarStyle(barWidth = 35.dp)
            )

            ReportChartPanel(title = chartTitle) {
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
                    val percentage = if(totalMetricValue > 0) (product.metricValue(selectedMetric) / totalMetricValue) * 100 else 0.0
                    SelectedTopProductCard(product = product, metric = selectedMetric, percentage = percentage)
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
                        supportingText = product.metricLabel(selectedMetric),
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

@Composable
fun ProfitabilityReportTab(viewModel: VetViewModel) {
    val productReports by viewModel.productProfitReports.collectAsState()
    val clientReports by viewModel.clientPurchaseReports.collectAsState()
    val categoryReports by viewModel.categoryProfitReports.collectAsState()
    val selectedHistoricalPeriod by viewModel.selectedHistoricalPeriod.collectAsState()
    val totalRevenue = remember(productReports) { productReports.sumOf { it.revenue } }
    val totalProfit = remember(productReports) { productReports.sumOf { it.profit } }
    val averageMargin = remember(productReports, totalRevenue) {
        if (totalRevenue > 0.0) (totalProfit / totalRevenue) * 100.0 else 0.0
    }
    val topProfit = remember(productReports) { productReports.take(5) }
    val topMargin = remember(productReports) {
        productReports.filter { it.revenue > 0.0 }.sortedByDescending { it.marginPercent }.take(5)
    }
    val topServices = remember(productReports) {
        productReports.filter { it.isService }.sortedByDescending { it.profit }.take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReportModuleHeader(
                icon = Icons.Default.AddShoppingCart,
                title = "Rentabilidad",
                body = "Compara ganancia, margen, servicios y clientes con mayor compra acumulada."
            )
        }
        item {
            PeriodSelector(viewModel)
        }
        if (productReports.isEmpty()) {
            item {
                ReportEmptyState(
                    icon = Icons.Default.Assessment,
                    title = "Sin datos de rentabilidad",
                    body = "Cuando haya ventas registradas, este modulo calculara ingresos, costos y margen."
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        title = "Ingresos analizados",
                        value = "Gs. ${formatCurrency(totalRevenue)}",
                        icon = Icons.Default.PointOfSale,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Ganancia estimada",
                        value = "Gs. ${formatCurrency(totalProfit)}",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                ReportMiniMetric(
                    label = "Margen promedio",
                    value = "${String.format(Locale.getDefault(), "%.1f", averageMargin)}%",
                    icon = Icons.Default.Assessment
                )
            }
            item {
                CategoryProfitChart(
                    reports = categoryReports,
                    periodLabel = selectedHistoricalPeriod?.displayName ?: stringResource(R.string.no_period_selected)
                )
            }
            item {
                ReportSectionTitle("Productos con mayor ganancia")
            }
            items(topProfit) { row ->
                ProductProfitReportRow(
                    report = row,
                    primaryLabel = "Ganancia Gs. ${formatCurrency(row.profit)}",
                    secondaryLabel = "Ingreso Gs. ${formatCurrency(row.revenue)}"
                )
            }
            item {
                ReportSectionTitle("Mejor margen")
            }
            items(topMargin) { row ->
                ProductProfitReportRow(
                    report = row,
                    primaryLabel = "${String.format(Locale.getDefault(), "%.1f", row.marginPercent)}% margen",
                    secondaryLabel = "Costo Gs. ${formatCurrency(row.cost)}"
                )
            }
            if (topServices.isNotEmpty()) {
                item {
                    ReportSectionTitle("Servicios mas rentables")
                }
                items(topServices) { row ->
                    ProductProfitReportRow(
                        report = row,
                        primaryLabel = "Ganancia Gs. ${formatCurrency(row.profit)}",
                        secondaryLabel = "Servicios vendidos ${formatCurrency(row.quantitySold).replace(",00", "")}"
                    )
                }
            }
            if (clientReports.isNotEmpty()) {
                item {
                    ReportSectionTitle("Clientes que mas compran")
                }
                items(clientReports.take(5), key = { it.clientId }) { row ->
                    ClientPurchaseReportRow(row)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopProductsFilterControls(
    selectedPeriod: TopProductsPeriod,
    selectedMetric: TopProductsMetric,
    selectedDate: java.time.LocalDate,
    onPeriodSelected: (TopProductsPeriod) -> Unit,
    onMetricSelected: (TopProductsMetric) -> Unit,
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val metricOptions = listOf(TopProductsMetric.QUANTITY, TopProductsMetric.REVENUE)
                metricOptions.forEachIndexed { index, metric ->
                    SegmentedButton(
                        onClick = { onMetricSelected(metric) },
                        selected = metric == selectedMetric,
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = metricOptions.size)
                    ) {
                        Text(if (metric == TopProductsMetric.QUANTITY) "Cantidad vendida" else "Monto ingresado")
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
    supportingText: String,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(supportingText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
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
    val stockHealth by viewModel.stockHealthSummary.collectAsState()
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
        item {
            StockHealthCard(summary = stockHealth)
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
private fun ReportSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ProductProfitReportRow(
    report: ProductProfitReport,
    primaryLabel: String,
    secondaryLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (report.isService) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (report.isService) Icons.Default.CheckCircle else Icons.Default.Inventory,
                    contentDescription = null,
                    tint = if (report.isService) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(report.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (report.isService) "Servicio" else "Producto",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(secondaryLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                primaryLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClientPurchaseReportRow(report: ClientPurchaseReport) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(report.clientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${report.saleCount} compras registradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Gs. ${formatCurrency(report.totalPurchased)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
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
private fun SalesTrendComparisonChart(points: List<SalesTrendComparisonPoint>) {
    var selectedIndex by remember(points) {
        mutableStateOf(points.indexOfLast { it.currentSales > 0.0 || it.previousSales > 0.0 }.takeIf { it >= 0 })
    }
    val maxValue = remember(points) {
        maxOf(1.0, points.maxOfOrNull { maxOf(it.currentSales, it.previousSales) } ?: 0.0)
    }
    val currentColor = MaterialTheme.colorScheme.primary
    val previousColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    ReportChartPanel(title = "Tendencia de ventas: semana actual vs anterior") {
        if (points.isEmpty()) {
            Text(
                "Todavia no hay ventas suficientes para comparar semanas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ReportChartPanel
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val top = 10.dp.toPx()
            val bottom = 20.dp.toPx()
            val left = 12.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val usableHeight = (size.height - top - bottom).coerceAtLeast(1f)
            val usableWidth = (right - left).coerceAtLeast(1f)
            val divisor = points.lastIndex.coerceAtLeast(1)

            fun offsetFor(index: Int, value: Double): Offset {
                val normalized = (value / maxValue).toFloat().coerceIn(0f, 1f)
                return Offset(
                    x = left + usableWidth * (index.toFloat() / divisor),
                    y = top + usableHeight * (1f - normalized)
                )
            }

            repeat(4) { step ->
                val y = top + usableHeight * (step / 3f)
                drawLine(
                    color = gridColor.copy(alpha = 0.55f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun pathFor(selector: (SalesTrendComparisonPoint) -> Double): Path {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val offset = offsetFor(index, selector(point))
                    if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                }
                return path
            }

            drawPath(
                path = pathFor { it.previousSales },
                color = previousColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = pathFor { it.currentSales },
                color = currentColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, point ->
                val isSelected = selectedIndex == index
                drawCircle(
                    color = previousColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                    center = offsetFor(index, point.previousSales)
                )
                drawCircle(
                    color = currentColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                    center = offsetFor(index, point.currentSales)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChartLegendDot(color = currentColor, label = "Actual")
            ChartLegendDot(color = previousColor, label = "Anterior")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.forEachIndexed { index, point ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    label = { Text(point.label, maxLines = 1) }
                )
            }
        }

        selectedIndex?.let { index ->
            points.getOrNull(index)?.let { point ->
                val delta = point.currentSales - point.previousSales
                val deltaText = if (delta >= 0) "+Gs. ${formatCurrency(delta)}" else "-Gs. ${formatCurrency(kotlin.math.abs(delta))}"
                Text(
                    "${point.label}: actual Gs. ${formatCurrency(point.currentSales)} | anterior Gs. ${formatCurrency(point.previousSales)} | variacion $deltaText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryProfitChart(
    reports: List<CategoryProfitReport>,
    periodLabel: String
) {
    var selectedCategory by remember(reports) { mutableStateOf(reports.firstOrNull()?.category) }
    val maxProfit = remember(reports) {
        maxOf(1.0, reports.maxOfOrNull { it.profit.coerceAtLeast(0.0) } ?: 0.0)
    }

    ReportChartPanel(title = "Rentabilidad por categoria - $periodLabel") {
        if (reports.isEmpty()) {
            Text(
                "No hay ventas con categorias para el periodo seleccionado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ReportChartPanel
        }

        reports.forEach { report ->
            val selected = selectedCategory == report.category
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { selectedCategory = report.category }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        report.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Gs. ${formatCurrency(report.profit)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (report.profit < 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { (report.profit.coerceAtLeast(0.0) / maxProfit).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = if (report.profit < 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        reports.firstOrNull { it.category == selectedCategory }?.let { selected ->
            Text(
                "Ingreso Gs. ${formatCurrency(selected.revenue)} | costo Gs. ${formatCurrency(selected.cost)} | margen ${String.format(Locale.getDefault(), "%.1f", selected.marginPercent)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StockHealthCard(summary: StockHealthSummary) {
    var expanded by remember { mutableStateOf(false) }
    val total = summary.totalCount
    val optimalRatio = if (total > 0) summary.optimalCount.toFloat() / total.toFloat() else 0f

    ReportChartPanel(title = "Estado de inventario") {
        if (total == 0) {
            Text(
                "Todavia no hay productos inventariables para medir salud de stock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ReportChartPanel
        }

        LinearProgressIndicator(
            progress = { optimalRatio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.errorContainer
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StockHealthMetric(
                label = "Optimos",
                value = summary.optimalCount.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            StockHealthMetric(
                label = "Bajo stock",
                value = summary.lowStockCount.toString(),
                icon = Icons.Default.WarningAmber,
                modifier = Modifier.weight(1f),
                isWarning = summary.lowStockCount > 0
            )
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Ocultar lectura" else "Ver lectura")
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                if (summary.lowStockCount > 0) {
                    "${summary.lowStockCount} productos necesitan reposicion o ajuste. Prioriza los que sostienen ventas frecuentes."
                } else {
                    "El inventario esta saludable: todos los productos medidos estan por encima de su umbral."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StockHealthMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SelectedTopProductCard(
    product: TopSellingProduct,
    metric: TopProductsMetric,
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
            Text("Monto ingresado: Gs. ${formatCurrency(product.totalRevenue)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (metric == TopProductsMetric.QUANTITY) {
                    stringResource(R.string.text_represents_percentage_sales, percentage)
                } else {
                    "Representa ${String.format(Locale.getDefault(), "%.1f", percentage)}% del ingreso del ranking"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun TopSellingProduct.metricValue(metric: TopProductsMetric): Double {
    return when (metric) {
        TopProductsMetric.QUANTITY -> totalSold
        TopProductsMetric.REVENUE -> totalRevenue
    }
}

private fun TopSellingProduct.metricLabel(metric: TopProductsMetric): String {
    return when (metric) {
        TopProductsMetric.QUANTITY -> "${formatCurrency(totalSold).replace(",00", "")} vendidos"
        TopProductsMetric.REVENUE -> "Gs. ${formatCurrency(totalRevenue)}"
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
