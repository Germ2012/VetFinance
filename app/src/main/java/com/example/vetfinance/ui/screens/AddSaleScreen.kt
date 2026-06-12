package com.example.vetfinance.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.vetfinance.R
import com.example.vetfinance.data.CartItem
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_BY_WEIGHT_OR_AMOUNT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.NumberTransformation
import ui.utils.formatCurrency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(viewModel: VetViewModel, navController: NavHostController) {
    val cart by viewModel.shoppingCart.collectAsState()
    val total by viewModel.saleTotal.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val inventory by viewModel.filteredInventory.collectAsState()
    val allProductsList by viewModel.inventory.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val frequentProducts by viewModel.frequentSaleProducts.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()
    val clientNameSuggestions by viewModel.clientNameSuggestions.collectAsState()

    var saleClientName by remember { mutableStateOf("") }
    var selectedSaleClientId by remember { mutableStateOf<String?>(null) }
    var saleFilter by remember { mutableStateOf("Todos") }
    var showExitCartDialog by remember { mutableStateOf(false) }
    var cartItemToEditPrice by remember { mutableStateOf<CartItem?>(null) }

    val showFractionalDialog by viewModel.showFractionalSaleDialog.collectAsState()
    val productForFractionalSale by viewModel.productForFractionalSale.collectAsState()

    val showDoseDialog by viewModel.showDoseSaleDialog.collectAsState()
    val productForDoseSale by viewModel.productForDoseSale.collectAsState()

    val saleTypeDialogProduct by viewModel.saleTypeDialogProduct.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearProductSearchQuery()
            viewModel.dismissFractionalSaleDialog()
            viewModel.dismissDoseSaleDialog()
            viewModel.clearClientNameSuggestions()
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
            title = { Text("Carrito activo") },
            text = { Text("Hay productos cargados. Podes guardar temporalmente el carrito y volver luego, o salir limpiandolo.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.clearCart()
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
                viewModel.updateCartItemPrice(cartItem, finalPrice, reason)
                cartItemToEditPrice = null
            }
        )
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            allProducts = allProductsList,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.insertOrUpdateProduct(newProduct)
                viewModel.onDismissAddProductDialog()
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) },
            suppliers = suppliers
        )
    }

    val currentProductForFractionalSale = productForFractionalSale
    if (showFractionalDialog && currentProductForFractionalSale != null) {
        FractionalSaleDialog(
            product = currentProductForFractionalSale,
            onDismiss = { viewModel.dismissFractionalSaleDialog() },
            onConfirm = { product, quantity ->
                viewModel.addOrUpdateProductInCart(product, quantity)
                viewModel.dismissFractionalSaleDialog()
            }
        )
    }

    val currentProductForDoseSale = productForDoseSale
    if (showDoseDialog && currentProductForDoseSale != null) {
        DoseSaleDialog(
            product = currentProductForDoseSale,
            onDismiss = { viewModel.dismissDoseSaleDialog() },
            onConfirm = { product, notes, price ->
                viewModel.addOrUpdateDoseInCart(product, notes ?: "", price ?: product.price)
                viewModel.dismissDoseSaleDialog()
            }
        )
    }

    saleTypeDialogProduct?.let { product ->
        SaleTypeDialog(
            product = product,
            viewModel = viewModel,
            allProducts = allProductsList
        )
    }

    val lowStockIds = lowStockProducts.map { it.productId }.toSet()
    val frequentIds = frequentProducts.map { it.productId }
    val visibleInventory = inventory
        .filter { product ->
            when (saleFilter) {
                "Productos" -> !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY
                "Servicios" -> product.isService
                "Dosis" -> !product.isService && product.sellingMethod == SELLING_METHOD_DOSE_ONLY
                "Bajo stock" -> product.productId in lowStockIds
                else -> true
            }
        }
        .sortedWith(
            compareBy<Product> {
                val rank = frequentIds.indexOf(it.productId)
                if (rank >= 0) rank else Int.MAX_VALUE
            }.thenBy { it.name.lowercase(Locale.getDefault()) }
        )
    val visibleProducts = visibleInventory.count { !it.isService && it.sellingMethod != SELLING_METHOD_DOSE_ONLY }
    val visibleServices = visibleInventory.count { it.isService }
    val visibleDoses = visibleInventory.count { !it.isService && it.sellingMethod == SELLING_METHOD_DOSE_ONLY }
    val cartUnits = cart.sumOf { it.quantity }

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
                    IconButton(onClick = { viewModel.onShowAddProductDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_new_product))
                    }
                }
            )
        },
        bottomBar = {
            SaleCheckoutBar(
                total = total,
                itemCount = cartUnits,
                enabled = cart.isNotEmpty(),
                onConfirm = {
                    viewModel.finalizeSale(
                        clientName = saleClientName,
                        selectedClientId = selectedSaleClientId
                    ) { navController.popBackStack() }
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
                selectedClientPhone = clients.find { it.clientId == selectedSaleClientId }?.phone,
                onClientNameChange = {
                    saleClientName = it
                    selectedSaleClientId = null
                    viewModel.onClientNameChange(it)
                }
            )

            AnimatedVisibility(visible = clientNameSuggestions.isNotEmpty() && saleClientName.isNotBlank()) {
                ClientSuggestionPanel(
                    suggestions = clientNameSuggestions,
                    onClientSelected = { client ->
                        saleClientName = client.name
                        selectedSaleClientId = client.clientId
                        viewModel.clearClientNameSuggestions()
                    }
                )
            }

            SaleSearchPanel(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.onProductSearchQueryChange(it) },
                onClearSearch = { viewModel.clearProductSearchQuery() }
            )

            AnimatedVisibility(visible = cart.isNotEmpty()) {
                SaleCartPanel(
                    cart = cart,
                    total = total,
                    onRemove = { viewModel.removeFromCart(it) },
                    onAdd = { viewModel.addToCart(it.product) },
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
                        visibleCount = visibleInventory.size,
                        productCount = visibleProducts,
                        serviceCount = visibleServices,
                        doseCount = visibleDoses
                    )
                }
                item {
                    SaleProductFilterRow(
                        selectedFilter = saleFilter,
                        onFilterSelected = { saleFilter = it }
                    )
                }
                if (frequentProducts.isNotEmpty() && saleFilter == "Todos" && searchQuery.isBlank()) {
                    item {
                        SaleInlineBanner(
                            icon = Icons.Default.PointOfSale,
                            title = "Frecuentes primero",
                            body = "Ordenados segun lo que mas vendes para cargar mas rapido."
                        )
                    }
                }
                if (visibleInventory.isEmpty()) {
                    item {
                        SaleEmptyProductState(
                            searchQuery = searchQuery,
                            onAddProduct = { viewModel.onShowAddProductDialog() }
                        )
                    }
                } else {
                    items(visibleInventory, key = { it.productId }) { product ->
                        ProductSelectionItem(
                            product = product,
                            quantityInCart = cart.filter { it.product.productId == product.productId }.sumOf { it.quantity },
                            onAdd = {
                                when {
                                    product.isContainer -> viewModel.openSaleTypeDialog(product)
                                    product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT -> viewModel.openFractionalSaleDialog(product)
                                    product.sellingMethod == SELLING_METHOD_DOSE_ONLY -> viewModel.openDoseSaleDialog(product)
                                    else -> viewModel.addToCart(product)
                                }
                            },
                            onRemove = {
                                val itemToRemove = cart.findLast { it.product.productId == product.productId }
                                if (itemToRemove != null) {
                                    viewModel.removeFromCart(itemToRemove)
                                }
                            }
                        )
                    }
                }
            }
        }
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
                shape = RoundedCornerShape(8.dp)
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
        shape = RoundedCornerShape(8.dp),
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
                shape = RoundedCornerShape(8.dp)
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
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 136.dp)
        ) {
            items(suggestions, key = { it.clientId }) { client ->
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
        shape = RoundedCornerShape(8.dp)
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
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
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
                items(cart, key = { it.cartItemId }) { cartItem ->
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
    productCount: Int,
    serviceCount: Int,
    doseCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Seleccionar producto o servicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$visibleCount opciones disponibles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SaleMiniMetric("Productos", productCount.toString(), Icons.Default.Inventory, Modifier.weight(1f))
            SaleMiniMetric("Servicios", serviceCount.toString(), Icons.Default.MedicalServices, Modifier.weight(1f))
            SaleMiniMetric("Dosis", doseCount.toString(), Icons.Default.WarningAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SaleMiniMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SaleInlineBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
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
        shape = RoundedCornerShape(8.dp),
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
            FilledTonalButton(onClick = onAddProduct, shape = RoundedCornerShape(8.dp)) {
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
        shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(8.dp),
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

    Dialog(onDismissRequest = onDismiss) {
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
    viewModel: VetViewModel,
    allProducts: List<Product>
) {
    val bulkProduct = remember(product, allProducts) {
        allProducts.find { it.productId == product.containedProductId }
    }

    AlertDialog(
        onDismissRequest = { viewModel.closeSaleTypeDialog() },
        title = { Text(stringResource(R.string.dialog_title_select_sale_type)) },
        text = { Text(stringResource(R.string.dialog_message_select_sale_type, product.name)) },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.addToCart(product)
                        viewModel.closeSaleTypeDialog()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_sell_by_unit, formatCurrency(product.price)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (bulkProduct != null) {
                            viewModel.openFractionalSaleDialog(bulkProduct)
                        }
                        viewModel.closeSaleTypeDialog()
                    },
                    enabled = bulkProduct != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_sell_bulk))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeSaleTypeDialog() }) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
