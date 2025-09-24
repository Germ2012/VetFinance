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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.data.Payment
import com.example.vetfinance.viewmodel.VetViewModel
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
    // Carga los pagos para este cliente específico una sola vez
    LaunchedEffect(key1 = clientId) {
        viewModel.loadPaymentsForClient(clientId)
    }

    val clients by viewModel.clients.collectAsState()
    val paymentHistory by viewModel.paymentHistory.collectAsState()
    val client = clients.find { it.clientId == clientId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Pagos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                    String.format("Deuda Actual: Gs %,.0f", it.debtAmount).replace(",", "."),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (it.debtAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Pagos Realizados", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (paymentHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Este cliente no ha realizado ningún pago.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paymentHistory) { payment ->
                        PaymentItem(payment)
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
                text = String.format("Gs %,.0f", payment.amountPaid).replace(",", "."),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}