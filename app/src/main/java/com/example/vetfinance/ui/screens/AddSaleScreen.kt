package com.example.vetfinance.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.vetfinance.R
import com.example.vetfinance.data.CartItem
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_BY_WEIGHT_OR_AMOUNT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import com.example.vetfinance.viewmodel.SaleUiEvent
import com.example.vetfinance.viewmodel.SaleViewModel
import com.example.vetfinance.ui.components.SkeletonLine
import com.example.vetfinance.ui.utils.NumberTransformation
import com.example.vetfinance.ui.utils.formatCurrency
import java.util.Locale
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(
    navController: NavHostController,
    saleViewModel: SaleViewModel = hiltViewModel()
) {
    val saleUiState by saleViewModel.uiState.collectAsStateWithLifecycle()
    val cart = saleUiState.cart
    val total = saleUiState.total
    val showAddProductDialog by saleViewModel.showAddProductDialog.collectAsStateWithLifecycle()
    val saleInventoryItems = saleViewModel.saleInventoryPaginated.collectAsLazyPagingItems()
    LaunchedEffect(saleInventoryItems) {
        saleViewModel.pagingRefreshEvents.collect {
            saleInventoryItems.refresh()
        }
    }
    val suppliers by saleViewModel.suppliers.collectAsStateWithLifecycle()
    val clients by saleViewModel.clients.collectAsStateWithLifecycle()
    val searchQuery by saleViewModel.productSearchQuery.collectAsStateWithLifecycle()
    val saleFilter by saleViewModel.saleInventoryFilter.collectAsStateWithLifecycle()
    val productNameSuggestions by saleViewModel.productNameSuggestions.collectAsStateWithLifecycle()
    val containedProductSuggestions by saleViewModel.containedProductSuggestions.collectAsStateWithLifecycle()
    val selectedContainedProduct by saleViewModel.selectedContainedProduct.collectAsStateWithLifecycle()
    val clientNameSuggestions by saleViewModel.clientNameSuggestions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var saleClientName by remember { mutableStateOf("") }
    var selectedSaleClientId by remember { mutableStateOf<String?>(null) }
    var showExitCartDialog by remember { mutableStateOf(false) }
    var cartItemToEditPrice by remember { mutableStateOf<CartItem?>(null) }

    val showFractionalDialog = saleUiState.showFractionalSaleDialog
    val productForFractionalSale = saleUiState.productForFractionalSale
    val showDoseDialog = saleUiState.showDoseSaleDialog
    val productForDoseSale = saleUiState.productForDoseSale
    val saleTypeDialogProduct = saleUiState.saleTypeDialogProduct
    val saleTypeBulkProduct by saleViewModel.saleTypeBulkProduct.collectAsStateWithLifecycle()

    LaunchedEffect(saleViewModel) {
        saleViewModel.events.collect { event ->
            when (event) {
                is SaleUiEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is SaleUiEvent.SaleFinished -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saleViewModel.clearProductSearchQuery()
            saleViewModel.clearSaleInventoryFilter()
            saleViewModel.dismissFractionalSaleDialog()
            saleViewModel.dismissDoseSaleDialog()
            saleViewModel.clearClientNameSuggestions()
            saleViewModel.clearContainedProductSelection()
        }
    }

    val requestExit: () -> Unit = {
        if (cart.isNotEmpty()) {
            showExitCartDialog = true
        } else {
            navController.popBackStack()
        }
    }

    BackHandler(enabled = cart.isNotEmpty()) {
        showExitCartDialog = true
    }

    if (showExitCartDialog) {
        AlertDialog(
            onDismissRequest = { showExitCartDialog = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Carrito activo") },
            text = { Text("Hay productos cargados. Podes guardar temporalmente el carrito y volver luego, o salir limpiandolo.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            saleViewModel.clearCart()
                            showExitCartDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Salir sin guardar")
                    }
                    Button(
                        onClick = {
                            showExitCartDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Guardar y salir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitCartDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    cartItemToEditPrice?.let { cartItem ->
        EditCartItemPriceDialog(
            cartItem = cartItem,
            onDismiss = { cartItemToEditPrice = null },
            onConfirm = { finalPrice, reason ->
                saleViewModel.updateCartItemPrice(cartItem, finalPrice, reason)
                cartItemToEditPrice = null
            }
        )
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            onDismiss = { saleViewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                saleViewModel.insertOrUpdateProduct(newProduct)
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { saleViewModel.onProductNameChange(it) },
            containedProductSuggestions = containedProductSuggestions,
            selectedContainedProduct = selectedContainedProduct,
            onContainedProductSearchChange = { saleViewModel.onContainedProductSearchChange(it) },
            onContainedProductSelected = { saleViewModel.onContainedProductSelected(it) },
            suppliers = suppliers
        )
    }

    val currentProductForFractionalSale = productForFractionalSale
    if (showFractionalDialog && currentProductForFractionalSale != null) {
        FractionalSaleDialog(
            product = currentProductForFractionalSale,
            onDismiss = { saleViewModel.dismissFractionalSaleDialog() },
            onConfirm = { product, quantity ->
                saleViewModel.addOrUpdateProductInCart(product, quantity)
                saleViewModel.dismissFractionalSaleDialog()
            }
        )
    }

    val currentProductForDoseSale = productForDoseSale
    if (showDoseDialog && currentProductForDoseSale != null) {
        DoseSaleDialog(
            product = currentProductForDoseSale,
            onDismiss = { saleViewModel.dismissDoseSaleDialog() },
            onConfirm = { product, notes, price ->
                saleViewModel.addOrUpdateDoseInCart(product, notes ?: "", price ?: product.price)
                saleViewModel.dismissDoseSaleDialog()
            }
        )
    }

    saleTypeDialogProduct?.let { product ->
        SaleTypeDialog(
            product = product,
            bulkProduct = saleTypeBulkProduct,
            onDismiss = { saleViewModel.closeSaleTypeDialog() },
            onSellByUnit = {
                saleViewModel.addToCart(product)
                saleViewModel.closeSaleTypeDialog()
            },
            onSellBulk = { bulkProduct ->
                saleViewModel.openFractionalSaleDialog(bulkProduct)
                saleViewModel.closeSaleTypeDialog()
            }
        )
    }

    val selectedClientPhone = remember(clients, selectedSaleClientId) {
        selectedSaleClientId?.let { clientId -> clients.find { it.clientId == clientId }?.phone }
    }
    val cartQuantityByProductId = remember(cart) {
        cart.groupBy { it.product.productId }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    }
    val lastCartItemByProductId = remember(cart) {
        cart.asReversed().associateBy { it.product.productId }
    }
    val cartUnits = remember(cart) { cart.sumOf { it.quantity } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_sale_title)) },
                navigationIcon = {
                    IconButton(onClick = requestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    IconButton(onClick = { saleViewModel.onShowAddProductDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_new_product))
                    }
                }
            )
        },
        bottomBar = {
            SaleCheckoutBar(
                total = total,
                itemCount = cartUnits,
                enabled = cart.isNotEmpty() && !saleUiState.isFinalizing,
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    saleViewModel.finalizeSale(
                        clientName = saleClientName,
                        selectedClientId = selectedSaleClientId
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            SaleCustomerPanel(
                clientName = saleClientName,
                selectedClientPhone = selectedClientPhone,
                onClientNameChange = {
                    saleClientName = it
                    selectedSaleClientId = null
                    saleViewModel.onClientNameChange(it)
                }
            )

            AnimatedVisibility(visible = clientNameSuggestions.isNotEmpty() && saleClientName.isNotBlank()) {
                ClientSuggestionPanel(
                    suggestions = clientNameSuggestions,
                    onClientSelected = { client ->
                        saleClientName = client.name
                        selectedSaleClientId = client.clientId
                        saleViewModel.clearClientNameSuggestions()
                    }
                )
            }

            SaleSearchPanel(
                searchQuery = searchQuery,
                onSearchChange = { saleViewModel.onProductSearchQueryChange(it) },
                onClearSearch = { saleViewModel.clearProductSearchQuery() }
            )

            AnimatedVisibility(visible = cart.isNotEmpty()) {
                SaleCartPanel(
                    cart = cart,
                    total = total,
                    onRemove = { saleViewModel.removeFromCart(it) },
                    onAdd = { saleViewModel.addToCart(it.product) },
                    onEditPrice = { cartItemToEditPrice = it }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SaleSelectorHeader(
                        visibleCount = saleInventoryItems.itemCount,
                        filter = saleFilter
                    )
                }
                item {
                    SaleProductFilterRow(
                        selectedFilter = saleFilter,
                        onFilterSelected = { saleViewModel.onSaleInventoryFilterSelected(it) }
                    )
                }
                if (saleInventoryItems.loadState.refresh is LoadState.Loading) {
                    items(8, contentType = { "sale-product-placeholder" }) {
                        SaleProductPlaceholder()
                    }
                } else if (saleInventoryItems.itemCount == 0) {
                    item {
                        SaleEmptyProductState(
                            searchQuery = searchQuery,
                            onAddProduct = { saleViewModel.onShowAddProductDialog() }
                        )
                    }
                } else {
                    items(
                        count = saleInventoryItems.itemCount,
                        key = saleInventoryItems.itemKey { it.productId },
                        contentType = { index ->
                            saleInventoryItems[index]?.let { product ->
                                when {
                                    product.isService -> "service"
                                    product.sellingMethod == SELLING_METHOD_DOSE_ONLY -> "dose"
                                    else -> "product"
                                }
                            } ?: "placeholder"
                        }
                    ) { index ->
                        val product = saleInventoryItems[index]
                        if (product == null) {
                            SaleProductPlaceholder()
                        } else {
                            Column {
                                SaleProductCompactRow(
                                    product = product,
                                    quantityInCart = cartQuantityByProductId[product.productId] ?: 0.0,
                                    onAdd = {
                                        when {
                                            product.isContainer -> saleViewModel.openSaleTypeDialog(product)
                                            product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT -> saleViewModel.openFractionalSaleDialog(product)
                                            product.sellingMethod == SELLING_METHOD_DOSE_ONLY -> saleViewModel.openDoseSaleDialog(product)
                                            else -> saleViewModel.addToCart(product)
                                        }
                                    },
                                    onRemove = {
                                        val itemToRemove = lastCartItemByProductId[product.productId]
                                        if (itemToRemove != null) {
                                            saleViewModel.removeFromCart(itemToRemove)
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            }
                        }
                    }
                    if (saleInventoryItems.loadState.append is LoadState.Loading) {
                        item(contentType = "sale-product-placeholder") {
                            SaleProductPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleProductCompactRow(
    product: Product,
    quantityInCart: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val isInCart = quantityInCart > 0.0
    val typeText = when {
        product.isService -> "Servicio"
        product.sellingMethod == SELLING_METHOD_DOSE_ONLY -> "Dosis"
        else -> "Producto"
    }
    val unit = product.unitMeasure?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    val stockText = if (product.isService || product.sellingMethod == SELLING_METHOD_DOSE_ONLY) {
        typeText
    } else {
        "Stock ${formatCurrency(product.stock).replace(",00", "")}$unit"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(typeText, product.category?.takeIf { it.isNotBlank() }).joinToString(" | "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Gs. ${formatCurrency(product.price)} | $stockText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRemove,
                enabled = isInCart,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.product_selection_remove_content_description))
            }
            Text(
                text = if (isInCart) formatCompactQuantity(quantityInCart) else "0",
                modifier = Modifier.widthIn(min = 28.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.product_selection_add_content_description))
            }
        }
    }
}

@Composable
private fun SaleProductPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.58f), height = 10.dp)
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.38f), height = 8.dp)
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.46f), height = 8.dp)
    }
}

@Composable
private fun SaleCheckoutBar(
    total: Double,
    itemCount: Double,
    enabled: Boolean,
    onConfirm: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "${formatCompactQuantity(itemCount)} items en carrito" else "Carrito vacio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.label_total_sale_amount, formatCurrency(total)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Button(
                onClick = onConfirm,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.button_confirm_sale))
            }
        }
    }
}

@Composable
private fun SaleCustomerPanel(
    clientName: String,
    selectedClientPhone: String?,
    onClientNameChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Cliente de la venta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Puedes asociarlo ahora o dejarlo como venta general.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = clientName,
                onValueChange = onClientNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.sale_client_label)) },
                supportingText = {
                    Text(selectedClientPhone ?: stringResource(R.string.sale_client_general_hint))
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun ClientSuggestionPanel(
    suggestions: List<Client>,
    onClientSelected: (Client) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 136.dp)
        ) {
            items(
                items = suggestions,
                key = { it.clientId },
                contentType = { "client-suggestion" }
            ) { client ->
                ListItem(
                    headlineContent = { Text(client.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(client.phone ?: stringResource(R.string.client_suggestion_no_phone))
                    },
                    leadingContent = {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onClientSelected(client) }
                )
            }
        }
    }
}

@Composable
private fun SaleSearchPanel(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        label = { Text(stringResource(R.string.placeholder_search_product_service)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.content_description_search)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.content_description_clear_search))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun SaleCartPanel(
    cart: List<CartItem>,
    total: Double,
    onRemove: (CartItem) -> Unit,
    onAdd: (CartItem) -> Unit,
    onEditPrice: (CartItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(R.string.shopping_cart_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${cart.size} lineas cargadas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(
                    text = "Gs. ${formatCurrency(total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 184.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = cart,
                    key = { it.cartItemId },
                    contentType = { item ->
                        if (item.product.sellingMethod == SELLING_METHOD_DOSE_ONLY) "dose-cart-item" else "cart-item"
                    }
                ) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onRemove = { onRemove(cartItem) },
                        onAdd = { onAdd(cartItem) },
                        onEditPrice = { onEditPrice(cartItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleSelectorHeader(
    visibleCount: Int,
    filter: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Seleccionar producto o servicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$visibleCount resultados cargados | Vista: $filter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SaleEmptyProductState(
    searchQuery: String,
    onAddProduct: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text("No hay coincidencias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (searchQuery.isBlank()) "Crea un producto o servicio para iniciar la venta." else "Prueba otro termino o registra esta opcion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onAddProduct, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.content_description_add_new_product))
            }
        }
    }
}

@Composable
fun CartItemRow(cartItem: CartItem, onRemove: () -> Unit, onAdd: () -> Unit, onEditPrice: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cartItem.product.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val priceText = cartItem.overridePrice?.let {
                    "${formatCurrency(it)} (Precio final)"
                } ?: formatCurrency(cartItem.product.price * cartItem.quantity)
                Text("Gs. $priceText", style = MaterialTheme.typography.bodyMedium)
                cartItem.notes?.takeIf { it.isNotBlank() }?.let {
                    Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onEditPrice, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Editar precio")
            }
            if (cartItem.product.sellingMethod != SELLING_METHOD_DOSE_ONLY) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove")
                    }
                    Text(
                            text = formatCompactQuantity(cartItem.quantity),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    }
                }
            } else {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove Dose")
                }
            }
        }
    }
}

private fun formatCompactQuantity(quantity: Double): String {
    return formatCurrency(quantity).removeSuffix(",00")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleProductFilterRow(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Todos", "Productos", "Servicios", "Dosis", "Bajo stock")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        filters.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
            ) {
                Text(filter, maxLines = 1)
            }
        }
    }
}

@Composable
fun EditCartItemPriceDialog(
    cartItem: CartItem,
    onDismiss: () -> Unit,
    onConfirm: (Double?, String?) -> Unit
) {
    val defaultPrice = cartItem.overridePrice ?: (cartItem.product.price * cartItem.quantity)
    var priceString by remember(cartItem) { mutableStateOf(defaultPrice.toLong().toString()) }
    var reason by remember(cartItem) { mutableStateOf(cartItem.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Precio final") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(cartItem.product.name, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = priceString,
                    onValueChange = { priceString = it.filter { char -> char.isDigit() } },
                    label = { Text("Precio final del item") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = NumberTransformation(),
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(priceString.toDoubleOrNull(), reason.ifBlank { null }) }) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun FractionalSaleDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, quantity: Double) -> Unit
) {
    var inputMode by remember { mutableStateOf("amount") }
    var amountString by remember { mutableStateOf("") }
    var quantityString by remember { mutableStateOf("") }
    var calculatedValue by remember { mutableStateOf("") }
    val quantityUnitFormat = stringResource(R.string.quantity_unit_format)
    val gsPrefix = stringResource(R.string.text_prefix_gs)
    val invalidPriceMsg = stringResource(R.string.error_invalid_product_price)
    val unitName = when (product.sellingMethod) {
        SELLING_METHOD_BY_WEIGHT_OR_AMOUNT -> stringResource(R.string.unit_kg)
        SELLING_METHOD_BY_UNIT -> stringResource(R.string.unit_unit)
        else -> ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.dialog_title_sell_product, product.name), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.text_price_details, formatCurrency(product.price), unitName), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = inputMode == "amount", onClick = { inputMode = "amount"; quantityString = ""; amountString = ""; calculatedValue = "" })
                        Text(stringResource(R.string.radio_button_by_amount))
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = inputMode == "quantity", onClick = { inputMode = "quantity"; quantityString = ""; amountString = ""; calculatedValue = "" })
                        Text(stringResource(R.string.radio_button_by_quantity, unitName))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (inputMode == "amount" && product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) {
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }
                            amountString = filtered
                            val amount = filtered.toDoubleOrNull() ?: 0.0
                            calculatedValue = if (product.price > 0) {
                                val qty = amount / product.price
                                quantityUnitFormat.format(String.format(Locale.getDefault(), "%.3f", qty), unitName)
                            } else {
                                invalidPriceMsg
                            }
                        },
                        label = { Text(stringResource(R.string.label_amount_gs)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text(gsPrefix) }
                    )
                    Text(stringResource(R.string.text_equivalent_to, calculatedValue), style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(
                        value = quantityString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '.' }
                            if (filtered.count { char -> char == '.' } <= 1) {
                                quantityString = filtered
                                val qty = filtered.toDoubleOrNull() ?: 0.0
                                val totalAmount = qty * product.price
                                calculatedValue = "$gsPrefix ${formatCurrency(totalAmount)}"
                            }
                        },
                        label = { Text(stringResource(R.string.label_quantity_unit, unitName)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text(stringResource(R.string.text_equivalent_to, calculatedValue), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val quantity = if (inputMode == "amount") {
                            val amount = amountString.toDoubleOrNull() ?: 0.0
                            if (product.price > 0) amount / product.price else 0.0
                        } else {
                            quantityString.toDoubleOrNull() ?: 0.0
                        }
                        if (quantity > 0) {
                            onConfirm(product, quantity)
                        }
                    }) {
                        Text(stringResource(R.string.confirm_button))
                    }
                }
            }
        }
    }
}

@Composable
fun DoseSaleDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, notes: String?, price: Double?) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var priceString by remember { mutableStateOf(product.price.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.dialog_title_add_dose, product.name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.label_notes_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceString,
                    onValueChange = { priceString = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_price_optional_override)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = NumberTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val overridePrice = priceString.toDoubleOrNull()
                onConfirm(product, notes.ifBlank { null }, overridePrice)
            }) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun SaleTypeDialog(
    product: Product,
    bulkProduct: Product?,
    onDismiss: () -> Unit,
    onSellByUnit: () -> Unit,
    onSellBulk: (Product) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.dialog_title_select_sale_type)) },
        text = { Text(stringResource(R.string.dialog_message_select_sale_type, product.name)) },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSellByUnit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_sell_by_unit, formatCurrency(product.price)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (bulkProduct != null) {
                            onSellBulk(bulkProduct)
                        }
                    },
                    enabled = bulkProduct != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_sell_bulk))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
