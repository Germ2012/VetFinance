package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import ui.utils.ThousandsSeparatorTransformation
// Importar formatCurrency si se usa directamente aquí, aunque parece que no.

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
        title = { Text(stringResource(R.string.add_new_client_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.client_name_label)) },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text(
                        text = stringResource(R.string.client_name_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.client_phone_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showDebtField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = debt,
                        onValueChange = { debt = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.client_initial_debt_label)) },
                        prefix = { Text(stringResource(R.string.text_prefix_gs)) }, // Prefijo Gs.
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
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
