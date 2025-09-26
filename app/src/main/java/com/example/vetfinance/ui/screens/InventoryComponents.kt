package com.example.vetfinance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SellingMethod
import ui.utils.ThousandsSeparatorTransformation
import ui.utils.formatCurrency // Importar formatCurrency
import java.util.Locale // Added import

@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit,
    onDelete: ((Product) -> Unit)? = null,
    productNameSuggestions: List<Product>,
    onProductNameChange: (String) -> Unit
) {
    val isEditing = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    // Asegurarse que el stock se inicializa correctamente como String
    var stock by remember { mutableStateOf(product?.stock?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toLong()?.toString() ?: "") }
    var isService by remember { mutableStateOf(product?.isService ?: false) }
    var selectedSellingMethod by remember { mutableStateOf(product?.selling_method ?: SellingMethod.BY_UNIT) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Actualizar el nombre para sugerencias cuando se abre el diálogo
    LaunchedEffect(Unit) {
        if (!isEditing) {
            onProductNameChange(name)
        }
    }

    if (showDeleteConfirmation && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar ${product.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke(product)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Producto/Servicio" else "Añadir Producto/Servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onProductNameChange(it) // Actualizar para sugerencias en tiempo real
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank() // Marcar error si el nombre está vacío
                )

                AnimatedVisibility(visible = productNameSuggestions.isNotEmpty() && name.isNotBlank()) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text("Coincidencias existentes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Gs. ") },
                    isError = price.isBlank() // Marcar error si el precio está vacío
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { if (it.all { char -> char.isDigit() }) cost = it },
                    label = { Text("Costo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Gs. ") },
                    isError = cost.isBlank() // Marcar error si el costo está vacío
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        // Permitir un solo punto decimal y solo números
                        val filtered = it.filter { char -> char.isDigit() || char == '.' }
                        if (filtered.count { char -> char == '.' } <= 1) {
                            stock = filtered
                        }
                    },
                    label = { Text("Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !isService,
                    modifier = Modifier.fillMaxWidth(),
                    isError = stock.isBlank() && !isService && selectedSellingMethod != SellingMethod.DOSE_ONLY // Marcar error si stock está vacío y es relevante
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isService, onCheckedChange = { isService = it })
                    Text("Es un servicio")
                }
                if (!isService) {
                    SellingMethodDropdown(
                        selectedMethod = selectedSellingMethod,
                        onMethodSelected = { selectedSellingMethod = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentProductData = product ?: Product(name = "", price = 0.0, stock = 0.0, cost = 0.0, selling_method = SellingMethod.BY_UNIT)

                    val newOrUpdatedProduct = currentProductData.copy(
                        name = name,
                        price = price.replace(".", "").toDoubleOrNull() ?: 0.0,
                        stock = if (isService || selectedSellingMethod == SellingMethod.DOSE_ONLY) 9999.0 else stock.toDoubleOrNull() ?: 0.0,
                        cost = cost.replace(".", "").toDoubleOrNull() ?: 0.0,
                        isService = isService,
                        selling_method = if (isService) SellingMethod.DOSE_ONLY else selectedSellingMethod
                    )
                    onConfirm(newOrUpdatedProduct)
                },
                enabled = name.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && (isService || selectedSellingMethod == SellingMethod.DOSE_ONLY || stock.isNotBlank())
            ) {
                Text(if (isEditing) "Actualizar" else "Guardar")
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
                        Text("ELIMINAR")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellingMethodDropdown(selectedMethod: SellingMethod, onMethodSelected: (SellingMethod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val methods = SellingMethod.values()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedMethod.displayName,
            onValueChange = {}, // No editable directamente
            readOnly = true,
            label = { Text("Forma de Venta") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            methods.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.displayName) },
                    onClick = {
                        onMethodSelected(method)
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
    quantity: Double, // Permitir Double para cantidades fraccionadas
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("Precio: Gs. ${formatCurrency(product.price)}", fontSize = 14.sp) // Usar formatCurrency y prefijo Gs.
                if (product.selling_method != SellingMethod.DOSE_ONLY && !product.isService) {
                     Text("Stock: ${formatCurrency(product.stock)}", fontSize = 12.sp) // Usar formatCurrency
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, enabled = quantity > 0.0) {
                    Icon(Icons.Default.Remove, contentDescription = "Quitar")
                }
                Text(
                    text = if (quantity > 0) formatCurrency(quantity) else "0", // Usar formatCurrency
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        }
    }
}
