// ruta: app/src/main/java/com/example/vetfinance/ui/screens/ReportsScreen.kt
package com.example.vetfinance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import com.example.vetfinance.viewmodel.Period
import com.example.vetfinance.viewmodel.VetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    val grossProfit = viewModel.getGrossProfitSummary(selectedPeriod) // Nuevo cálculo
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
        val formattedSales = String.format("₲ %,.0f", salesSummary).replace(",", ".")
        val formattedProfit = String.format("₲ %,.0f", grossProfit).replace(",", ".") // Formatear nuevo valor

        SummaryCard(title = "Total Ventas (${selectedPeriod.displayName})", value = formattedSales)
        SummaryCard(title = "Beneficio Bruto (${selectedPeriod.displayName})", value = formattedProfit) // Nueva tarjeta

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

@Composable
fun TopProductsReportTab(viewModel: VetViewModel) {
    val topProducts by viewModel.topSellingProducts.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Productos y Servicios Más Vendidos", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        if (topProducts.isNotEmpty()) {
            val barChartData = BarChartData(
                chartData = topProducts.mapIndexed { index, product ->
                    BarData(
                        point = Point(index.toFloat(), product.totalSold.toFloat()),
                        label = product.name,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                xAxisData = AxisData.Builder()
                    .axisStepSize(60.dp)
                    .steps(topProducts.size -1)
                    .bottomPadding(40.dp)
                    .axisLabelAngle(30f)
                    .labelData { index -> topProducts.getOrNull(index)?.name ?: "" }
                    .build(),
                yAxisData = AxisData.Builder()
                    .steps(5)
                    .labelAndAxisLinePadding(20.dp)
                    .labelData { value -> "%.0f".format(value) }
                    .build(),
                barStyle = BarStyle(barWidth = 35.dp)
            )
            BarChart(modifier = Modifier.height(450.dp), barChartData = barChartData)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay datos de ventas para mostrar un ranking.")
            }
        }
    }
}

@Composable
fun DebtsReportTab(viewModel: VetViewModel) {
    val totalDebt by viewModel.totalDebt.collectAsState()
    val formattedDebt = String.format("₲ %,.0f", totalDebt ?: 0.0).replace(",", ".")

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SummaryCard(title = "Deuda Total Pendiente de Clientes", value = formattedDebt)
    }
}

@Composable
fun InventoryReportTab(viewModel: VetViewModel) {
    val totalValue by viewModel.totalInventoryValue.collectAsState()
    val formattedValue = String.format("₲ %,.0f", totalValue ?: 0.0).replace(",", ".")

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SummaryCard(title = "Valor Total del Inventario", value = formattedValue)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
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