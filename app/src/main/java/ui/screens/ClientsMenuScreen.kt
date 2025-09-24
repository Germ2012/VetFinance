package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.navigation.Screen

@Composable
fun ClientsMenuScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Gestión", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            MenuItem(
                title = "Clientes con Deuda",
                onClick = { navController.navigate(Screen.DebtClients.route) }
            )
        }
        item {
            MenuItem(
                title = "Mascotas",
                onClick = { navController.navigate(Screen.Pets.route) }
            )
        }
    }
}

@Composable
fun MenuItem(title: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}