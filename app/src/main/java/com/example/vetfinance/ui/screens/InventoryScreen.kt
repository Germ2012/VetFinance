package com.example.vetfinance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productForCostHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockAdjustment by remember { mutableStateOf<Product?>(null) }
    var lowStockAlertExpanded by remember { mutableStateOf(false) }
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val productCostHistory by viewModel.productCostHistory.collectAsState()
    val productStockMovements by viewModel.productStockMovements.collectAsState()

    val productsFilterText = stringResource(R.string.inventory_filter_products)
    val servicesFilterText = stringResource(R.string.inventory_filter_services)

    val filteredProducts = remember(inventory, filter, searchQuery, productsFilterText, servicesFilterText) {
        val byType = when (filter) {
            productsFilterText -> inventory.filter { !it.isService }
            servicesFilterText -> inventory.filter { it.isService }
            else -> inventory
        }
        if (searchQuery.isBlank()) {
            byType
        } else {
            byType.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    product.supplierIdFk.orEmpty().contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val productCount = remember(inventory) { inventory.count { !it.isService } }
    val serviceCount = remember(inventory) { inventory.count { it.isService } }

    LaunchedEffect(lowStockProducts.isEmpty()) {
        if (lowStockProducts.isEmpty()) lowStockAlertExpanded = false
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            InventoryHeader(
                productCount = productCount,
                serviceCount = serviceCount,
                lowStockCount = lowStockProducts.size
            )
            AnimatedVisibility(visible = lowStockProducts.isNotEmpty()) {
                InventoryAlertBanner(
                    lowStockProducts = lowStockProducts,
                    expanded = lowStockAlertExpanded,
                    onToggle = { lowStockAlertExpanded = !lowStockAlertExpanded },
                    onAdjustStock = { productForStockAdjustment = it }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onProductSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_product_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.clearProductSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search_content_description))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InventoryFilter(selectedFilter = filter, onFilterSelected = { viewModel.onInventoryFilterChanged(it) })
            InventoryListHeader(
                showingCount = filteredProducts.size,
                totalCount = inventory.size,
                filter = filter
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
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
                            InventoryEmptyState(
                                message = stringResource(R.string.inventory_no_products_matching_filter),
                                onAddClick = { viewModel.onShowAddProductDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(
    productCount: Int,
    serviceCount: Int,
    lowStockCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.tab_inventory),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Busca, filtra y corrige stock desde una sola vista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            InventoryOverview(
                productCount = productCount,
                serviceCount = serviceCount,
                lowStockCount = lowStockCount
            )
        }
    }
}

@Composable
private fun InventoryAlertBanner(
    lowStockProducts: List<Product>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdjustStock: (Product) -> Unit
) {
    val lowStockCount = lowStockProducts.size
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .animateContentSize()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stock bajo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "$lowStockCount productos necesitan reposicion o ajuste.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                    lowStockProducts
                        .sortedWith(
                            compareBy<Product> { product ->
                                val threshold = product.lowStockThreshold ?: 1.0
                                if (threshold > 0) product.stock / threshold else product.stock
                            }.thenBy { it.name.lowercase(Locale.getDefault()) }
                        )
                        .take(8)
                        .forEach { product ->
                            LowStockProductRow(product = product, onAdjustStock = onAdjustStock)
                        }
                    if (lowStockProducts.size > 8) {
                        Text(
                            text = "Mostrando 8 de $lowStockCount alertas. Usa el filtro de inventario para ver el resto.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockProductRow(
    product: Product,
    onAdjustStock: (Product) -> Unit
) {
    val threshold = product.lowStockThreshold ?: 0.0
    val stockRatio = if (threshold > 0) (product.stock / threshold).coerceIn(0.0, 1.0).toFloat() else 0f
    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Stock ${formatCurrency(product.stock).replace(",00", "")}$unit de minimo ${formatCurrency(threshold).replace(",00", "")}$unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = { onAdjustStock(product) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Ajustar")
                }
            }
            LinearProgressIndicator(
                progress = { stockRatio },
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
private fun InventoryListHeader(
    showingCount: Int,
    totalCount: Int,
    filter: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("Vista: $filter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text("$showingCount de $totalCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InventoryOverview(
    productCount: Int,
    serviceCount: Int,
    lowStockCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InventoryMetric(
            modifier = Modifier.weight(1f),
            label = "Productos",
            value = productCount.toString(),
            icon = Icons.Default.Inventory
        )
        InventoryMetric(
            modifier = Modifier.weight(1f),
            label = "Servicios",
            value = serviceCount.toString(),
            icon = Icons.Default.AddShoppingCart
        )
        InventoryMetric(
            modifier = Modifier.weight(1f),
            label = "Alertas",
            value = lowStockCount.toString(),
            icon = Icons.Default.WarningAmber,
            isWarning = lowStockCount > 0
        )
    }
}

@Composable
private fun InventoryMetric(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isWarning: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    val stockReference = (product.lowStockThreshold ?: product.stock.coerceAtLeast(1.0)).coerceAtLeast(1.0)
    val stockRatio = (product.stock / stockReference).coerceIn(0.0, 1.0).toFloat()
    val isLowStock = product.lowStockThreshold?.let { threshold ->
        threshold > 0 && product.stock < threshold
    } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onEdit(product) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (product.isService) stringResource(R.string.inventory_item_type_service)
                                else stringResource(R.string.inventory_item_type_product)
                            )
                        }
                    )
                    if (!product.isService) {
                        AssistChip(onClick = {}, label = { Text(product.sellingMethod) })
                    }
                }
                product.category?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "Categor\u00eda: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (product.isContainer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { onOpenContainer(product) },
                        enabled = product.stock >= 1,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
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
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { stockRatio },
                        modifier = Modifier
                            .width(112.dp)
                            .height(6.dp),
                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (isLowStock) {
                        Text(
                            text = "Bajo stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
private fun InventoryEmptyState(
    message: String,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onAddClick, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.content_description_add_product))
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
    var showUnsavedChangesDialog by remember(product) { mutableStateOf(false) }
    val hasUnsavedChanges = stock != product.stock.toString() || note.isNotBlank()
    val requestDismiss: () -> Unit = {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = requestDismiss,
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
            TextButton(onClick = requestDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Cambios sin guardar") },
            text = { Text("Hay un ajuste de stock en edicion. Si sales ahora, se perderan los cambios.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Salir sin guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("Seguir editando")
                }
            }
        )
    }
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
