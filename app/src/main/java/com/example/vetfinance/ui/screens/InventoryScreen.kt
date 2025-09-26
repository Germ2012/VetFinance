package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod // Added import
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: VetViewModel) {
    val showDialog by viewModel.showAddProductDialog.collectAsState()
    val filter by viewModel.inventoryFilter.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()

    // Consider isLoading based on a specific flag from ViewModel if inventory can be legitimately empty
    val isLoading by viewModel.isLoading.collectAsState() 

    val filteredProducts = remember(inventory, filter) {
        when (filter) {
            "Productos" -> inventory.filter { !it.isService }
            "Servicios" -> inventory.filter { it.isService }
            else -> inventory // "Todos"
        }
    }

    // --- DIÁLOGOS ---
    if (showDialog) {
        ProductDialog(
            product = null,
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct.name, newProduct.price, newProduct.stock, newProduct.cost, newProduct.isService, newProduct.selling_method) // Added selling_method
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
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
                // Ensure updatedProduct includes selling_method from the dialog
                viewModel.updateProduct(updatedProduct) 
                productToEdit = null
            },
            productNameSuggestions = productNameSuggestions,
            onProductNameChange = { viewModel.onProductNameChange(it) }
        )
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar '${product.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancelar")
                }
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

            if (isLoading && inventory.isEmpty()) { // Show indicator if loading AND inventory is currently empty
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryItem(
                            product = product,
                            onEdit = { productToEdit = it },
                            onDelete = { productToDelete = it }
                        )
                    }
                    if (filteredProducts.isEmpty() && !isLoading) {
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
fun InventoryItem(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 0.dp), // Adjust padding for IconButton
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (product.isService) "Servicio" else "Producto (${product.selling_method.name})", // Display selling_method
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = "Gs. ${formatCurrency(product.price)}", // Usar formatCurrency y prefijo Gs.
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!product.isService && product.selling_method != SellingMethod.DOSE_ONLY) {
                    Text(
                        text = "Stock: ${formatCurrency(product.stock)}", // Usar formatCurrency
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(modifier = Modifier.align(Alignment.Top)) { // Align box to the top to keep menu consistent
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            onEdit(product)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            onDelete(product)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
