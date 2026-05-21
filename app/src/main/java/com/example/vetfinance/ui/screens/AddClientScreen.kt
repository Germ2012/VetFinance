package com.example.vetfinance.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun AddClientScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    val clientSuggestions by viewModel.clientNameSuggestions.collectAsState()

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
