package com.example.vetfinance.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vetfinance.ui.screens.AddClientScreen
import com.example.vetfinance.ui.screens.AddPetScreen
import com.example.vetfinance.ui.screens.AddRestockScreen
import com.example.vetfinance.ui.screens.AddSaleScreen
import com.example.vetfinance.ui.screens.CalendarScreen
import com.example.vetfinance.ui.screens.ClientDetailScreen
import com.example.vetfinance.ui.screens.ClientsMenuScreen
import com.example.vetfinance.ui.screens.DashboardScreen
import com.example.vetfinance.ui.screens.DebtClientsScreen
import com.example.vetfinance.ui.screens.InventoryScreen
import com.example.vetfinance.ui.screens.PetDetailScreen
import com.example.vetfinance.ui.screens.PetsScreen
import com.example.vetfinance.ui.screens.ReportsScreen
import com.example.vetfinance.ui.screens.RestockScreen
import com.example.vetfinance.ui.screens.SalesScreen
import com.example.vetfinance.ui.screens.SettingsScreen
import com.example.vetfinance.ui.screens.SuppliersScreen
import com.example.vetfinance.viewmodel.VetViewModel

@Composable
fun AppNavigation(navController: NavHostController, viewModel: VetViewModel) {
    val operationErrorMessage by viewModel.operationErrorMessage.collectAsState()
    val operationSuccessMessage by viewModel.operationSuccessMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationSuccessMessage) {
        operationSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationSuccessMessage()
        }
    }

    operationErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearOperationErrorMessage() },
            title = { Text("Atencion") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearOperationErrorMessage() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Box {
        NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel, navController) }
            composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
            composable(Screen.Sales.route) { SalesScreen(viewModel, navController) }
            composable(Screen.Reports.route) { ReportsScreen(viewModel) }
            composable(Screen.Inventory.route) { InventoryScreen(viewModel) }
            composable(Screen.Clients.route) { ClientsMenuScreen(navController) }

            composable(Screen.AddSale.route) { AddSaleScreen(viewModel, navController) }
            composable(Screen.DebtClients.route) { DebtClientsScreen(viewModel, navController) }
            composable(
                route = Screen.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                ClientDetailScreen(viewModel, clientId, navController)
            }

            composable(Screen.Pets.route) { PetsScreen(viewModel, navController) }
            composable(
                route = Screen.PetDetail.route,
                arguments = listOf(navArgument("petId") { type = NavType.StringType })
            ) { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId") ?: ""
                PetDetailScreen(viewModel, petId, navController)
            }

            composable("add_client_screen") { AddClientScreen(viewModel, navController) }
            composable("add_pet_screen") { AddPetScreen(viewModel, navController) }
            composable(Screen.Suppliers.route) { SuppliersScreen(viewModel, navController) }
            composable(Screen.Restock.route) { RestockScreen(viewModel, navController) }
            composable(Screen.AddRestock.route) { AddRestockScreen(viewModel, navController) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel, navController) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
