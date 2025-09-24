package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.vetfinance.data.Client
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun DebtClientsScreen(viewModel: VetViewModel, navController: NavController) {
    // Se obtienen los datos paginados y los estados del ViewModel
    val clientsWithDebt = viewModel.debtClientsPaginated.collectAsLazyPagingItems()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()

    // Limpia la búsqueda al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearClientSearchQuery()
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

            // Barra de búsqueda que filtra la lista paginada
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

            // Lista paginada de clientes
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(count = clientsWithDebt.itemCount) { index ->
                    clientsWithDebt[index]?.let { client ->
                        ClientItem(
                            client = client,
                            onPayClick = { viewModel.onShowPaymentDialog(client) },
                            onItemClick = { navController.navigate("client_detail/${client.clientId}") }
                        )
                    }
                }

                // Manejo de estados de carga de Paging para mostrar indicadores
                clientsWithDebt.loadState.let { loadState ->
                    if (loadState.refresh is LoadState.Loading) {
                        item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    }
                    if (loadState.append is LoadState.Loading) {
                        item { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
                    }
                    if (loadState.refresh is LoadState.NotLoading && clientsWithDebt.itemCount == 0) {
                        item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Ningún cliente coincide o no hay deudas.") } }
                    }
                }
            }
        }
    }
}

/**
 * Componente que muestra la información de un cliente en una tarjeta.
 */
@Composable
fun ClientItem(
    client: Client,
    onPayClick: () -> Unit,
    onItemClick: () -> Unit
) {
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
                Text(client.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                client.phone?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                val formattedDebt = String.format("₲ %,.0f", client.debtAmount).replace(",", ".")
                Text(formattedDebt, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onPayClick, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Pagar")
                }
            }
        }
    }
}

/**
 * Diálogo para registrar un pago para un cliente específico.
 */
@Composable
fun PaymentDialog(
    client: Client,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val formattedDebt = String.format("%,.0f", client.debtAmount).replace(",", ".")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pago") },
        text = {
            Column {
                Text("Cliente: ${client.name}")
                Text("Deuda actual: ₲$formattedDebt")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto a pagar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() ?: 0.0 > 0
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}