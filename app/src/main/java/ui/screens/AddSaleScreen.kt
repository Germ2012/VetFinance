package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.vetfinance.data.Product
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(viewModel: VetViewModel, navController: NavHostController) {
    val cart by viewModel.shoppingCart.collectAsState()
    val total by viewModel.saleTotal.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val inventory by viewModel.filteredInventory.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()


    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCart()
            viewModel.clearProductSearchQuery()
        }
    }

    if (showAddProductDialog) {
        // Usamos el diálogo unificado
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService)
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("Total: Gs %,.0f", total).replace(",", "."),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                        quantity = cart[product] ?: 0,
                        onAdd = { viewModel.addToCart(product) },
                        onRemove = { viewModel.removeFromCart(product) }
                    )
                }
            }
        }
    }
}