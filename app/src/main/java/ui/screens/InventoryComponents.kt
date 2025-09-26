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
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toLong()?.toString() ?: "") }
    var isService by remember { mutableStateOf(product?.isService ?: false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                        onProductNameChange(it)
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
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

                OutlinedTextField(value = price, onValueChange = { if (it.all { char -> char.isDigit() }) price = it }, label = { Text("Precio") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = ThousandsSeparatorTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cost, onValueChange = { if (it.all { char -> char.isDigit() }) cost = it }, label = { Text("Costo") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = ThousandsSeparatorTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stock, onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !isService, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isService, onCheckedChange = { isService = it })
                    Text("Es un servicio")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentProductData = product ?: Product(name = "", price = 0.0, stock = 0.0, cost = 0.0)

                    val newOrUpdatedProduct = currentProductData.copy(
                        name = name,
                        price = price.replace(".", "").toDoubleOrNull() ?: 0.0,
                        stock = if (isService) 9999.0 else stock.toDoubleOrNull() ?: 0.0,
                        cost = cost.replace(".", "").toDoubleOrNull() ?: 0.0,
                        isService = isService
                    )
                    onConfirm(newOrUpdatedProduct)
                },
                enabled = name.isNotBlank() && price.isNotBlank() && cost.isNotBlank()
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

@Composable
fun ProductSelectionItem(
    product: Product,
    quantity: Double,
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
                Text("Precio: Gs ${ui.utils.formatCurrency(product.price)}", fontSize = 14.sp)
                if (product.selling_method != SellingMethod.DOSE_ONLY && !product.isService) {
                     Text("Stock: ${product.stock}", fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, enabled = quantity > 0.0) {
                    Icon(Icons.Default.Remove, contentDescription = "Quitar")
                }
                Text(
                    text = if (quantity > 0) "%.2f".format(Locale.US, quantity) else "0",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        }
    }
}
