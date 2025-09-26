// ruta: app/src/main/java/com/example/vetfinance/ui/screens/SalesScreen.kt

package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.SaleWithProducts
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: VetViewModel, navController: NavController) {
    val filteredSales by viewModel.filteredSales.collectAsState()
    val selectedDate by viewModel.selectedSaleDateFilter.collectAsState()
    val isLoading = filteredSales.isEmpty() && selectedDate == null // Consider loading only if no filter and no sales

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var saleToDelete by remember { mutableStateOf<SaleWithProducts?>(null) }

    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar esta venta? Esta acción no se puede deshacer y el stock de los productos será restaurado.") },
            confirmButton = {
                Button(
                    onClick = {
                        saleToDelete?.let { viewModel.deleteSale(it) }
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onSaleDateFilterSelected(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.AddSale.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Venta")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Historial de Ventas", style = MaterialTheme.typography.headlineMedium)

                FilterChip(
                    selected = selectedDate != null,
                    onClick = { showDatePicker = true },
                    label = {
                        val labelText = if (selectedDate != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            "Fecha: ${sdf.format(Date(selectedDate!!))}"
                        } else {
                            "Filtrar por Fecha"
                        }
                        Text(labelText)
                    },
                    trailingIcon = {
                        if (selectedDate != null) {
                            IconButton(onClick = { viewModel.clearSaleDateFilter() }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar filtro")
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (filteredSales.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = if (selectedDate != null) {
                            "No se encontraron ventas para esta fecha."
                        } else {
                            "No se han registrado ventas."
                        }
                        Text(message)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSales) { saleWithProducts ->
                            SaleItem(
                                saleWithProducts = saleWithProducts,
                                onDeleteClick = { saleToDelete = saleWithProducts }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItem(saleWithProducts: SaleWithProducts, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val saleDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(saleWithProducts.sale.date),
                ZoneId.systemDefault()
            )
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Venta #${saleWithProducts.sale.saleId.take(8)}...",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                onDeleteClick()
                                expanded = false
                            }
                        )
                    }
                }
            }
            Text(
                text = saleDateTime.format(formatter),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (saleWithProducts.crossRefs.isNotEmpty()) {
                saleWithProducts.crossRefs.forEach { crossRef ->
                    val product = saleWithProducts.products.find { it.id == crossRef.productId }
                    Text("• ${formatCurrency(crossRef.quantity)}x ${product?.name ?: "Producto desconocido"} (Gs. ${formatCurrency(crossRef.priceAtTimeOfSale)})")
                }
            } else {
                Text("Venta manual o sin productos detallados.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: Gs. ${formatCurrency(saleWithProducts.sale.totalAmount)}", // Usar formatCurrency y prefijo Gs.
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}