package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_ADJUSTMENT
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_INITIAL
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_PAYMENT
import com.example.vetfinance.data.ClientDebtHistory
import com.example.vetfinance.data.Payment
import com.example.vetfinance.viewmodel.VetViewModel
import ui.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: VetViewModel,
    clientId: String,
    navController: NavController
) {
    if (clientId.isBlank()) return
    LaunchedEffect(key1 = clientId) {
        viewModel.loadPaymentsForClient(clientId)
        viewModel.loadDebtHistoryForClient(clientId)
    }

    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val paymentHistory by viewModel.paymentHistory.collectAsStateWithLifecycle()
    val debtHistory by viewModel.debtHistory.collectAsStateWithLifecycle()
    val client = clients.find { it.clientId == clientId }
    val sortedDebtHistory = remember(debtHistory) { debtHistory.sortedByDescending { it.eventDate } }
    val sortedPayments = remember(paymentHistory) { paymentHistory.sortedByDescending { it.paymentDate } }
    val totalPayments = remember(paymentHistory) { paymentHistory.sumOf { it.amount } }
    val totalDebtIncreases = remember(debtHistory) {
        debtHistory.filter { it.amountChange > 0.0 }.sumOf { it.amountChange }
    }
    val lastPaymentDate = remember(sortedPayments) { sortedPayments.firstOrNull()?.paymentDate }
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

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
            if (client == null) {
                item {
                    Text("Cliente no encontrado", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                item {
                    ClientStatementHeader(
                        name = client.name,
                        phone = client.phone,
                        currentDebt = client.debtAmount
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClientMiniMetric(
                            label = "Pagado",
                            value = "Gs. ${formatCurrency(totalPayments)}",
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        ClientMiniMetric(
                            label = "Cargos",
                            value = "Gs. ${formatCurrency(totalDebtIncreases)}",
                            icon = Icons.Default.WarningAmber,
                            modifier = Modifier.weight(1f),
                            isWarning = totalDebtIncreases > totalPayments
                        )
                    }
                }
                item {
                    ClientMiniMetric(
                        label = "Ultimo pago",
                        value = lastPaymentDate?.let { sdf.format(Date(it)) } ?: "Sin pagos",
                        icon = Icons.Default.CheckCircle
                    )
                }
                item {
                    Text(
                        "Estado de cuenta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (sortedDebtHistory.isEmpty()) {
                    item {
                        StatementEmptyState()
                    }
                } else {
                    items(sortedDebtHistory, key = { it.historyId }) { item ->
                        AccountStatementRow(item = item)
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
private fun StatementEmptyState() {
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
            Text(stringResource(R.string.client_detail_no_debt_history_message), style = MaterialTheme.typography.bodyMedium)
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

@Composable
fun PaymentItem(payment: Payment) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val date = Date(payment.paymentDate)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(sdf.format(date), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.text_prefix_gs) + " " + formatCurrency(payment.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DebtHistoryItem(item: ClientDebtHistory) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val title = when (item.eventType) {
        CLIENT_DEBT_EVENT_INITIAL -> stringResource(R.string.debt_history_initial)
        CLIENT_DEBT_EVENT_PAYMENT -> stringResource(R.string.debt_history_payment)
        CLIENT_DEBT_EVENT_ADJUSTMENT -> stringResource(R.string.debt_history_adjustment)
        else -> item.eventType
    }
    val sign = if (item.amountChange >= 0) "+" else "-"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$title - ${sdf.format(Date(item.eventDate))}", fontWeight = FontWeight.Bold)
            Text("$sign ${stringResource(R.string.text_prefix_gs)} ${formatCurrency(kotlin.math.abs(item.amountChange))}")
            Text(stringResource(R.string.debt_history_balance_after, formatCurrency(item.balanceAfter)))
            item.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
    }
}
