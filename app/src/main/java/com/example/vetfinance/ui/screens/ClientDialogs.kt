package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ui.utils.ThousandsSeparatorTransformation
import ui.utils.formatCurrency // Importar formatCurrency

@Composable
fun AddOrEditClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, debt: Double) -> Unit,
    showDebtField: Boolean = true // Parámetro para controlar la visibilidad del campo de deuda
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nuevo Cliente") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Nombre del cliente") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text("El nombre no puede estar vacío", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() } },
                    label = { Text("Teléfono (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showDebtField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = debt,
                        onValueChange = { debt = it.filter { char -> char.isDigit() } },
                        label = { Text("Deuda Inicial (Opcional)") },
                        prefix = { Text("Gs. ") }, // Prefijo Gs.
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val debtAmount = debt.replace(".", "").toDoubleOrNull() ?: 0.0
                        onConfirm(name, phone, debtAmount)
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
