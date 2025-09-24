// ruta: app/src/main/java/com/example/vetfinance/navigation/AppNavigation.kt

package com.example.vetfinance.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vetfinance.ui.screens.AddClientScreen
import com.example.vetfinance.ui.screens.AddPetScreen
import com.example.vetfinance.ui.screens.PetDetailScreen
import com.example.vetfinance.ui.screens.PetsScreen
import com.example.vetfinance.ui.screens.AddSaleScreen
import com.example.vetfinance.ui.screens.ClientDetailScreen
import com.example.vetfinance.ui.screens.DebtClientsScreen
import com.example.vetfinance.ui.screens.DashboardScreen
import com.example.vetfinance.ui.screens.InventoryScreen
import com.example.vetfinance.ui.screens.ReportsScreen
import com.example.vetfinance.ui.screens.SalesScreen
import com.example.vetfinance.viewmodel.VetViewModel
import com.example.vetfinance.ui.screens.ClientsMenuScreen

@Composable
fun AppNavigation(navController: NavHostController, viewModel: VetViewModel) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        // --- Pantallas Principales ---
        // 👇 CORRECCIÓN: Se pasa el navController a DashboardScreen
        composable(Screen.Dashboard.route) { DashboardScreen(viewModel, navController) }

        composable(Screen.Sales.route) { SalesScreen(viewModel, navController) }
        composable(Screen.Reports.route) { ReportsScreen(viewModel) }
        composable(Screen.Inventory.route) { InventoryScreen(viewModel) }
        composable(Screen.AddSale.route) { AddSaleScreen(viewModel, navController) }

        // --- Pantallas de Clientes ---
        composable(Screen.Clients.route) { ClientsMenuScreen(navController) }
        composable(Screen.DebtClients.route) { DebtClientsScreen(viewModel, navController) }
        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?:""
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

        // --- Pantallas para Añadir ---
        composable("add_client_screen") {
            AddClientScreen(viewModel, navController)
        }

        composable("add_pet_screen") {
            AddPetScreen(viewModel, navController)
        }
    }
}