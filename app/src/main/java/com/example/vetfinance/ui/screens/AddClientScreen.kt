package com.example.vetfinance.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun AddClientScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    AddOrEditClientDialog(
        onDismiss = {
            navController.popBackStack()
        },
        onConfirm = { name, phone, debt ->
            viewModel.addClient(name, phone, debt)
            navController.popBackStack()
        },
        showDebtField = true
    )
}
