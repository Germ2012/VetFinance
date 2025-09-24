// ruta: app/src/main/java/com/example/vetfinance/ui/screens/AddSaleScreen.kt

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
    // --- Estados observados desde el ViewModel ---
    val cart by viewModel.shoppingCart.collectAsState()
    val total by viewModel.saleTotal.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val filteredInventory by viewModel.filteredInventory.collectAsState()

    // 👇 CORRECCIÓN: Se usa el nombre correcto de la variable de búsqueda de productos
    val searchQuery by viewModel.productSearchQuery.collectAsState()

    // 👇 CORRECCIÓN: Se llama a la función correcta para limpiar la búsqueda de productos
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCart()
            viewModel.clearProductSearchQuery()
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { name, price, stock, isService ->
                viewModel.addProduct(name, price, stock, isService)
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
                        text = String.format("Total: Gs %,.0f", total).replace(",", "."),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.finalizeSale()
                            navController.popBackStack()
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
                // 👇 CORRECCIÓN: Se usa la variable correcta
                value = searchQuery,
                // 👇 CORRECCIÓN: Se llama a la función correcta
                onValueChange = { viewModel.onProductSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Buscar producto o servicio...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    // 👇 CORRECCIÓN: Se usa la variable correcta
                    if (searchQuery.isNotEmpty()) {
                        // 👇 CORRECCIÓN: Se llama a la función correcta
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
                items(filteredInventory) { product ->
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

// El composable ProductSelectionItem no necesita cambios.
@Composable
fun ProductSelectionItem(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text(String.format("Gs %,.0f", product.price).replace(",", "."))
            }

            if (quantity == 0) {
                Button(onClick = onAdd) { Text("Añadir") }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = onRemove
                    ) { Text("-", fontSize = 20.sp) }

                    Text("$quantity", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))

                    Button(
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = onAdd
                    ) { Text("+", fontSize = 20.sp) }
                }
            }
        }
    }
}