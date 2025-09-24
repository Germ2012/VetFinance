package com.example.vetfinance.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define todas las rutas de navegación de la aplicación.
 * Se usa una sealed class para asegurar que solo se puedan usar las rutas aquí definidas.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Pantallas principales que aparecen en la barra de navegación inferior
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Sales : Screen("sales", "Ventas", Icons.Default.PointOfSale)
    object Reports : Screen("reports", "Reportes", Icons.Default.Assessment)
    object Inventory : Screen("inventory", "Inventario", Icons.Default.Inventory)
    object Clients : Screen("clients", "Clientes", Icons.Default.People)
    object Calendar : Screen("calendar", "Calendario", Icons.Default.CalendarMonth)

    // Pantallas secundarias a las que se navega desde otras partes de la app
    object AddSale : Screen("add_sale", "Nueva Venta", Icons.Default.Add)
    object ClientDetail : Screen("client_detail/{clientId}", "Detalle de Cliente", Icons.AutoMirrored.Filled.ReceiptLong)
    object DebtClients : Screen("debt_clients", "Clientes con Deuda", Icons.Default.People)
    object Pets : Screen("pets", "Mascotas", Icons.Default.Pets)
    object PetDetail : Screen("pet_detail/{petId}", "Detalle de Mascota", Icons.Default.Pets)
}