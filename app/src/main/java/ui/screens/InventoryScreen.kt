package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Product
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: VetViewModel) {
    val inventory by viewModel.inventory.collectAsState()
    val showDialog by viewModel.showAddProductDialog.collectAsState()

    // Mostramos el diálogo si el estado es 'true'
    if (showDialog) {
        AddProductDialog(
            onDismiss = { viewModel.onDismissAddProductDialog() },
            onConfirm = { name, price, stock, isService ->
                viewModel.addProduct(name, price, stock, isService)
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
            if (inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos en el inventario.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(inventory) { product ->
                        InventoryItem(product)
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItem(product: Product) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                // --- CAMBIO APLICADO AQUÍ ---
                // Se crea una variable con el precio formateado en Guaraníes
                val formattedPrice = String.format("₲ %,.0f", product.price).replace(",", ".")

                Text(
                    text = formattedPrice, // Se usa la nueva variable
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


@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, stock: Int, isService: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var isService by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Producto/Servicio") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isService // Deshabilitado si es un servicio
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isService, onCheckedChange = { isService = it })
                    Text("Es un servicio")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        price.toDoubleOrNull() ?: 0.0,
                        if (isService) 999 else stock.toIntOrNull() ?: 0,
                        isService
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}