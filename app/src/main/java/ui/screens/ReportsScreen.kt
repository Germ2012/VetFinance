// ruta: app/src/main/java/com/example/vetfinance/ui/screens/ReportsScreen.kt

package com.example.vetfinance.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vetfinance.viewmodel.Period
import com.example.vetfinance.viewmodel.VetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: VetViewModel) {
    var selectedPeriod by remember { mutableStateOf(Period.DAY) }
    val summary = viewModel.getSalesSummary(selectedPeriod)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launcher para IMPORTAR (seleccionar un archivo ZIP)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val resultado = viewModel.importarDatosDesdeZIP(it, context)
                    Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Launcher para EXPORTAR (crear un archivo ZIP)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val csvDataMap = viewModel.exportarDatosCompletos()
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            ZipOutputStream(outputStream).use { zos ->
                                csvDataMap.forEach { (fileName, content) ->
                                    val entry = ZipEntry(fileName)
                                    zos.putNextEntry(entry)
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
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Reportes y Copias de Seguridad", style = MaterialTheme.typography.headlineMedium)

        SegmentedControl(
            selected = selectedPeriod,
            onPeriodSelected = { newPeriod -> selectedPeriod = newPeriod }
        )

        val formattedSummary = String.format("₲ %,.0f", summary).replace(",", ".")
        SummaryCard(
            title = "Total Ventas (${selectedPeriod.displayName})",
            value = formattedSummary
        )

        Spacer(modifier = Modifier.weight(1f)) // Empuja los botones hacia abajo

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botón para importar datos
            Button(onClick = {
                importLauncher.launch(arrayOf("application/zip"))
            }) {
                Text("Importar Datos")
            }

            // Botón para exportar datos
            Button(onClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("backup_vetfinance_$timestamp.zip")
            }) {
                Text("Exportar Datos")
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