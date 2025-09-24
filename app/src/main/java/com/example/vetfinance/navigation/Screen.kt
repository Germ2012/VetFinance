package com.example.vetfinance.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.automirrored.filled.ReceiptLong

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Sales : Screen("sales", "Ventas", Icons.Default.PointOfSale)
    object Reports : Screen("reports", "Reportes", Icons.Default.Assessment)
    object Inventory : Screen("inventory", "Inventario", Icons.Default.Inventory)
    object Clients : Screen("clients", "Clientes", Icons.Default.People)
    object AddSale : Screen("add_sale", "Nueva Venta", Icons.Default.Add)
    object ClientDetail : Screen("client_detail/{clientId}", "Detalle de Cliente", Icons.AutoMirrored.Filled.ReceiptLong)
    object DebtClients : Screen("debt_clients", "Clientes con Deuda", Icons.Default.People)
    object Pets : Screen("pets", "Mascotas", Icons.Default.Pets)
    object PetDetail : Screen("pet_detail/{petId}", "Detalle de Mascota", Icons.Default.Pets)
}