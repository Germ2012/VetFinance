package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.STOCK_MOVEMENT_CONTAINER_OPEN
import com.example.vetfinance.data.STOCK_MOVEMENT_MANUAL_ADJUSTMENT
import com.example.vetfinance.data.STOCK_MOVEMENT_RESTOCK
import com.example.vetfinance.data.STOCK_MOVEMENT_SALE
import com.example.vetfinance.data.STOCK_MOVEMENT_SALE_REVERSAL
import com.example.vetfinance.data.StockMovement
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: VetViewModel) {
    val showDialog by viewModel.showAddProductDialog.collectAsState()
    val filter by viewModel.inventoryFilter.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productForCostHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockAdjustment by remember { mutableStateOf<Product?>(null) }
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val productCostHistory by viewModel.productCostHistory.collectAsState()
    val productStockMovements by viewModel.productStockMovements.collectAsState()

    val productsFilterText = stringResource(R.string.inventory_filter_products)
    val servicesFilterText = stringResource(R.string.inventory_filter_services)

    val filteredProducts = remember(inventory, filter, productsFilterText, servicesFilterText) {
        when (filter) {
            productsFilterText -> inventory.filter { !it.isService }
            servicesFilterText -> inventory.filter { it.isService }
            else -> inventory
        }
    }

    if (showDialog) {
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.insertOrUpdateProduct(newProduct)
                viewModel.onDismissAddProductDialog()
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            allProducts = inventory,
            suppliers = suppliers
        )
    }

    productToEdit?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = {
                productToEdit = null
                viewModel.clearProductNameSuggestions()
            },
            onConfirm = { updatedProduct ->
                viewModel.insertOrUpdateProduct(updatedProduct)
                productToEdit = null
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            allProducts = inventory,
            suppliers = suppliers
        )
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_delete_product_message, product.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    productForCostHistory?.let { product ->
        CostHistoryDialog(
            product = product,
            history = productCostHistory,
            onDismiss = { productForCostHistory = null }
        )
    }

    productForStockHistory?.let { product ->
        StockMovementHistoryDialog(
            product = product,
            movements = productStockMovements,
            onDismiss = { productForStockHistory = null }
        )
    }

    productForStockAdjustment?.let { product ->
        AdjustStockDialog(
            product = product,
            onDismiss = { productForStockAdjustment = null },
            onConfirm = { newStock, note ->
                viewModel.adjustProductStock(product, newStock, note)
                productForStockAdjustment = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddProductDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_product))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text(stringResource(R.string.tab_inventory), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            InventoryFilter(selectedFilter = filter, onFilterSelected = { viewModel.onInventoryFilterChanged(it) })
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredProducts, key = { it.productId }) { product ->
                        InventoryItem(
                            product = product,
                            onEdit = { productToEdit = it },
                            onDelete = { productToDelete = it },
                            onShowCostHistory = {
                                productForCostHistory = it
                                viewModel.loadProductCostHistory(it.productId)
                            },
                            onShowStockHistory = {
                                productForStockHistory = it
                                viewModel.loadProductStockMovements(it.productId)
                            },
                            onAdjustStock = { productForStockAdjustment = it },
                            onOpenContainer = { viewModel.openContainerForBulkSale(it) }
                        )
                    }
                    if (filteredProducts.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize().padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.inventory_no_products_matching_filter))
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryFilter(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf(
        stringResource(R.string.inventory_filter_all),
        stringResource(R.string.inventory_filter_products),
        stringResource(R.string.inventory_filter_services)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        filters.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                onClick = { onFilterSelected(label) },
                selected = selectedFilter == label
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun InventoryItem(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onShowCostHistory: (Product) -> Unit,
    onShowStockHistory: (Product) -> Unit,
    onAdjustStock: (Product) -> Unit,
    onOpenContainer: (Product) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (product.isService) stringResource(R.string.inventory_item_type_service)
                    else stringResource(R.string.inventory_item_type_product) + ": " + product.sellingMethod,
                    style = MaterialTheme.typography.bodySmall
                )
                product.category?.takeIf { it.isNotBlank() }?.let {
                    Text("Categoria: $it", style = MaterialTheme.typography.bodySmall)
                }
                if (product.isContainer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onOpenContainer(product) }, enabled = product.stock >= 1) {
                        Text("Abrir 1 para Venta a Granel")
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(product.price),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!product.isService) {
                    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
                    Text(
                        text = "Stock: ${formatCurrency(product.stock).replace(",00","")}$unit",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(modifier = Modifier.align(Alignment.Top)) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_content_description))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.inventory_item_menu_edit)) },
                        onClick = {
                            onEdit(product)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.inventory_item_menu_delete)) },
                        onClick = {
                            onDelete(product)
                            expanded = false
                        }
                    )
                    if (!product.isService) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.inventory_item_menu_cost_history)) },
                            onClick = {
                                onShowCostHistory(product)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Historial de stock") },
                            onClick = {
                                onShowStockHistory(product)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajustar stock") },
                            onClick = {
                                onAdjustStock(product)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockMovementHistoryDialog(
    product: Product,
    movements: List<StockMovement>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock de ${product.name}") },
        text = {
            if (movements.isEmpty()) {
                Text("Todavia no hay movimientos de stock para este producto.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(movements, key = { it.movementId }) { movement ->
                        Column {
                            Text(stockMovementLabel(movement.movementType), fontWeight = FontWeight.Bold)
                            Text("${sdf.format(Date(movement.movementDate))} - Cambio: ${movement.quantityChange} - Stock: ${movement.stockAfter}")
                            movement.unitCost?.let { Text("Costo registrado: Gs. ${formatCurrency(it)}") }
                            movement.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accept_button)) }
        }
    )
}

private fun stockMovementLabel(type: String): String {
    return when (type) {
        STOCK_MOVEMENT_SALE -> "Venta"
        STOCK_MOVEMENT_SALE_REVERSAL -> "Venta anulada"
        STOCK_MOVEMENT_RESTOCK -> "Reabastecimiento"
        STOCK_MOVEMENT_CONTAINER_OPEN -> "Apertura de contenedor"
        STOCK_MOVEMENT_MANUAL_ADJUSTMENT -> "Ajuste manual"
        else -> type
    }
}

@Composable
fun AdjustStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var stock by remember(product) { mutableStateOf(product.stock.toString()) }
    var note by remember(product) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar stock de ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() || char == '.' }
                        if (filtered.count { char -> char == '.' } <= 1) stock = filtered
                    },
                    label = { Text("Nuevo stock") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Motivo obligatorio") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = note.isBlank()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(stock.toDoubleOrNull() ?: product.stock, note) },
                enabled = note.isNotBlank() && (stock.toDoubleOrNull() ?: -1.0) >= 0.0
            ) { Text(stringResource(R.string.save_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

@Composable
fun CostHistoryDialog(
    product: Product,
    history: List<com.example.vetfinance.data.ProductCostHistoryItem>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cost_history_title, product.name)) },
        text = {
            if (history.isEmpty()) {
                Text(stringResource(R.string.cost_history_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history) { item ->
                        Column {
                            Text(item.supplierName ?: stringResource(R.string.label_no_supplier), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.cost_history_item, sdf.format(Date(item.orderDate)), formatCurrency(item.costAtTime), item.quantity.toString()))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accept_button)) }
        }
    )
}
