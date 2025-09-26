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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod
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
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService, newProduct.selling_method)
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
        )
    }

    if (showFractionalDialog && productForFractionalSale != null) {
        FractionalSaleDialog(
            product = productForFractionalSale!!,
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
                title = { Text("Registrar Nueva Venta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onShowAddProductDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir Nuevo Producto")
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
                        text = "Total: Gs. ${formatCurrency(total)}", // Usar formatCurrency y prefijo Gs.
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.finalizeSale { navController.popBackStack() }
                        },
                        enabled = cart.isNotEmpty()
                    ) {
                        Text("Confirmar Venta")
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
                label = { Text("Buscar producto o servicio...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearProductSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
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
                            if (product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT || product.selling_method == SellingMethod.BY_FRACTION) { // Modificado para incluir BY_FRACTION
                                viewModel.openFractionalSaleDialog(product)
                            } else {
                                viewModel.addToCart(product)
                            }
                        },
                        onRemove = {
                             if (product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT || product.selling_method == SellingMethod.BY_FRACTION) { // Modificado para incluir BY_FRACTION
                                // Para remover fraccionales, simplemente los quitamos o ponemos en 0 si es necesario un diálogo futuro
                                viewModel.addOrUpdateProductInCart(product, 0.0) // Esto quita el producto
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
    var inputMode by remember { mutableStateOf(if (product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT) "amount" else "quantity") } // Default a cantidad para BY_FRACTION
    var amountString by remember { mutableStateOf("") }
    var quantityString by remember { mutableStateOf("") }
    var calculatedValue by remember { mutableStateOf("") }


    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Vender: ${product.name}", style = MaterialTheme.typography.titleLarge)
                Text("Precio: Gs. ${formatCurrency(product.price)} / ${product.selling_method.unitName}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = inputMode == "amount",
                            onClick = { inputMode = "amount"; quantityString = ""; amountString = ""; calculatedValue = "" }
                        )
                        Text("Por Monto (Gs.)")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = inputMode == "quantity",
                            onClick = { inputMode = "quantity"; quantityString = ""; amountString = ""; calculatedValue = "" }
                        )
                        Text("Por Cantidad (${product.selling_method.unitName})")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (inputMode == "amount" && product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT) {
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }
                            amountString = filtered
                            val amount = filtered.toDoubleOrNull() ?: 0.0
                            if (product.price > 0) {
                                val qty = amount / product.price
                                calculatedValue = "${formatCurrency(qty)} ${product.selling_method.unitName}"
                            } else {
                                calculatedValue = "Precio de producto no válido"
                            }
                        },
                        label = { Text("Monto en Gs.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text("Gs. ") }
                    )
                    Text("Equivale a: $calculatedValue", style = MaterialTheme.typography.bodySmall)
                } else { // Aplica a inputMode == "quantity" para BY_WEIGHT_OR_AMOUNT y siempre para BY_FRACTION
                    OutlinedTextField(
                        value = quantityString,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '.' }
                            if (filtered.count { char -> char == '.' } <= 1) {
                                quantityString = filtered
                                val qty = filtered.toDoubleOrNull() ?: 0.0
                                val totalAmount = qty * product.price
                                calculatedValue = "Gs. ${formatCurrency(totalAmount)}"
                            }
                        },
                        label = { Text("Cantidad (${product.selling_method.unitName})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text("Total: $calculatedValue", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val finalQuantity = if (inputMode == "amount" && product.selling_method == SellingMethod.BY_WEIGHT_OR_AMOUNT) {
                            val amount = amountString.toDoubleOrNull() ?: 0.0
                            if (product.price > 0) amount / product.price else 0.0
                        } else {
                            quantityString.toDoubleOrNull() ?: 0.0
                        }
                        if (finalQuantity > 0) {
                            onConfirm(product, finalQuantity)
                        }
                    }) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

// ¡IMPORTANTE!
// La función @Composable fun ProductSelectionItem(...) ha sido eliminada de este archivo.
// Ahora solo existe en `InventoryComponents.kt`, lo que resuelve el error de compilación.