// ruta: app/src/main/java/com/example/vetfinance/ui/screens/ReportsScreen.kt
package com.example.vetfinance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.vetfinance.viewmodel.Period
import com.example.vetfinance.viewmodel.TopProductsPeriod
import com.example.vetfinance.viewmodel.VetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import ui.utils.formatCurrency // Importar formatCurrency

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(viewModel: VetViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Ventas y Backups", "Top Productos", "Deudas", "Inventario")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
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
fun SalesAndBackupTab(viewModel: VetViewModel) {
    var selectedPeriod by remember { mutableStateOf(Period.DAY) }
    val salesSummary = viewModel.getSalesSummary(selectedPeriod)
    val grossProfit = viewModel.getGrossProfitSummary(selectedPeriod)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                    Toast.makeText(context, "No hay datos para exportar.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Exportación completada.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
    ) {
        SegmentedControl(selected = selectedPeriod, onPeriodSelected = { newPeriod -> selectedPeriod = newPeriod })
        val formattedSales = "Gs. ${formatCurrency(salesSummary)}"
        val formattedProfit = "Gs. ${formatCurrency(grossProfit)}"

        SummaryCard(title = "Total Ventas (${selectedPeriod.displayName})", value = formattedSales)
        SummaryCard(title = "Beneficio Bruto (${selectedPeriod.displayName})", value = formattedProfit)

        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { importLauncher.launch(arrayOf("application/zip")) }) { Text("Importar") }
            Button(onClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("backup_vetfinance_$timestamp.zip")
            }) { Text("Exportar") }
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
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Productos y Servicios Más Vendidos", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Controles de Filtro
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
                        label = "", // Sin etiqueta en el eje X
                        color = chartColors[index % chartColors.size].copy(alpha = if (isSelected) 1f else 0.4f)
                    )
                },
                xAxisData = AxisData.Builder().labelData { "" }.build(), // Ocultar etiquetas del eje X
                yAxisData = AxisData.Builder()
                    .steps(5)
                    .labelAndAxisLinePadding(20.dp)
                    .labelData { value -> value.toInt().toString() }
                    .build(),
                barStyle = BarStyle(barWidth = 35.dp)
            )

            BarChart(modifier = Modifier.height(250.dp), barChartData = barChartData)

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de detalles del producto seleccionado
            AnimatedVisibility(visible = selectedProduct != null) {
                selectedProduct?.let { product ->
                    val percentage = (product.totalSold.toFloat() / totalSold) * 100
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Cantidad vendida: ${product.totalSold}")
                            Text(String.format("Representa el %.2f%% de las ventas", percentage))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leyenda Interactiva
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxWidth(),
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay datos de ventas para el período seleccionado.")
            }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        val options = TopProductsPeriod.values()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, period ->
                SegmentedButton(
                    onClick = { onPeriodSelected(period) },
                    selected = period == selectedPeriod,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(period.displayName)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDateSelectorClick, modifier = Modifier.fillMaxWidth()) {
            val formatter = when (selectedPeriod) {
                TopProductsPeriod.WEEK -> DateTimeFormatter.ofPattern("w 'de' YYYY", Locale("es", "ES"))
                TopProductsPeriod.MONTH -> DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("es", "ES"))
                TopProductsPeriod.YEAR -> DateTimeFormatter.ofPattern("yyyy", Locale("es", "ES"))
            }
            Text(selectedDate.format(formatter).replaceFirstChar { it.uppercase() })
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
    val borderModifier = if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
        Text(name, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
fun DebtsReportTab(viewModel: VetViewModel) {
    val totalDebt by viewModel.totalDebt.collectAsState()
    val formattedDebt = "Gs. ${formatCurrency(totalDebt ?: 0.0)}"
    val clients by viewModel.clients.collectAsState()
    val clientsWithDebt = remember(clients) { clients.filter { it.debtAmount > 0 } }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(title = "Deuda Total Pendiente de Clientes", value = formattedDebt)
        HorizontalDivider()
        Text("Detalle de Deudas", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clientsWithDebt) { client ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = client.name)
                    Text(text = "Gs. ${formatCurrency(client.debtAmount)}")
                }
            }
        }
    }
}


@Composable
fun InventoryReportTab(viewModel: VetViewModel) {
    val totalValue by viewModel.totalInventoryValue.collectAsState()
    val formattedValue = "Gs. ${formatCurrency(totalValue ?: 0.0)}"
    val inventory by viewModel.inventory.collectAsState()
    val productsOnly = remember(inventory) { inventory.filter { !it.isService } }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(title = "Valor Total del Inventario", value = formattedValue)
        HorizontalDivider()
        Text("Detalle de Stock (Productos)", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(productsOnly) { product ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = product.name)
                    Text(text = "Stock: ${formatCurrency(product.stock.toDouble())}") // Asumiendo que stock puede ser Double
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControl(selected: Period, onPeriodSelected: (Period) -> Unit) {
    val options = Period.values()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, period ->
            SegmentedButton(
                onClick = { onPeriodSelected(period) },
                selected = period == selected,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(period.displayName)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.displaySmall)
        }
    }
}
