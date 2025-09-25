package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Product
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: VetViewModel) {
    val showDialog by viewModel.showAddProductDialog.collectAsState()
    val filter by viewModel.inventoryFilter.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(inventory, filter) {
        when (filter) {
            "Productos" -> inventory.filter { !it.isService }
            "Servicios" -> inventory.filter { it.isService }
            else -> inventory // "Todos"
        }
    }

    // --- DIÁLOGOS ---
    // Diálogo para AÑADIR un nuevo producto
    if (showDialog) {
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService)
            }
        )
    }

    // Diálogo para EDITAR un producto existente
    productToEdit?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = { productToEdit = null },
            onConfirm = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                productToEdit = null
            },
            onDelete = { productToDelete ->
                viewModel.deleteProduct(productToDelete) // Asumiendo que tienes una función deleteProduct en tu ViewModel
                productToEdit = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddProductDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Producto")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            InventoryFilter(selectedFilter = filter, onFilterSelected = { viewModel.onInventoryFilterChanged(it) })
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredProducts) { product ->
                    InventoryItem(product, onEdit = { productToEdit = it })
                }
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize().padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay productos que coincidan con el filtro.")
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
    val filters = listOf("Todos", "Productos", "Servicios")
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
fun InventoryItem(product: Product, onEdit: (Product) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onEdit(product) }) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (product.isService) "Servicio" else "Producto",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val formattedPrice = String.format("₲ %,.0f", product.price).replace(",", ".")
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!product.isService) {
                    Text(
                        text = "Stock: ${product.stock}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}