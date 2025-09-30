package com.example.vetfinance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.R
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_BY_WEIGHT_OR_AMOUNT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import com.example.vetfinance.data.Supplier // Added import
import ui.utils.ThousandsSeparatorTransformation
import ui.utils.formatCurrency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit,
    onDelete: ((Product) -> Unit)? = null,
    productNameSuggestions: List<Product>,
    onProductNameChange: (String) -> Unit,
    allProducts: List<Product>,
    suppliers: List<Supplier> // Added suppliers parameter
) {
    val isEditing = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toLong()?.toString() ?: "") }
    var isService by remember { mutableStateOf(product?.isService ?: false) }
    var selectedSellingMethod by remember { mutableStateOf(product?.sellingMethod ?: SELLING_METHOD_BY_UNIT) }
    var lowStockThreshold by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Estados para la funcionalidad de contenedor
    var isContainer by remember { mutableStateOf(product?.isContainer ?: false) }
    var containerSize by remember { mutableStateOf(product?.containerSize?.toString() ?: "") }
    var selectedContainedProduct by remember { mutableStateOf<Product?>(null) }

    // State for supplier selection
    var selectedSupplierId by remember { mutableStateOf(product?.supplierIdFk) }
    val selectedSupplier = remember(selectedSupplierId, suppliers) {
        suppliers.find { it.supplierId == selectedSupplierId }
    }

    LaunchedEffect(product) {
        if (product?.isContainer == true && product.containedProductId != null) {
            selectedContainedProduct = allProducts.find { it.productId == product.containedProductId }
        }
        if (!isEditing) {
            onProductNameChange(name)
        }
        // Initialize selectedSupplierId if editing an existing product
        if (isEditing) {
            selectedSupplierId = product?.supplierIdFk
        }
    }

    if (showDeleteConfirmation && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.confirm_delete_product_message, product.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke(product)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete_button)) }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel_button)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) stringResource(R.string.product_dialog_edit_title) else stringResource(R.string.product_dialog_add_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onProductNameChange(it)
                    },
                    label = { Text(stringResource(R.string.product_dialog_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )

                AnimatedVisibility(visible = productNameSuggestions.isNotEmpty() && name.isNotBlank()) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.product_dialog_existing_matches), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            items(productNameSuggestions) { suggestion ->
                                Text(
                                    text = suggestion.name,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                    label = { Text(stringResource(R.string.product_dialog_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    isError = price.isBlank()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { if (it.all { char -> char.isDigit() }) cost = it },
                    label = { Text(stringResource(R.string.product_dialog_cost_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    isError = cost.isBlank()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() || char == '.' }
                        if (filtered.count { char -> char == '.' } <= 1) stock = filtered
                    },
                    label = { Text(stringResource(R.string.product_dialog_stock_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !isService,
                    modifier = Modifier.fillMaxWidth(),
                    isError = stock.isBlank() && !isService && selectedSellingMethod != SELLING_METHOD_DOSE_ONLY
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isService, onCheckedChange = { isService = it })
                    Text(stringResource(R.string.product_dialog_is_service_checkbox))
                }
                if (!isService) {
                    SellingMethodDropdown(
                        selectedMethod = selectedSellingMethod,
                        onMethodSelected = { selectedSellingMethod = it }
                    )
                }

                if (selectedSellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) {
                    OutlinedTextField(
                        value = lowStockThreshold,
                        onValueChange = { lowStockThreshold = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text(stringResource(R.string.low_stock_threshold_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Supplier Dropdown
                var supplierDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: stringResource(R.string.label_select_supplier_optional), 
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
                        // Option for no supplier
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_no_supplier)) }, 
                            onClick = {
                                selectedSupplierId = null
                                supplierDropdownExpanded = false
                            }
                        )
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


                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isContainer, onCheckedChange = { isContainer = it })
                    Text(stringResource(R.string.is_container_product_checkbox))
                }

                if (isContainer) {
                    OutlinedTextField(
                        value = containerSize,
                        onValueChange = { containerSize = it },
                        label = { Text(stringResource(R.string.container_size_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedContainedProduct?.name ?: stringResource(R.string.select_bulk_product_placeholder),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.contained_product_label))},
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            allProducts.filter { !it.isContainer && !it.isService }.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text(prod.name) },
                                    onClick = {
                                        selectedContainedProduct = prod
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentProductData = product ?: Product(productId = "", name = "", price = 0.0, stock = 0.0, cost = 0.0, sellingMethod = SELLING_METHOD_BY_UNIT, isService = false)

                    val newOrUpdatedProduct = currentProductData.copy(
                        name = name,
                        price = price.replace(".", "").toDoubleOrNull() ?: 0.0,
                        stock = if (isService || selectedSellingMethod == SELLING_METHOD_DOSE_ONLY) 0.0 else stock.toDoubleOrNull() ?: 0.0,
                        cost = cost.replace(".", "").toDoubleOrNull() ?: 0.0,
                        isService = isService,
                        sellingMethod = if (isService) SELLING_METHOD_DOSE_ONLY else selectedSellingMethod,
                        lowStockThreshold = if (selectedSellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT) lowStockThreshold.toDoubleOrNull() else null,
                        isContainer = isContainer,
                        containerSize = if (isContainer) containerSize.toDoubleOrNull() else null,
                        containedProductId = if (isContainer) selectedContainedProduct?.productId else null,
                        supplierIdFk = selectedSupplierId // Save selected supplier ID
                    )
                    onConfirm(newOrUpdatedProduct)
                },
                enabled = name.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && (isService || selectedSellingMethod != SELLING_METHOD_BY_UNIT || stock.isNotBlank())
            ) {
                Text(if (isEditing) stringResource(R.string.product_dialog_update_button) else stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isEditing) Arrangement.SpaceBetween else Arrangement.End
            ) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_button).uppercase(Locale.getDefault()))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button).uppercase(Locale.getDefault()))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellingMethodDropdown(selectedMethod: String, onMethodSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sellingMethodsWithStringRes = listOf(
        Pair(SELLING_METHOD_BY_UNIT, R.string.selling_method_by_unit),
        Pair(SELLING_METHOD_BY_WEIGHT_OR_AMOUNT, R.string.selling_method_by_weight_or_amount),
        Pair(SELLING_METHOD_DOSE_ONLY, R.string.selling_method_dose_only)
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = stringResource(sellingMethodsWithStringRes.find { it.first == selectedMethod }?.second ?: R.string.selling_method_by_unit),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.selling_method_dropdown_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sellingMethodsWithStringRes.forEach { methodPair ->
                DropdownMenuItem(
                    text = { Text(stringResource(methodPair.second)) },
                    onClick = {
                        onMethodSelected(methodPair.first)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun ProductSelectionItem(
    product: Product,
    quantityInCart: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.product_selection_price_label_gs, formatCurrency(product.price)), fontSize = 14.sp)
                if (product.sellingMethod != SELLING_METHOD_DOSE_ONLY && !product.isService) {
                    Text(stringResource(R.string.product_selection_stock_label, formatCurrency(product.stock).replace(",00","")), fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, enabled = quantityInCart > 0.0) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.product_selection_remove_content_description))
                }
                Text(
                    text = if (quantityInCart > 0) formatCurrency(quantityInCart).replace(",00", "") else "0",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onAdd, enabled = product.isService || product.sellingMethod != SELLING_METHOD_BY_UNIT || quantityInCart < product.stock) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.product_selection_add_content_description))
                }
            }
        }
    }
}