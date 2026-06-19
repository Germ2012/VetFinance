package com.example.vetfinance.ui.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.vetfinance.data.AppSettings

@Composable
fun SecurityPinDialog(
    settings: AppSettings,
    actionLabel: String,
    onDismiss: () -> Unit,
    onAuthorized: () -> Unit
) {
    val requiredPin = settings.securityPin
    if (requiredPin.isBlank()) {
        LaunchedEffect(Unit) {
            onAuthorized()
        }
        return
    }

    var pin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar accion") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter { char -> char.isDigit() }.take(6)
                    hasError = false
                },
                label = { Text("PIN requerido para $actionLabel") },
                isError = hasError,
                supportingText = {
                    if (hasError) {
                        Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Esta capa evita cambios accidentales.")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == requiredPin) {
                        onAuthorized()
                    } else {
                        hasError = true
                    }
                },
                enabled = pin.isNotBlank()
            ) {
                Text("Autorizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
