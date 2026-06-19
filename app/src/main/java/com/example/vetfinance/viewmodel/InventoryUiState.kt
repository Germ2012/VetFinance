package com.example.vetfinance.viewmodel

import androidx.compose.runtime.Immutable
import com.example.vetfinance.data.AppSettings
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.Supplier

@Immutable
data class InventoryScreenUiState(
    val showAddProductDialog: Boolean = false,
    val filter: String = "Todos",
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val productCount: Int = 0,
    val serviceCount: Int = 0,
    val lowStockCount: Int = 0,
    val suppliers: List<Supplier> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val productNameSuggestions: List<Product> = emptyList(),
    val isLoading: Boolean = true
)
