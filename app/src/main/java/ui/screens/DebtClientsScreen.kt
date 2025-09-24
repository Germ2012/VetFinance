// ruta: app/src/main/java/com/example/vetfinance/ui/screens/DebtClientsScreen.kt

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
    // Se obtienen los clientes filtrados y la consulta de búsqueda del ViewModel
    val clientsWithDebt by viewModel.filteredDebtClients.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()

    // Estados para el diálogo de pago
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()

    // Limpia la búsqueda al salir de la pantalla para no afectar la próxima vez que se entre
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearClientSearchQuery()
        }
    }

    // Diálogo para registrar un pago
    if (showPaymentDialog && clientForPayment != null) {
        PaymentDialog(
            client = clientForPayment!!,
            onDismiss = { viewModel.onDismissPaymentDialog() },
            onConfirm = { amount -> viewModel.makePayment(amount) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navega a la pantalla para añadir un nuevo cliente
                    navController.navigate("add_client_screen")
                }
            ) {
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

            // Barra de búsqueda para filtrar clientes por nombre
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

            // Muestra la lista de clientes o un mensaje si no hay resultados
            if (clientsWithDebt.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ningún cliente coincide con la búsqueda o no hay deudas.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clientsWithDebt) { client ->
                        ClientItem(
                            client = client,
                            onPayClick = { viewModel.onShowPaymentDialog(client) },
                            onItemClick = {
                                navController.navigate("client_detail/${client.clientId}")
                            }
                        )
                    }
                }
            }
        }
    }
}