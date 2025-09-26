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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: VetViewModel) {
    val showDialog by viewModel.showAddProductDialog.collectAsState()
    val filter by viewModel.inventoryFilter.collectAsState() // This will hold localized filter string
    val inventory by viewModel.inventory.collectAsState()
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val productNameSuggestions by viewModel.productNameSuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val productsFilterText = stringResource(R.string.inventory_filter_products)
    val servicesFilterText = stringResource(R.string.inventory_filter_services)

    val filteredProducts = remember(inventory, filter, productsFilterText, servicesFilterText) {
        when (filter) {
            productsFilterText -> inventory.filter { !it.isService }
            servicesFilterText -> inventory.filter { it.isService }
            else -> inventory // "Todos" or default
        }
    }

    // --- DIÁLOGOS ---
    if (showDialog) {
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

    productToEdit?.let { product ->
        ProductDialog(
            product = product,
            onDismiss = {
                productToEdit = null
                viewModel.clearProductNameSuggestions()
            },
            onConfirm = { updatedProduct ->
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
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_delete_product_message, product.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowAddProductDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_product))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text(stringResource(R.string.tab_inventory), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            InventoryFilter(selectedFilter = filter, onFilterSelected = { viewModel.onInventoryFilterChanged(it) })
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && inventory.isEmpty()) {
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
                                Text(stringResource(R.string.inventory_no_products_matching_filter))
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
    val filters = listOf(
        stringResource(R.string.inventory_filter_all),
        stringResource(R.string.inventory_filter_products),
        stringResource(R.string.inventory_filter_services)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        filters.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                onClick = { onFilterSelected(label) }, // label is already localized
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
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (product.isService) stringResource(R.string.inventory_item_type_service)
                           else stringResource(R.string.inventory_item_type_product, product.selling_method.name),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.text_prefix_gs) + formatCurrency(product.price),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!product.isService && product.selling_method != SellingMethod.DOSE_ONLY) {
                    Text(
                        text = stringResource(R.string.label_product_stock, formatCurrency(product.stock).replace(",00","")),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(modifier = Modifier.align(Alignment.Top)) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_content_description))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.inventory_item_menu_edit)) },
                        onClick = {
                            onEdit(product)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.inventory_item_menu_delete)) },
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
