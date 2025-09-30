package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SELLING_METHOD_BY_WEIGHT_OR_AMOUNT
import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import com.example.vetfinance.data.CartItem
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(viewModel: VetViewModel, navController: NavHostController) {
    val cart by viewModel.shoppingCart.collectAsState()
    val total by viewModel.saleTotal.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val inventory by viewModel.filteredInventory.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

    val showFractionalDialog by viewModel.showFractionalSaleDialog.collectAsState()
    val productForFractionalSale by viewModel.productForFractionalSale.collectAsState()

    val showDoseDialog by viewModel.showDoseSaleDialog.collectAsState()
    val productForDoseSale by viewModel.productForDoseSale.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCart()
            viewModel.clearProductSearchQuery()
            viewModel.dismissFractionalSaleDialog()
            viewModel.dismissDoseSaleDialog()
        }
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(
                    newProduct.name, newProduct.price, newProduct.stock,
                    newProduct.cost, newProduct.isService, newProduct.sellingMethod
                )
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
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
                viewModel.addOrUpdateDoseInCart(product, notes, price)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_sale_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_total_sale_amount, formatCurrency(total)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { viewModel.finalizeSale { navController.popBackStack() } },
                        enabled = cart.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.button_confirm_sale))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onProductSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.placeholder_search_product_service)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.content_description_search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearProductSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.content_description_clear_search))
                        }
                    }
                },
                singleLine = true
            )

            // --- SECCIÓN DEL CARRITO ---
            if (cart.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.shopping_cart_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f), // Ocupa el espacio disponible
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cart, key = { it.cartItemId }) { cartItem ->
                        CartItemRow(
                            cartItem = cartItem,
                            onRemove = { viewModel.removeFromCart(cartItem) },
                            onAdd = { viewModel.addToCart(cartItem.product) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // --- SECCIÓN DE INVENTARIO ---
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inventory, key = { it.productId }) { product ->
                    ProductSelectionItem(
                        product = product,
                        quantityInCart = cart.filter { it.product.productId == product.productId }.sumOf { it.quantity },
                        onAdd = {
                            when (product.sellingMethod) {
                                SELLING_METHOD_BY_WEIGHT_OR_AMOUNT -> viewModel.openFractionalSaleDialog(product)
                                SELLING_METHOD_DOSE_ONLY -> viewModel.openDoseSaleDialog(product)
                                else -> viewModel.addToCart(product)
                            }
                        },
                        onRemove = {
                            val itemToRemove = cart.findLast { it.product.productId == product.productId }
                            if (itemToRemove != null) {
                                viewModel.removeFromCart(itemToRemove)
                            }
                        },
                        onQuantityChange = { qty ->
                            viewModel.addOrUpdateProductInCart(product, qty)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FractionalSaleDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, quantity: Double) -> Unit
) {
    var inputMode by remember { mutableStateOf(if (product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) "amount" else "quantity") }
    var amountString by remember { mutableStateOf("") }
    var quantityString by remember { mutableStateOf("") }
    var calculatedValue by remember { mutableStateOf("") }
    val quantityUnitFormat = stringResource(R.string.quantity_unit_format)
    val gsPrefix = stringResource(R.string.text_prefix_gs)
    val invalidPriceMsg = stringResource(R.string.error_invalid_product_price)
    val unitName = when (product.sellingMethod) {
        SELLING_METHOD_BY_WEIGHT_OR_AMOUNT -> stringResource(R.string.unit_kg)
        SELLING_METHOD_BY_UNIT -> stringResource(R.string.unit_unit)
        SELLING_METHOD_DOSE_ONLY -> stringResource(R.string.unit_dose)
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
                    Text(stringResource(R.string.text_total_calculated, calculatedValue), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val finalQuantity = if (inputMode == "amount" && product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) {
                            val amount = amountString.toDoubleOrNull() ?: 0.0
                            if (product.price > 0) amount / product.price else 0.0
                        } else {
                            quantityString.toDoubleOrNull() ?: 0.0
                        }
                        if (finalQuantity > 0) {
                            onConfirm(product, finalQuantity)
                        }
                    }) {
                        Text(stringResource(R.string.button_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(cartItem: CartItem, onRemove: () -> Unit, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cartItem.product.name, fontWeight = FontWeight.Bold)
                    if (cartItem.notes != null) {
                        Text(
                            text = stringResource(R.string.notes_prefix, cartItem.notes),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                Text(
                    text = formatCurrency(cartItem.overridePrice ?: (cartItem.product.price * cartItem.quantity)),
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.content_description_remove_one))
                }
                Text(
                    text = if (cartItem.quantity % 1.0 == 0.0) cartItem.quantity.toInt().toString() else "%.2f".format(cartItem.quantity),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onAdd,
                    enabled = cartItem.product.sellingMethod == SELLING_METHOD_BY_UNIT
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_one))
                }
            }
        }
    }
}

@Composable
fun DoseSaleDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, notes: String, price: Double) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var priceString by remember { mutableStateOf(product.price.toLong().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.dose_dialog_title, product.name), style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.dose_dialog_notes_label)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                OutlinedTextField(
                    value = priceString,
                    onValueChange = { priceString = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.dose_dialog_price_label)) },
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalPrice = priceString.toDoubleOrNull() ?: product.price
                            onConfirm(product, notes, finalPrice)
                        },
                        enabled = priceString.isNotBlank()
                    ) {
                        Text(stringResource(R.string.button_confirm))
                    }
                }
            }
        }
    }
}