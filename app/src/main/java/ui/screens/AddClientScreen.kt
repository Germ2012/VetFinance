package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Añadir Nuevo Cliente") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nombre del cliente") },
                isError = nameError,
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = debt,
                onValueChange = { debt = it.filter { char -> char.isDigit() } },
                label = { Text("Deuda Inicial (Opcional)") },
                prefix = { Text("₲") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val debtAmount = debt.toDoubleOrNull() ?: 0.0
                        viewModel.addClient(name, phone, debtAmount)
                        navController.popBackStack()
                    } else {
                        nameError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cliente")
            }
        }
    }
}