package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SaleProductCrossRef
import com.example.vetfinance.data.SaleWithProducts
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: VetViewModel, navController: NavController) {
    val filteredSales by viewModel.filteredSales.collectAsState()
    val selectedDate by viewModel.selectedSaleDateFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var saleToDelete by remember { mutableStateOf<SaleWithProducts?>(null) }

    // --- DIÁLOGOS ---
    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_delete_sale_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        saleToDelete?.let { viewModel.deleteSale(it) }
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            // --- INICIO DE LA CORRECCIÓN ---
                            // Ajusta la marca de tiempo UTC a la zona horaria del dispositivo
                            val tz = TimeZone.getDefault()
                            val offset = tz.getOffset(selectedMillis)
                            val adjustedMillis = selectedMillis + offset
                            viewModel.onSaleDateFilterSelected(adjustedMillis)
                            // --- FIN DE LA CORRECCIÓN ---
                        } else {
                            viewModel.onSaleDateFilterSelected(null)
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- PANTALLA PRINCIPAL ---
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddSale.route) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.register_sale_fab))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // --- BARRA DE FILTRO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.sales_history_title), style = MaterialTheme.typography.headlineMedium)
                FilterChip(
                    selected = selectedDate != null,
                    onClick = { showDatePicker = true },
                    label = {
                        val labelText = if (selectedDate != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            stringResource(R.string.date_filter_label, sdf.format(Date(selectedDate!!)))
                        } else {
                            stringResource(R.string.filter_by_date_chip)
                        }
                        Text(labelText)
                    },
                    trailingIcon = {
                        if (selectedDate != null) {
                            IconButton(onClick = { viewModel.clearSaleDateFilter() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_filter_content_description))
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- LISTA DE VENTAS ---
            if (isLoading && filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val message = if (selectedDate != null) stringResource(R.string.no_sales_for_date)
                    else stringResource(R.string.no_sales_recorded)
                    Text(message)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredSales, key = { it.sale.saleId }) { sale ->
                        SaleItem(
                            saleWithProducts = sale,
                            onDelete = { saleToDelete = sale }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItem(
    saleWithProducts: SaleWithProducts,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- ENCABEZADO DE LA TARJETA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(saleWithProducts.sale.date)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_description_delete_sale))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // --- DETALLES DE PRODUCTOS/SERVICIOS ---
            if (saleWithProducts.crossRefs.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_details_available),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            } else {
                saleWithProducts.crossRefs.forEach { detail ->
                    val product = saleWithProducts.products.find { it.productId == detail.productId }
                    if (product != null) {
                        SaleDetailItem(saleDetail = detail, product = product)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- TOTAL DE LA VENTA ---
            Text(
                text = stringResource(R.string.sale_item_total_prefix, formatCurrency(saleWithProducts.sale.totalAmount)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun SaleDetailItem(saleDetail: SaleProductCrossRef, product: Product) {
    val isDoseSale = saleDetail.overridePrice != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)

            // Lógica para mostrar las notas de cada ítem (servicio/dosis)
            if (isDoseSale && !saleDetail.notes.isNullOrBlank()) {
                Text(
                    text = "\"${saleDetail.notes}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }

            // Muestra la cantidad si no es una venta por dosis con precio manual
            if (!isDoseSale) {
                Text(
                    text = stringResource(
                        R.string.sale_detail_item_quantity_prefix,
                        formatCurrency(saleDetail.quantitySold).removeSuffix(".00").removeSuffix(",00")
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val priceToShow = saleDetail.overridePrice ?: (saleDetail.priceAtTimeOfSale * saleDetail.quantitySold)
        Text(
            text = formatCurrency(priceToShow),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
