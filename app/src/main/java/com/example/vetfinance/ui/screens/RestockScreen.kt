package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange // Added for Tarea 2
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
import java.text.SimpleDateFormat // Added for Tarea 2
import java.util.* // Added for Tarea 2
import ui.utils.ThousandsSeparatorTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val allProducts by viewModel.inventory.collectAsState()
    val searchQuery by viewModel.restockSearchQuery.collectAsState() // Added for Tarea 2
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) } // Added for Tarea 2
    var showDatePicker by remember { mutableStateOf(false) } // Added for Tarea 2

    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    val selectedSupplier = remember(selectedSupplierId, suppliers) {
        suppliers.find { it.supplierId == selectedSupplierId }
    }

    val restockQuantitiesAndCosts = remember { mutableStateMapOf<String, Pair<String, String>>() }

    val productsToShow by remember(selectedSupplierId, allProducts, searchQuery) { // searchQuery added for Tarea 2
        derivedStateOf {
            val baseList = allProducts.filter { !it.isService }
            val filteredBySupplier = if (selectedSupplierId != null) {
                baseList.filter { it.supplierIdFk == selectedSupplierId }
            } else {
                baseList
            }
            if (searchQuery.isNotBlank()) { // Added for Tarea 2
                filteredBySupplier.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                filteredBySupplier
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showDatePicker) { // Added for Tarea 2
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text(stringResource(id = R.string.accept_button)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(id = R.string.cancel_button)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                        itemsToRestock = itemsToRestock,
                        orderDate = selectedDate // Pasa la fecha seleccionada - Modified for Tarea 2
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.restock_success_message))
                    }
                    restockQuantitiesAndCosts.clear()
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
            
            // Search Bar - Added for Tarea 2
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onRestockSearchQueryChange(it) },
                label = { Text(stringResource(R.string.search_product_hint)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Date Picker Row - Added for Tarea 2
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(stringResource(R.string.invoice_date_label), modifier = Modifier.weight(1f))
                Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate)))
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date_content_description))
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
                    visualTransformation = ThousandsSeparatorTransformation(), // This was Tarea 1
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
