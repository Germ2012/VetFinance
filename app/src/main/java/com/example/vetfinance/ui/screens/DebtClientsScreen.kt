package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Client
import com.example.vetfinance.viewmodel.DebtCollectionRow
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency // Importar formatCurrency
import ui.utils.NumberTransformation

@Composable
fun DebtClientsScreen(viewModel: VetViewModel, navController: NavController) {
    val allClients by viewModel.clients.collectAsState()
    val pendingCollectionRows by viewModel.pendingCollectionRows.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val clientForPayment by viewModel.clientForPayment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showOnlyWithDebt by remember { mutableStateOf(true) }
    var minimumDebtText by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("Mayor deuda") }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var clientToAdjustDebt by remember { mutableStateOf<Client?>(null) }
    val sortOptions = remember { listOf("Mayor deuda", "Menor deuda", "Nombre") }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearClientSearchQuery()
        }
    }

    val collectionRows = remember(allClients, pendingCollectionRows) {
        val pendingByClientId = pendingCollectionRows.associateBy { it.client.clientId }
        allClients.map { client ->
            pendingByClientId[client.clientId] ?: DebtCollectionRow(
                client = client,
                totalSold = 0.0,
                totalPaid = 0.0,
                balance = client.debtAmount
            )
        }
    }

    val filteredRows = remember(collectionRows, pendingCollectionRows, searchQuery, showOnlyWithDebt, minimumDebtText, selectedSort) {
        val minimumDebt = minimumDebtText.toDoubleOrNull() ?: 0.0
        val baseRows = if (showOnlyWithDebt) {
            pendingCollectionRows
        } else {
            collectionRows
        }
        val filtered = baseRows.filter { row ->
            val matchesSearch = searchQuery.isBlank() ||
                    row.client.name.contains(searchQuery, ignoreCase = true) ||
                    row.client.phone.orEmpty().contains(searchQuery, ignoreCase = true)
            matchesSearch && row.balance >= minimumDebt
        }
        when (selectedSort) {
            "Menor deuda" -> filtered.sortedBy { it.balance }
            "Nombre" -> filtered.sortedBy { it.client.name.lowercase() }
            else -> filtered.sortedByDescending { it.balance }
        }
    }
    val totalPending = remember(filteredRows) { filteredRows.sumOf { it.balance } }
    val totalPaid = remember(filteredRows) { filteredRows.sumOf { it.totalPaid } }

    val currentClientForPayment = clientForPayment
    if (showPaymentDialog && currentClientForPayment != null) {
        PaymentDialog(
            client = currentClientForPayment,
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

    clientToAdjustDebt?.let { client ->
        DebtAdjustmentDialog(
            client = client,
            onDismiss = { clientToAdjustDebt = null },
            onConfirm = { newDebt, note ->
                viewModel.adjustClientDebt(client, newDebt, note)
                clientToAdjustDebt = null
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(
                text = "Filtra, cobra y consulta el historial de cada cliente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            CollectionSummaryCard(
                clientCount = filteredRows.size,
                totalPending = totalPending,
                totalPaid = totalPaid
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.debt_clients_show_only_with_debt_switch))
                        Switch(
                            checked = showOnlyWithDebt,
                            onCheckedChange = { showOnlyWithDebt = it }
                        )
                    }
                    OutlinedTextField(
                        value = minimumDebtText,
                        onValueChange = { minimumDebtText = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Deuda m\u00ednima") },
                        prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                        visualTransformation = NumberTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortOptions.forEach { option ->
                            FilterChip(
                                selected = selectedSort == option,
                                onClick = { selectedSort = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredRows, key = { it.client.clientId }) { row ->
                        val client = row.client
                        ClientItem(
                            row = row,
                            onDetailClick = { navController.navigate("client_detail/${client.clientId}") },
                            onPayClick = { viewModel.onShowPaymentDialog(client) },
                            onAdjustDebtClick = { clientToAdjustDebt = client },
                            onDeleteClick = { clientToDelete = client }
                        )
                    }

                    if (filteredRows.isEmpty()) {
                        item {
                            val message = when {
                                searchQuery.isNotBlank() -> stringResource(R.string.debt_clients_empty_search_message)
                                showOnlyWithDebt -> stringResource(R.string.debt_clients_empty_debt_message)
                                else -> stringResource(R.string.debt_clients_empty_clients_message)
                            }
                            DebtClientsEmptyState(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSummaryCard(
    clientCount: Int,
    totalPending: Double,
    totalPaid: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cobros pendientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryValue(
                    label = "Saldo",
                    value = "${stringResource(R.string.text_prefix_gs)} ${formatCurrency(totalPending)}",
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Clientes",
                    value = clientCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Pagado",
                    value = "${stringResource(R.string.text_prefix_gs)} ${formatCurrency(totalPaid)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ClientItem(
    row: DebtCollectionRow,
    onDetailClick: () -> Unit,
    onPayClick: () -> Unit,
    onAdjustDebtClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val client = row.client

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    client.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                client.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Text(phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.client_item_debt_label, formatCurrency(row.balance)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Vendido: ${stringResource(R.string.text_prefix_gs)} ${formatCurrency(row.totalSold)} - Pagado: ${stringResource(R.string.text_prefix_gs)} ${formatCurrency(row.totalPaid)}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (row.balance > 0) {
                FilledTonalButton(onClick = onPayClick, shape = RoundedCornerShape(8.dp)) {
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
                        text = { Text(stringResource(R.string.debt_clients_adjust_debt_menu_item)) },
                        onClick = {
                            onAdjustDebtClick()
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

@Composable
private fun DebtClientsEmptyState(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DebtAdjustmentDialog(
    client: Client,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var debt by remember(client) { mutableStateOf(client.debtAmount.toLong().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_adjustment_title, client.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = debt,
                    onValueChange = { debt = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.debt_adjustment_amount_label)) },
                    prefix = { Text(stringResource(R.string.text_prefix_gs)) },
                    visualTransformation = NumberTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_adjustment_note_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(debt.toDoubleOrNull() ?: 0.0, note.ifBlank { null }) }
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
