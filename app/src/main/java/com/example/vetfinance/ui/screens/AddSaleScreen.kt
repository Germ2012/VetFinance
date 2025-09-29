package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod // Assuming SellingMethod enum is here
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency
// Asegúrate de que esta importación esté presente si los archivos están en diferentes paquetes.
// Si están en el mismo, es opcional.
import com.example.vetfinance.ui.screens.ProductSelectionItem
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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCart()
            viewModel.clearProductSearchQuery()
            viewModel.dismissFractionalSaleDialog()
        }
    }

    if (showAddProductDialog) {
        ProductDialog(
            product = null, // Assuming ProductDialog can handle null for new product
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct -> // newProduct here should be a temporary data holder
                // This part needs to align with ProductDialog's output and VetViewModel.addProduct
                // Assuming newProduct contains a 'sellingMethodEnum: SellingMethod'
                // Or if newProduct.selling_method is a string like "Por Unidad":
                // val sm = SellingMethod.fromString(newProduct.selling_method) ?: SellingMethod.BY_UNIT // Fallback
                // For now, I'll assume newProduct.selling_method is the enum type as expected by addProduct
                // This might need adjustment based on ProductDialog implementation
                viewModel.addProduct(
                    newProduct.name, 
                    newProduct.price, 
                    newProduct.stock, 
                    newProduct.cost, 
                    newProduct.isService, 
                    newProduct.selling_method // This expects SellingMethod enum
                )
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
        )
    }

    val currentProductForFractionalSale = productForFractionalSale
    if (showFractionalDialog && currentProductForFractionalSale != null) {
        FractionalSaleDialog(
            product = currentProductForFractionalSale, // Smart-cast to non-null
            onDismiss = { viewModel.dismissFractionalSaleDialog() },
            onConfirm = { product, quantity ->
                viewModel.addOrUpdateProductInCart(product, quantity)
                viewModel.dismissFractionalSaleDialog()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_total_sale_amount, formatCurrency(total)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.finalizeSale { navController.popBackStack() }
                        },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inventory) { product ->
                    ProductSelectionItem(
                        product = product,
                        quantity = cart[product] ?: 0.0,
                        onAdd = {
                            // Assuming Product.sellingMethod is String, and SellingMethod enum has stringValue
                            if (product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue || 
                                product.sellingMethod == SellingMethod.BY_FRACTION.stringValue) { 
                                viewModel.openFractionalSaleDialog(product)
                            } else {
                                viewModel.addToCart(product)
                            }
                        },
                        onRemove = {
                             if (product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue || 
                                 product.sellingMethod == SellingMethod.BY_FRACTION.stringValue) { 
                                viewModel.addOrUpdateProductInCart(product, 0.0) // This quita el producto
                            } else {
                                viewModel.removeFromCart(product)
                            }
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
    var inputMode by remember { 
        mutableStateOf(
            if (product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue) "amount" 
            else "quantity" // Default a cantidad para BY_FRACTION (o si no es BY_WEIGHT_OR_AMOUNT)
        ) 
    }
    var amountString by remember { mutableStateOf("") }
    var quantityString by remember { mutableStateOf("") }
    var calculatedValue by remember { mutableStateof("") }
    val invalidPriceMsg = stringResource(R.string.error_invalid_product_price)
    // Placeholder for unitName, this needs to be resolved based on SellingMethod enum or string resources
    val unitName = when(SellingMethod.fromString(product.sellingMethod)){
        SellingMethod.BY_WEIGHT_OR_AMOUNT -> stringResource(R.string.unit_kg) // Example
        SellingMethod.BY_UNIT -> stringResource(R.string.unit_unit)   // Example
        SellingMethod.DOSE_ONLY -> stringResource(R.string.unit_dose) // Example
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

                if (product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = inputMode == "amount",
                            onClick = { inputMode = "amount"; quantityString = ""; amountString = ""; calculatedValue = "" }
                        )
                        Text(stringResource(R.string.radio_button_by_amount))
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = inputMode == "quantity",
                            onClick = { inputMode = "quantity"; quantityString = ""; amountString = ""; calculatedValue = "" }
                        )
                        Text(stringResource(R.string.radio_button_by_quantity, unitName))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (inputMode == "amount" && product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue) {
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }
                            amountString = filtered
                            val amount = filtered.toDoubleOrNull() ?: 0.0
                            if (product.price > 0) {
                                val qty = amount / product.price
                                calculatedValue = stringResource(R.string.quantity_unit_format, formatCurrency(qty), unitName)
                            } else {
                                calculatedValue = invalidPriceMsg
                            }
                        },
                        label = { Text(stringResource(R.string.label_amount_gs)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text(stringResource(R.string.text_prefix_gs)) }
                    )
                    Text(stringResource(R.string.text_equivalent_to, calculatedValue), style = MaterialTheme.typography.bodySmall)
                } else { // Aplica a inputMode == "quantity" para BY_WEIGHT_OR_AMOUNT y siempre para BY_FRACTION
                    OutlinedTextField(
                        value = quantityString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '.' }
                            if (filtered.count { char -> char == '.' } <= 1) {
                                quantityString = filtered
                                val qty = filtered.toDoubleOrNull() ?: 0.0
                                val totalAmount = qty * product.price
                                calculatedValue = stringResource(R.string.text_prefix_gs) + formatCurrency(totalAmount)
                            }
                        },
                        label = { Text(stringResource(R.string.label_quantity_unit, unitName)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text(stringResource(R.string.text_total_calculated, calculatedValue), style = MaterialTheme.typography.bodySmall)
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
                        val finalQuantity = if (inputMode == "amount" && product.sellingMethod == SellingMethod.BY_WEIGHT_OR_AMOUNT.stringValue) {
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

// ¡IMPORTANTE!
// La función @Composable fun ProductSelectionItem(...) ha sido eliminada de este archivo.
// Ahora solo existe en `InventoryComponents.kt`, lo que resuelve el error de compilación.
