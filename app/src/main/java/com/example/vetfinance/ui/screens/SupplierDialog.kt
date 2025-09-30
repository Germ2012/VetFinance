package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vetfinance.R
import com.example.vetfinance.data.Supplier

@Composable
fun SupplierDialog(
    supplier: Supplier?, // Null when adding a new supplier
    onDismiss: () -> Unit,
    onConfirm: (Supplier) -> Unit
) {
    var name by remember(supplier) { mutableStateOf(supplier?.name ?: "") }
    var contactPerson by remember(supplier) { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember(supplier) { mutableStateOf(supplier?.phone ?: "") }
    var email by remember(supplier) { mutableStateOf(supplier?.email ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (supplier == null) stringResource(R.string.dialog_title_add_supplier) 
                           else stringResource(R.string.dialog_title_edit_supplier, supplier.name), 
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text(stringResource(R.string.label_supplier_name)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text(stringResource(R.string.label_supplier_contact_person)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.label_supplier_phone)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.label_supplier_email)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isBlank()) {
                            nameError = stringResource(R.string.error_supplier_name_required) 
                        } else {
                            val newOrUpdatedSupplier = supplier?.copy(
                                name = name.trim(),
                                contactPerson = contactPerson.trim().ifBlank { null },
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null }
                            ) ?: Supplier(
                                name = name.trim(),
                                contactPerson = contactPerson.trim().ifBlank { null },
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null }
                            )
                            onConfirm(newOrUpdatedSupplier)
                        }
                    }) {
                        Text(stringResource(R.string.button_confirm))
                    }
                }
            }
        }
    }
}