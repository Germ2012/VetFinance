package com.example.vetfinance.ui.screens

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vetfinance.viewmodel.ClientEntryViewModel

@Composable
fun AddClientScreen(
    navController: NavController,
    viewModel: ClientEntryViewModel = hiltViewModel()
) {
    val clientSuggestions by viewModel.clientNameSuggestions.collectAsStateWithLifecycle()

    AddOrEditClientDialog(
        onDismiss = {
            navController.popBackStack()
        },
        onConfirm = { name, phone, debt ->
            viewModel.addClient(name, phone, debt)
            navController.popBackStack()
        },
        showDebtField = true,
        clientSuggestions = clientSuggestions,
        onNameChange = { viewModel.onClientNameChange(it) },
        onSuggestionSelected = { viewModel.clearClientNameSuggestions() }
    )
}
