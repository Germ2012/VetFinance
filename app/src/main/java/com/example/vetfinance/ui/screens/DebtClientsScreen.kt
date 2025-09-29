package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Client
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency

@Composable
fun DebtClientsScreen(viewModel: VetViewModel, navController: NavController) {
    val allClients by viewModel.clients.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()

    var showOnlyWithDebt by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    val isLoading = allClients.isEmpty() && searchQuery.isBlank() && !showOnlyWithDebt

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearClientSearchQuery()
        }
    }

    val filteredClients = remember(allClients, searchQuery, showOnlyWithDebt) {
        val clients = if (showOnlyWithDebt) {
            allClients.filter { it.debtAmount > 0 }
        } else {
            allClients
        }
        clients.filter { client ->
            if (searchQuery.isBlank()) true
            else client.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val currentClientForPayment = clientForPayment
    if (showPaymentDialog && currentClientForPayment != null) {
        PaymentDialog(
            client = currentClientForPayment, // Smart-cast to non-null
            onDismiss = { viewModel.onDismissPaymentDialog() },
            onConfirm = { amount -> viewModel.makePayment(amount) }
        )
    }

    clientToDelete?.let { client ->
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text(stringResource(R.string.confirm_deletion_title)) },
            text = { Text(stringResource(R.string.debt_clients_confirm_delete_message, client.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(client)
                        clientToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_client_screen") }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.debt_clients_add_client_fab))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.debt_clients_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onClientSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.debt_clients_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, 
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearClientSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search_content_description))
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(stringResource(R.string.debt_clients_show_only_with_debt_switch))
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = showOnlyWithDebt,
                    onCheckedChange = { showOnlyWithDebt = it }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredClients, key = { it.clientId }) { client -> 
                        ClientItem(
                            client = client,
                            onDetailClick = { navController.navigate("client_detail/${client.clientId}") },
                            onPayClick = { viewModel.onShowPaymentDialog(client) },
                            onDeleteClick = { clientToDelete = client }
                        )
                    }

                    if (filteredClients.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val message = when {
                                    searchQuery.isNotBlank() -> stringResource(R.string.debt_clients_empty_search_message)
                                    showOnlyWithDebt -> stringResource(R.string.debt_clients_empty_debt_message)
                                    else -> stringResource(R.string.debt_clients_empty_clients_message)
                                }
                                Text(message)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientItem(
    client: Client,
    onDetailClick: () -> Unit,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.client_item_debt_label, formatCurrency(client.debtAmount)), style = MaterialTheme.typography.bodyMedium)
            }
            if (client.debtAmount > 0) {
                TextButton(onClick = onPayClick) {
                    Text(stringResource(R.string.client_item_pay_button))
                }
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_content_description))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.debt_clients_view_details_menu_item)) },
                        onClick = {
                            onDetailClick()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_button)) },
                        onClick = {
                            onDeleteClick()
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}