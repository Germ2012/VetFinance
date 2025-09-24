package com.example.vetfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vetfinance.navigation.AppNavigation
import com.example.vetfinance.navigation.Screen
import com.example.vetfinance.ui.theme.VetFinanceTheme
import com.example.vetfinance.viewmodel.VetViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VetFinanceTheme {
                MainScreen(viewModel = hiltViewModel<VetViewModel>())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VetViewModel) {
    val navController = rememberNavController()
    // Se definen los ítems que aparecerán en la barra de navegación inferior,
    // incluyendo la pantalla de Calendario.
    val navItems = listOf(
        Screen.Dashboard,
        Screen.Calendar,
        Screen.Sales,
        Screen.Reports,
        Screen.Inventory,
        Screen.Clients
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Evita acumular una gran pila de destinos
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Evita volver a lanzar el mismo destino si ya está en la cima
                                launchSingleTop = true
                                // Restaura el estado al volver a seleccionar un ítem
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // AppNavigation contiene el NavHost que renderiza la pantalla actual.
            AppNavigation(navController = navController, viewModel = viewModel)
        }
    }
}