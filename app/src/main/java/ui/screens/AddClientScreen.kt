package com.example.vetfinance.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun AddClientScreen(
    viewModel: VetViewModel,
    navController: NavController
) {
    // Este composable ahora actúa como un anfitrión para el diálogo.
    // Se muestra el diálogo inmediatamente al navegar a esta pantalla.
    AddOrEditClientDialog(
        onDismiss = {
            // Al descartar, simplemente regresa a la pantalla anterior.
            navController.popBackStack()
        },
        onConfirm = { name, phone, debt ->
            // Al confirmar, añade el cliente y luego regresa.
            viewModel.addClient(name, phone, debt)
            navController.popBackStack()
        },
        showDebtField = true // Para clientes nuevos desde la lista, se muestra el campo de deuda.
    )
}
