package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.viewmodel.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val showSupplierDialog by viewModel.showSupplierDialog.collectAsState()
    val editingSupplier by viewModel.editingSupplier.collectAsState()

    if (showSupplierDialog) {
        SupplierDialog(
            supplier = editingSupplier,
            onDismiss = { viewModel.onDismissSupplierDialog() },
            onConfirm = { supplier ->
                viewModel.addOrUpdateSupplier(supplier)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_suppliers)) }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowSupplierDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_supplier))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (suppliers.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.suppliers_empty_list), 
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else {
                items(suppliers, key = { it.supplierId }) { supplier ->
                    SupplierListItem(
                        supplier = supplier,
                        onClick = { viewModel.onShowSupplierDialog(supplier) }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierListItem(
    supplier: Supplier,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = supplier.name, style = MaterialTheme.typography.titleMedium)
            supplier.contactPerson?.takeIf { it.isNotBlank() }?.let {
                Text(text = stringResource(R.string.supplier_contact_person_label, it), style = MaterialTheme.typography.bodyMedium) 
            }
            supplier.phone?.takeIf { it.isNotBlank() }?.let {
                Text(text = stringResource(R.string.supplier_phone_label, it), style = MaterialTheme.typography.bodyMedium) 
            }
            supplier.email?.takeIf { it.isNotBlank() }?.let {
                Text(text = stringResource(R.string.supplier_email_label, it), style = MaterialTheme.typography.bodyMedium) 
            }
        }
    }
}