package com.example.vetfinance.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vetfinance.ui.screens.*
import com.example.vetfinance.viewmodel.VetViewModel
import ui.screens.CalendarScreen

@Composable
fun AppNavigation(navController: NavHostController, viewModel: VetViewModel) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        // --- Pantallas Principales (desde la barra de navegación) ---
        composable(Screen.Dashboard.route) { DashboardScreen(viewModel, navController) }
        composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
        composable(Screen.Sales.route) { SalesScreen(viewModel, navController) }
        composable(Screen.Reports.route) { ReportsScreen(viewModel) }
        composable(Screen.Inventory.route) { InventoryScreen(viewModel) }
        composable(Screen.Clients.route) { ClientsMenuScreen(navController) }

        // --- Pantallas Secundarias y de Detalles ---
        composable(Screen.AddSale.route) { AddSaleScreen(viewModel, navController) }
        composable(Screen.DebtClients.route) { DebtClientsScreen(viewModel, navController) }
        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ClientDetailScreen(viewModel, clientId, navController)
        }

        // --- Pantallas de Mascotas ---
        composable(Screen.Pets.route) { PetsScreen(viewModel, navController) }
        composable(
            route = Screen.PetDetail.route,
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(viewModel, petId, navController)
        }

        // --- Pantallas para Añadir Entidades ---
        composable("add_client_screen") { AddClientScreen(viewModel, navController) }
        composable("add_pet_screen") { AddPetScreen(viewModel, navController) }
    }
}