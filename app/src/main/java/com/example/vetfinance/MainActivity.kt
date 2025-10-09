
package com.example.vetfinance


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

    // --- Inicio del código de permisos ---

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Permiso de notificaciones CONCEDIDO")
        } else {
            println("Permiso de notificaciones DENEGADO")
        }
    }

    // 2. Función que verifica y solicita el permiso si es necesario.
    private fun askNotificationPermission() {
        // Esta comprobación solo es necesaria para Android 13 (API nivel 33) y superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Se comprueba si el permiso ya ha sido concedido.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Si no está concedido, se lanza el diálogo de solicitud de permiso.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Se llama a la función para pedir el permiso justo al crear la actividad.
        askNotificationPermission()

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
                               popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                               launchSingleTop = true
                               restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(navController = navController, viewModel = viewModel)
        }
    }
}