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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.SaleWithProducts
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
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
    val isLoading by viewModel.isLoading.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var saleToDelete by remember { mutableStateOf<SaleWithProducts?>(null) }

    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_deletion_sale_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        saleToDelete?.let { viewModel.deleteSale(it) }
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
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
                ) { Text(stringResource(R.string.accept_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) }
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.register_sale_fab))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
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

            if (isLoading && filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (filteredSales.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val message = if (selectedDate != null) {
                            stringResource(R.string.no_sales_for_date)
                        } else {
                            stringResource(R.string.no_sales_recorded)
                        }
                        Text(message)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSales, key = { it.sale.saleId }) { saleWithProducts ->
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
                    text = stringResource(R.string.sale_item_title, saleWithProducts.sale.saleId.take(8).uppercase()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_content_description))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_button)) },
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
                    val product = saleWithProducts.products.find { it.productId == crossRef.productId }
                    val quantityFormatted = if (crossRef.quantitySold % 1.0 == 0.0) {
                        crossRef.quantitySold.toInt().toString()
                    } else {
                        String.format(Locale.getDefault(), "%.2f", crossRef.quantitySold)
                    }
                    Text(stringResource(
                        R.string.sale_item_product_detail,
                        quantityFormatted,
                        product?.name ?: stringResource(R.string.unknown_product),
                        formatCurrency(crossRef.priceAtTimeOfSale))
                    )
                }
            } else {
                Text(stringResource(R.string.manual_sale_no_details), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.total_amount_label, formatCurrency(saleWithProducts.sale.totalAmount)),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}