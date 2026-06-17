package com.example.vetfinance.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddClientScreen(
    viewModel: VetViewModel,
    navController: NavController
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
