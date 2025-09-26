package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.data.Client
import ui.utils.ThousandsSeparatorTransformation
import ui.utils.formatCurrency // Importar formatCurrency

/**
 * Componente que muestra la informaciÃ³n de un cliente en una tarjeta.
 */
@Composable
fun ClientItem(client: Client, onPayClick: () -> Unit, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "Deuda: Gs. ${formatCurrency(client.debtAmount)}", // Usar formatCurrency y prefijo Gs.
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(onClick = onPayClick) {
                Text("Abonar")
            }
        }
    }
}

/**
 * DiÃ¡logo para registrar un pago para un cliente especÃ­fico.
 */
@Composable
fun PaymentDialog(client: Client, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar Deuda de ${client.name}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                label = { Text("Monto a abonar") },
                prefix = { Text("Gs. ") }, // Prefijo Gs.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                visualTransformation = ThousandsSeparatorTransformation()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val paymentAmount = amount.replace(".", "").toDoubleOrNull() ?: 0.0
                    if (paymentAmount > 0) {
                        onConfirm(paymentAmount)
                    }
                },
                enabled = amount.isNotBlank()
            ) {
                Text("Confirmar Pago")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
