package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.RestockOrderItem
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.viewmodel.VetViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val allProducts by viewModel.inventory.collectAsState() // Assuming inventory holds all products

    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    val selectedSupplier = remember(selectedSupplierId, suppliers) {
        suppliers.find { it.supplierId == selectedSupplierId }
    }

    // Map: ProductID to Pair(quantityString, costString)
    val restockQuantitiesAndCosts = remember { mutableStateMapOf<String, Pair<String, String>>() }

    val productsToShow by remember(selectedSupplierId, allProducts) {
        derivedStateOf {
            if (selectedSupplierId == null) {
                allProducts.filter { !it.isService } // Show all non-service products if no supplier
            } else {
                allProducts.filter { !it.isService && it.supplierIdFk == selectedSupplierId }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_restock)) }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (selectedSupplierId == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.restock_no_supplier_selected_error)) } 
                        return@Button
                    }

                    val itemsToRestock = mutableListOf<RestockOrderItem>()
                    var calculatedTotalCost = 0.0

                    restockQuantitiesAndCosts.forEach { (productId, pair) ->
                        val quantity = pair.first.toDoubleOrNull()
                        val cost = pair.second.toDoubleOrNull()

                        if (quantity != null && quantity > 0 && cost != null && cost >= 0) {
                            itemsToRestock.add(
                                RestockOrderItem(
                                    orderIdFk = "", // This will be set in ViewModel
                                    productIdFk = productId,
                                    quantity = quantity,
                                    costPerUnit = cost
                                )
                            )
                            calculatedTotalCost += quantity * cost
                        }
                    }

                    if (itemsToRestock.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.restock_no_items_error)) } 
                        return@Button
                    }

                    viewModel.executeRestock(
                        supplierId = selectedSupplierId!!,
                        totalCost = calculatedTotalCost,
                        itemsToRestock = itemsToRestock
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.restock_success_message)) 
                    }
                    restockQuantitiesAndCosts.clear() // Clear inputs
                    // Optionally navigate back or to a different screen
                    // navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedSupplierId != null && restockQuantitiesAndCosts.any { val q = it.value.first.toDoubleOrNull(); val c = it.value.second.toDoubleOrNull(); q != null && q > 0 && c != null && c >= 0 }
            ) {
                Text(stringResource(R.string.button_confirm_restock)) 
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // Supplier Dropdown
            var supplierDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = supplierDropdownExpanded,
                onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.name ?: stringResource(R.string.label_select_supplier), 
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_supplier)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = supplierDropdownExpanded,
                    onDismissRequest = { supplierDropdownExpanded = false }
                ) {
                    suppliers.forEach { supplier ->
                        DropdownMenuItem(
                            text = { Text(supplier.name) },
                            onClick = {
                                selectedSupplierId = supplier.supplierId
                                supplierDropdownExpanded = false
                                // Consider clearing restockQuantitiesAndCosts if supplier changes
                                // restockQuantitiesAndCosts.clear()
                            }
                        )
                    }
                }
            }

            if (productsToShow.isEmpty()){
                 Text(
                    text = stringResource(R.string.products_from_supplier_empty), 
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f) // Takes remaining space
                ) {
                    items(productsToShow, key = { it.productId }) { product ->
                        RestockProductItem(
                            product = product,
                            quantity = restockQuantitiesAndCosts[product.productId]?.first ?: "",
                            cost = restockQuantitiesAndCosts[product.productId]?.second ?: "",
                            onQuantityChange = { newQty ->
                                val currentCost = restockQuantitiesAndCosts[product.productId]?.second ?: ""
                                restockQuantitiesAndCosts[product.productId] = Pair(newQty, currentCost)
                            },
                            onCostChange = { newCost ->
                                val currentQty = restockQuantitiesAndCosts[product.productId]?.first ?: ""
                                restockQuantitiesAndCosts[product.productId] = Pair(currentQty, newCost)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestockProductItem(
    product: Product,
    quantity: String,
    cost: String,
    onQuantityChange: (String) -> Unit,
    onCostChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.current_stock_label, product.stock.toString()), style = MaterialTheme.typography.bodySmall) 
            Text(stringResource(R.string.current_cost_label, product.cost.toString()), style = MaterialTheme.typography.bodySmall) 
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.label_quantity_to_add)) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = onCostChange,
                    label = { Text(stringResource(R.string.label_new_unit_cost)) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}