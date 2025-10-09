package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetfinance.R
import com.example.vetfinance.data.Client
import ui.utils.NumberTransformation
import ui.utils.formatCurrency

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
                    text = stringResource(R.string.client_item_debt_label, formatCurrency(client.debtAmount)),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(onClick = onPayClick) {
                Text(stringResource(R.string.client_item_pay_button))
            }
        }
    }
}
@Composable
fun PaymentDialog(client: Client, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.payment_dialog_title, client.name)) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.payment_dialog_amount_label)) },
                prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                visualTransformation = NumberTransformation()
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
                Text(stringResource(R.string.payment_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
