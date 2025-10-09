package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import ui.utils.NumberTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRestockScreen( // Renamed
    viewModel: VetViewModel,
    navController: NavController
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val allProducts by viewModel.inventory.collectAsState()
    val searchQuery by viewModel.restockSearchQuery.collectAsState()
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    val selectedSupplier = remember(selectedSupplierId, suppliers) {
        suppliers.find { it.supplierId == selectedSupplierId }
    }

    LaunchedEffect(Unit) {
        viewModel.onRestockSearchQueryChange("")
    }

    val restockQuantitiesAndCosts = remember { mutableStateMapOf<String, Pair<String, String>>() }

    val productsToShow by remember(selectedSupplierId, allProducts, searchQuery) {
        derivedStateOf {
            val baseList = allProducts.filter { !it.isService }
            val filteredBySupplier = if (selectedSupplierId != null) {
                baseList.filter { it.supplierIdFk == selectedSupplierId }
            } else {
                baseList
            }
            if (searchQuery.isNotBlank()) {
                filteredBySupplier.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                filteredBySupplier
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val localDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        selectedDate = localDate.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    } else {
                        selectedDate = System.currentTimeMillis()
                    }
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
                title = { Text("Registrar Reabastecimiento") },
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
                                    orderIdFk = "",
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
                        orderDate = selectedDate
                    )

                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.restock_success_message))
                        navController.popBackStack()
                    }
                    restockQuantitiesAndCosts.clear()
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
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onRestockSearchQueryChange(it) },
                label = { Text(stringResource(R.string.search_product_hint)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
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
                    modifier = Modifier.weight(1f)
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
                    visualTransformation = NumberTransformation(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
