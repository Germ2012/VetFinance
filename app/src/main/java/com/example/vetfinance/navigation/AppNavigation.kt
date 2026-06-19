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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.vetfinance.viewmodel.AppMessagesViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    appMessagesViewModel: AppMessagesViewModel = hiltViewModel()
) {
    val operationErrorMessage by appMessagesViewModel.operationErrorMessage.collectAsStateWithLifecycle()
    val operationSuccessMessage by appMessagesViewModel.operationSuccessMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationSuccessMessage) {
        operationSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            appMessagesViewModel.clearOperationSuccessMessage()
        }
    }

    operationErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { appMessagesViewModel.clearOperationErrorMessage() },
            title = { Text("Atencion") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { appMessagesViewModel.clearOperationErrorMessage() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Box {
        NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Sales.route) { SalesScreen(navController) }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.Inventory.route) { InventoryScreen() }
            composable(Screen.Clients.route) { ClientsMenuScreen(navController) }

            composable(Screen.AddSale.route) { AddSaleScreen(navController) }
            composable(Screen.DebtClients.route) { DebtClientsScreen(navController) }
            composable(
                route = Screen.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                ClientDetailScreen(clientId, navController)
            }

            composable(Screen.Pets.route) { PetsScreen(navController) }
            composable(
                route = Screen.PetDetail.route,
                arguments = listOf(navArgument("petId") { type = NavType.StringType })
            ) { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId") ?: ""
                PetDetailScreen(petId, navController)
            }

            composable("add_client_screen") { AddClientScreen(navController) }
            composable("add_pet_screen") { AddPetScreen(navController) }
            composable(Screen.Suppliers.route) { SuppliersScreen(navController) }
            composable(Screen.Restock.route) { RestockScreen(navController) }
            composable(Screen.AddRestock.route) { AddRestockScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
