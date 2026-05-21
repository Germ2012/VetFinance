package com.example.vetfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    val clients by viewModel.clients.collectAsState()
    val paymentHistory by viewModel.paymentHistory.collectAsState()
    val debtHistory by viewModel.debtHistory.collectAsState()
    val client = clients.find { it.clientId == clientId }

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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            client?.let {
                Text(it.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.client_detail_current_debt_label_gs, formatCurrency(it.debtAmount)),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (it.debtAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(stringResource(R.string.client_detail_payments_made_title), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (paymentHistory.isEmpty()) {
                Text(stringResource(R.string.client_detail_no_payments_message))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paymentHistory) { payment ->
                        PaymentItem(payment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.client_detail_debt_history_title), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (debtHistory.isEmpty()) {
                Text(stringResource(R.string.client_detail_no_debt_history_message))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(debtHistory) { item ->
                        DebtHistoryItem(item)
                    }
                }
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
