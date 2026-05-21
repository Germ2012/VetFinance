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
import com.example.vetfinance.data.Client
import ui.utils.NumberTransformation

@Composable
fun AddOrEditClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, debt: Double) -> Unit,
    client: Client? = null,
    showDebtField: Boolean = true
) {
    val initialDebt = remember(client?.clientId) {
        client?.debtAmount?.let { amount ->
            if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        }.orEmpty()
    }
    var name by remember(client?.clientId) { mutableStateOf(client?.name.orEmpty()) }
    var phone by remember(client?.clientId) { mutableStateOf(client?.phone.orEmpty()) }
    var debt by remember(client?.clientId) { mutableStateOf(initialDebt) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (client == null) R.string.add_new_client_title else R.string.edit_client_title
                )
            )
        },
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
                        prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = NumberTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        // CORRECCIÓN APLICADA: No se necesita .replace(".", "")
                        val debtAmount = debt.toDoubleOrNull() ?: 0.0
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
