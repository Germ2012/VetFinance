package com.example.vetfinance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.STOCK_MOVEMENT_CONTAINER_OPEN
import com.example.vetfinance.data.STOCK_MOVEMENT_MANUAL_ADJUSTMENT
import com.example.vetfinance.data.STOCK_MOVEMENT_RESTOCK
import com.example.vetfinance.data.STOCK_MOVEMENT_SALE
import com.example.vetfinance.data.STOCK_MOVEMENT_SALE_REVERSAL
import com.example.vetfinance.data.StockMovement
import com.example.vetfinance.ui.components.HighVolumeModeToggle
import com.example.vetfinance.ui.components.SkeletonLine
import com.example.vetfinance.viewmodel.InventoryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vetfinance.ui.utils.formatCurrency
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.inventoryUiState.collectAsStateWithLifecycle()
    val showDialog = uiState.showAddProductDialog
    val filter = uiState.filter
    val searchQuery = uiState.searchQuery
    val suppliers = uiState.suppliers
    val appSettings = uiState.appSettings
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productForActions by remember { mutableStateOf<Product?>(null) }
    var secureProductToDelete by remember { mutableStateOf<Product?>(null) }
    var productForCostHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockHistory by remember { mutableStateOf<Product?>(null) }
    var productForStockAdjustment by remember { mutableStateOf<Product?>(null) }
    var secureStockProduct by remember { mutableStateOf<Product?>(null) }
    var secureStockValue by remember { mutableStateOf<Double?>(null) }
    var secureStockNote by remember { mutableStateOf("") }
    var showLowStockSheet by remember { mutableStateOf(false) }
    val productNameSuggestions = uiState.productNameSuggestions
    val isLoading = uiState.isLoading
    val productCostHistory by viewModel.productCostHistory.collectAsStateWithLifecycle()
    val productStockMovements by viewModel.productStockMovements.collectAsStateWithLifecycle()
    val pagedProducts = viewModel.inventoryPaginated.collectAsLazyPagingItems()
    LaunchedEffect(pagedProducts) {
        viewModel.pagingRefreshEvents.collect {
            pagedProducts.refresh()
        }
    }
    val dialogProductsState = if (showDialog || productToEdit != null) {
        viewModel.inventory.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf<List<Product>>(emptyList()) }
    }
    val dialogProducts = dialogProductsState.value
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inventoryListState = rememberLazyListState()
    var highVolumeMode by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(inventoryListState) {
        snapshotFlow { inventoryListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
            }
    }
    val haptic = LocalHapticFeedback.current

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
            allProducts = dialogProducts,
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
            allProducts = dialogProducts,
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
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        secureProductToDelete = product
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

    secureProductToDelete?.let { product ->
        SecurityPinDialog(
            settings = appSettings,
            actionLabel = "eliminar producto",
            onDismiss = { secureProductToDelete = null },
            onAuthorized = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteProduct(product)
                secureProductToDelete = null
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
                secureStockProduct = product
                secureStockValue = newStock
                secureStockNote = note
                productForStockAdjustment = null
            }
        )
    }

    val stockProduct = secureStockProduct
    val stockValue = secureStockValue
    if (stockProduct != null && stockValue != null) {
        SecurityPinDialog(
            settings = appSettings,
            actionLabel = "ajustar stock",
            onDismiss = {
                secureStockProduct = null
                secureStockValue = null
                secureStockNote = ""
            },
            onAuthorized = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.adjustProductStock(stockProduct, stockValue, secureStockNote)
                secureStockProduct = null
                secureStockValue = null
                secureStockNote = ""
            }
        )
    }

    productForActions?.let { product ->
        InventoryActionsSheet(
            product = product,
            onDismiss = { productForActions = null },
            onEdit = {
                productToEdit = product
                productForActions = null
            },
            onDelete = {
                productToDelete = product
                productForActions = null
            },
            onShowCostHistory = {
                productForCostHistory = product
                viewModel.loadProductCostHistory(product.productId)
                productForActions = null
            },
            onShowStockHistory = {
                productForStockHistory = product
                viewModel.loadProductStockMovements(product.productId)
                productForActions = null
            },
            onAdjustStock = {
                productForStockAdjustment = product
                productForActions = null
            },
            onOpenContainer = {
                viewModel.openContainerForBulkSale(product)
                productForActions = null
            }
        )
    }

    if (showLowStockSheet) {
        val lowStockProducts by viewModel.lowStockProductsByName.collectAsStateWithLifecycle()
        LowStockSheet(
            products = lowStockProducts,
            onDismiss = { showLowStockSheet = false },
            onAdjustStock = {
                productForStockAdjustment = it
                showLowStockSheet = false
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
                productCount = uiState.productCount,
                serviceCount = uiState.serviceCount,
                lowStockCount = uiState.lowStockCount
            )
            AnimatedVisibility(visible = uiState.lowStockCount > 0) {
                InventoryAlertBanner(
                    lowStockCount = uiState.lowStockCount,
                    onOpen = { showLowStockSheet = true }
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                ),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            InventoryFilter(selectedFilter = filter, onFilterSelected = { viewModel.onInventoryFilterChanged(it) })
            InventoryListHeader(
                showingCount = pagedProducts.itemCount,
                totalCount = uiState.totalCount,
                filter = filter,
                highVolumeMode = highVolumeMode,
                onHighVolumeModeChange = { highVolumeMode = it }
            )
            Spacer(modifier = Modifier.height(12.dp))

            if ((isLoading && uiState.totalCount == 0) || pagedProducts.loadState.refresh is LoadState.Loading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(6, contentType = { "inventory-skeleton" }) {
                        InventoryItemPlaceholder()
                    }
                }
            } else {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    LazyColumn(
                        state = inventoryListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(
                            count = pagedProducts.itemCount,
                            key = pagedProducts.itemKey { it.productId },
                            contentType = { index ->
                                pagedProducts[index]?.let {
                                    val type = if (it.isService) "service" else "product"
                                    if (highVolumeMode) "dense-$type" else type
                                } ?: "placeholder"
                            }
                        ) { index ->
                            val product = pagedProducts[index]
                            if (product != null) {
                                InventoryItem(
                                    product = product,
                                    highVolumeMode = highVolumeMode,
                                    onOpen = { productForActions = it },
                                    onShowActions = { productForActions = it }
                                )
                            } else {
                                InventoryItemPlaceholder(highVolumeMode = highVolumeMode)
                            }
                        }
                        if (pagedProducts.itemCount == 0 && !isLoading && pagedProducts.loadState.refresh !is LoadState.Loading) {
                            item {
                                InventoryEmptyState(
                                    message = stringResource(R.string.inventory_no_products_matching_filter),
                                    onAddClick = { viewModel.onShowAddProductDialog() }
                                )
                            }
                        }
                        if (pagedProducts.loadState.append is LoadState.Loading) {
                            item {
                                InventoryItemPlaceholder(highVolumeMode = highVolumeMode)
                            }
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
        shape = MaterialTheme.shapes.medium,
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
    lowStockCount: Int,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            TextButton(onClick = onOpen) {
                Text("Ver lista")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LowStockSheet(
    products: List<Product>,
    onDismiss: () -> Unit,
    onAdjustStock: (Product) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Alertas de stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${products.size} productos ordenados por nombre",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = products,
                    key = { it.productId },
                    contentType = { "low-stock-row" }
                ) { product ->
                    LowStockProductRow(product = product, onAdjustStock = onAdjustStock)
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
    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Ajustar")
        }
    }
}

@Composable
private fun InventoryListHeader(
    showingCount: Int,
    totalCount: Int,
    filter: String,
    highVolumeMode: Boolean,
    onHighVolumeModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        HighVolumeModeToggle(enabled = highVolumeMode, onEnabledChange = onHighVolumeModeChange)
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
        shape = MaterialTheme.shapes.medium,
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
    highVolumeMode: Boolean,
    onOpen: (Product) -> Unit,
    onShowActions: (Product) -> Unit
) {
    val isLowStock = remember(product.stock, product.lowStockThreshold) {
        product.lowStockThreshold?.let { threshold ->
            threshold > 0 && product.stock < threshold
        } == true
    }
    val unit = remember(product.unitMeasure) {
        product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    }
    val typeText = remember(product.isService, product.isContainer, product.sellingMethod) {
        when {
            product.isService -> "Servicio"
            product.isContainer -> "Contenedor"
            else -> product.sellingMethod
        }
    }
    val stockLabel = remember(product.isService, product.stock, unit) {
        if (product.isService) null else "Stock ${formatCurrency(product.stock).replace(",00", "")}$unit"
    }
    val detailText = remember(typeText, product.category, stockLabel) {
        listOfNotNull(typeText, product.category?.takeIf { it.isNotBlank() }, stockLabel).joinToString(" | ")
    }
    val priceText = remember(product.price) {
        "Gs. ${formatCurrency(product.price)}"
    }
    val detailColor = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val rowInteractionSource = remember { MutableInteractionSource() }
    val actionInteractionSource = remember { MutableInteractionSource() }
    val compactMetric = remember(product.isService, product.stock, product.price, unit) {
        if (product.isService) priceText else stockLabel ?: priceText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (highVolumeMode) 48.dp else 64.dp)
            .clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = { onOpen(product) }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (highVolumeMode) 1.dp else 3.dp)
        ) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                style = if (highVolumeMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!highVolumeMode) {
                Text(
                    detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = detailColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = if (highVolumeMode) compactMetric else priceText,
            style = if (highVolumeMode) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = if (highVolumeMode && isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        if (!highVolumeMode) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = actionInteractionSource,
                        indication = null,
                        onClick = { onShowActions(product) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_content_description))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryActionsSheet(
    product: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowCostHistory: () -> Unit,
    onShowStockHistory: () -> Unit,
    onAdjustStock: () -> Unit,
    onOpenContainer: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    val stockText = if (product.isService) {
                        stringResource(R.string.inventory_item_type_service)
                    } else {
                        "Stock ${formatCurrency(product.stock).replace(",00", "")} | ${product.sellingMethod}"
                    }
                    Text(stockText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )
            HorizontalDivider()
            InventoryActionRow(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.inventory_item_menu_edit),
                onClick = onEdit
            )
            InventoryActionRow(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.inventory_item_menu_delete),
                isDestructive = true,
                onClick = onDelete
            )
            if (!product.isService) {
                InventoryActionRow(
                    icon = Icons.Default.History,
                    label = stringResource(R.string.inventory_item_menu_cost_history),
                    onClick = onShowCostHistory
                )
                InventoryActionRow(
                    icon = Icons.Default.History,
                    label = "Historial de stock",
                    onClick = onShowStockHistory
                )
                if (product.isContainer) {
                    InventoryActionRow(
                        icon = Icons.Default.AddShoppingCart,
                        label = "Abrir 1 para venta a granel",
                        enabled = product.stock >= 1,
                        onClick = onOpenContainer
                    )
                }
                InventoryActionRow(
                    icon = Icons.Default.Inventory,
                    label = "Ajustar stock",
                    onClick = onAdjustStock
                )
            }
        }
    }
}

@Composable
private fun InventoryActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    isDestructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        },
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier
    )
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
        shape = MaterialTheme.shapes.medium,
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
            Button(onClick = onAddClick, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.content_description_add_product))
            }
        }
    }
}

@Composable
private fun InventoryItemPlaceholder(highVolumeMode: Boolean = false) {
    if (highVolumeMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonLine(modifier = Modifier.weight(1f), height = 10.dp)
            SkeletonLine(modifier = Modifier.width(72.dp), height = 10.dp)
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.58f), height = 10.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.34f), height = 10.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.22f), height = 8.dp)
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
                    items(
                        items = movements,
                        key = { it.movementId },
                        contentType = { "stock-movement" }
                    ) { movement ->
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
                    items(
                        items = history,
                        key = { historyItem -> "${historyItem.orderDate}-${historyItem.supplierName}-${historyItem.costAtTime}-${historyItem.quantity}" },
                        contentType = { "cost-history" }
                    ) { item ->
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
