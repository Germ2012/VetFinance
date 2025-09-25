package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Product
import ui.utils.ThousandsSeparatorTransformation

/**
 * Diálogo unificado para añadir o editar un producto/servicio.
 * Si 'product' es null, funciona en modo "Añadir". De lo contrario, en modo "Editar".
 */
@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit,
    onDelete: ((Product) -> Unit)? = null
) {
    val isEditing = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toLong()?.toString() ?: "") }
    var isService by remember { mutableStateOf(product?.isService ?: false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { if (it.all { char -> char.isDigit() }) price = it }, label = { Text("Precio") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = ThousandsSeparatorTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cost, onValueChange = { if (it.all { char -> char.isDigit() }) cost = it }, label = { Text("Costo") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = ThousandsSeparatorTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stock, onValueChange = { if (it.all { char -> char.isDigit() }) stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !isService, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isService, onCheckedChange = { isService = it })
                    Text("Es un servicio")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newOrUpdatedProduct = (product ?: Product(name = "", price = 0.0, stock = 0, cost = 0.0)).copy(
                        name = name,
                        price = price.replace(".", "").toDoubleOrNull() ?: 0.0,
                        stock = if (isService) 9999 else stock.toIntOrNull() ?: 0,
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
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold)
                Text(text = "Precio: Gs ${"%,.0f".format(product.price).replace(",",".")}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove, enabled = quantity > 0) {
                    Icon(Icons.Default.Remove, contentDescription = "Quitar")
                }
                Text(text = quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        }
    }
}