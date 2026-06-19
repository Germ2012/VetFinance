package com.example.vetfinance.viewmodel

import androidx.compose.runtime.Immutable
import com.example.vetfinance.data.AppSettings
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.DebtCollectionSummary

@Immutable
data class DebtCollectionFilters(
    val showOnlyWithDebt: Boolean = true,
    val minimumDebt: Double = 0.0,
    val sortMode: String = "Mayor deuda"
)

@Immutable
data class DebtClientsScreenUiState(
    val searchQuery: String = "",
    val showPaymentDialog: Boolean = false,
    val clientForPayment: Client? = null,
    val isLoading: Boolean = true,
    val appSettings: AppSettings = AppSettings(),
    val collectionSummary: DebtCollectionSummary = DebtCollectionSummary(0, 0.0, 0.0)
)
