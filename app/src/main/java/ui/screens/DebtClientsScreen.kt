package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun DebtClientsScreen(viewModel: VetViewModel, navController: NavController) {
    // --- Estados del ViewModel ---
    val allClients by viewModel.clients.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()

    // Limpia la búsqueda al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearClientSearchQuery()
        }
    }

    // --- Lógica de filtrado en el Composable ---
    val clientsWithDebt = remember(allClients, searchQuery) {
        allClients
            .filter { it.debtAmount > 0 } // 1. Filtra solo clientes con deuda
            .filter { client -> // 2. Filtra por la búsqueda
                if (searchQuery.isBlank()) true
                else client.name.contains(searchQuery, ignoreCase = true)
            }
    }

    // Muestra el diálogo de pago si es necesario
    if (showPaymentDialog && clientForPayment != null) {
        PaymentDialog(
            client = clientForPayment!!,
            onDismiss = { viewModel.onDismissPaymentDialog() },
            onConfirm = { amount -> viewModel.makePayment(amount) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_client_screen") }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Cliente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Clientes con Deuda",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onClientSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearClientSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Lista de clientes filtrada
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clientsWithDebt) { client ->
                    ClientItem(
                        client = client,
                        onPayClick = { viewModel.onShowPaymentDialog(client) },
                        onItemClick = { navController.navigate("client_detail/${client.clientId}") }
                    )
                }

                if (clientsWithDebt.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ningún cliente coincide o no hay deudas.")
                        }
                    }
                }
            }
        }
    }
}