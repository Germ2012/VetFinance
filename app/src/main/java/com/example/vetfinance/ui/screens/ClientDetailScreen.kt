package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vetfinance.R
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_ADJUSTMENT
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_INITIAL
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_PAYMENT
import com.example.vetfinance.data.ClientDebtHistory
import com.example.vetfinance.data.ClientDebtHistorySummaryRow
import com.example.vetfinance.data.ClientPaymentSummaryRow
import com.example.vetfinance.data.Payment
import com.example.vetfinance.ui.utils.formatCurrency
import com.example.vetfinance.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    navController: NavController,
    viewModel: DebtViewModel = hiltViewModel()
) {
    if (clientId.isBlank()) return
    val clientFlow = remember(clientId) { viewModel.clientById(clientId) }
    val client by clientFlow.collectAsStateWithLifecycle(initialValue = null)
    val payments = remember(clientId) { viewModel.paymentsForClientPaginated(clientId) }.collectAsLazyPagingItems()
    val debtHistory = remember(clientId) { viewModel.debtHistoryForClientPaginated(clientId) }.collectAsLazyPagingItems()
    LaunchedEffect(payments, debtHistory) {
        viewModel.pagingRefreshEvents.collect {
            payments.refresh()
            debtHistory.refresh()
        }
    }
    val paymentSummaryFlow = remember(clientId) { viewModel.paymentSummaryForClient(clientId) }
    val debtSummaryFlow = remember(clientId) { viewModel.debtHistorySummaryForClient(clientId) }
    val paymentSummary by paymentSummaryFlow.collectAsStateWithLifecycle(initialValue = ClientPaymentSummaryRow())
    val debtSummary by debtSummaryFlow.collectAsStateWithLifecycle(initialValue = ClientDebtHistorySummaryRow())
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val currentClient = client

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.client_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (currentClient == null) {
                item {
                    Text("Cliente no encontrado", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                item {
                    ClientStatementHeader(
                        name = currentClient.name,
                        phone = currentClient.phone,
                        currentDebt = currentClient.debtAmount
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClientMiniMetric(
                            label = "Pagado",
                            value = "Gs. ${formatCurrency(paymentSummary.totalPaid)}",
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        ClientMiniMetric(
                            label = "Cargos",
                            value = "Gs. ${formatCurrency(debtSummary.debtIncreases)}",
                            icon = Icons.Default.WarningAmber,
                            modifier = Modifier.weight(1f),
                            isWarning = debtSummary.debtIncreases > paymentSummary.totalPaid
                        )
                    }
                }
                item {
                    ClientMiniMetric(
                        label = "Ultimo pago",
                        value = paymentSummary.lastPaymentDate?.let { sdf.format(Date(it)) } ?: "Sin pagos",
                        icon = Icons.Default.CheckCircle
                    )
                }
                item {
                    Text(
                        "Pagos recientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    payments.loadState.refresh is LoadState.Loading -> {
                        item { StatementLoadingRow() }
                    }
                    payments.loadState.refresh is LoadState.NotLoading && payments.itemCount == 0 -> {
                        item { StatementEmptyState("Sin pagos registrados") }
                    }
                    else -> {
                        items(
                            count = payments.itemCount,
                            key = payments.itemKey { it.paymentId },
                            contentType = { "payment" }
                        ) { index ->
                            payments[index]?.let { payment ->
                                PaymentStatementRow(payment = payment)
                            }
                        }
                        if (payments.loadState.append is LoadState.Loading) {
                            item { StatementLoadingRow() }
                        }
                    }
                }
                item {
                    Text(
                        "Estado de cuenta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    debtHistory.loadState.refresh is LoadState.Loading -> {
                        item { StatementLoadingRow() }
                    }
                    debtHistory.loadState.refresh is LoadState.NotLoading && debtHistory.itemCount == 0 -> {
                        item {
                            StatementEmptyState(stringResource(R.string.client_detail_no_debt_history_message))
                        }
                    }
                    else -> {
                        items(
                            count = debtHistory.itemCount,
                            key = debtHistory.itemKey { it.historyId },
                            contentType = { "debtHistory" }
                        ) { index ->
                            debtHistory[index]?.let { item ->
                                AccountStatementRow(item = item)
                            }
                        }
                        if (debtHistory.loadState.append is LoadState.Loading) {
                            item { StatementLoadingRow() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientStatementHeader(
    name: String,
    phone: String?,
    currentDebt: Double
) {
    val isDebtFree = currentDebt <= 0.0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDebtFree) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = if (isDebtFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(phone ?: "Sin telefono", style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.client_detail_current_debt_label_gs, formatCurrency(currentDebt)),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDebtFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ClientMiniMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatementEmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatementLoadingRow() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Cargando movimientos...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentStatementRow(payment: Payment) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pago recibido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(sdf.format(Date(payment.paymentDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "Gs. ${formatCurrency(payment.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AccountStatementRow(item: ClientDebtHistory) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val title = when (item.eventType) {
        CLIENT_DEBT_EVENT_INITIAL -> stringResource(R.string.debt_history_initial)
        CLIENT_DEBT_EVENT_PAYMENT -> stringResource(R.string.debt_history_payment)
        CLIENT_DEBT_EVENT_ADJUSTMENT -> stringResource(R.string.debt_history_adjustment)
        else -> item.eventType
    }
    val isPositive = item.amountChange >= 0.0
    val amountColor = if (isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val sign = if (isPositive) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(sdf.format(Date(item.eventDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "$sign Gs. ${formatCurrency(kotlin.math.abs(item.amountChange))}",
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                stringResource(R.string.debt_history_balance_after, formatCurrency(item.balanceAfter)),
                style = MaterialTheme.typography.bodyMedium
            )
            item.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
