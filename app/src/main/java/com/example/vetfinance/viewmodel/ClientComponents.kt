package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Client

// Componente para mostrar un solo cliente en la lista
@Composable
fun ClientItem(
    client: Client,
    onPayClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, fontWeight = FontWeight.Bold)
                val formattedDebt = String.format("₲ %,.0f", client.debtAmount).replace(",", ".")
                Text("Deuda: $formattedDebt")
            }
            Button(onClick = onPayClick) {
                Text("Pagar")
            }
        }
    }
}


// Diálogo para registrar un pago
@Composable
fun PaymentDialog(
    client: Client,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val debtFormatted = String.format("%,.0f", client.debtAmount).replace(",", ".")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Realizar Pago") },
        text = {
            Column {
                Text("Cliente: ${client.name}")
                Text("Deuda actual: ₲$debtFormatted")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Monto a pagar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paymentAmount = amount.toDoubleOrNull() ?: 0.0
                    if (paymentAmount > 0) {
                        onConfirm(paymentAmount)
                    }
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
@Composable
fun AddOwnerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nuevo Dueño") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Nombre del dueño") },
                    isError = nameError,
                    singleLine = true
                )
                if (nameError) {
                    Text("El nombre no puede estar vacío", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() } },
                    label = { Text("Teléfono (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone)
                    } else {
                        nameError = true
                    }
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