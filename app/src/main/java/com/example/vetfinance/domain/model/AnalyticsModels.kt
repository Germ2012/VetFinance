package com.example.vetfinance.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class GlobalSearchResult(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String
)

@Immutable
data class CashClosingSummary(
    val salesCount: Int,
    val salesTotal: Double,
    val paymentsTotal: Double,
    val debtIncreases: Double,
    val debtAdjustments: Double,
    val operationalTotal: Double
)

@Immutable
data class ProductProfitReport(
    val productId: String,
    val name: String,
    val isService: Boolean,
    val quantitySold: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double,
    val marginPercent: Double
)

@Immutable
data class ClientPurchaseReport(
    val clientName: String,
    val clientId: String,
    val totalPurchased: Double,
    val saleCount: Int
)

@Immutable
data class FinancialSummary(
    val salesTotal: Double,
    val grossProfit: Double
)

@Immutable
data class SalesTrendComparisonPoint(
    val label: String,
    val currentSales: Double,
    val previousSales: Double
)

@Immutable
data class CategoryProfitReport(
    val category: String,
    val revenue: Double,
    val cost: Double,
    val profit: Double,
    val marginPercent: Double
)

@Immutable
data class StockHealthSummary(
    val optimalCount: Int,
    val lowStockCount: Int
) {
    val totalCount: Int
        get() = optimalCount + lowStockCount
}
