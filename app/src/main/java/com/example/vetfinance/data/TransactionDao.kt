package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow


@Immutable
data class AppointmentWithDetails(
    @Embedded val appointment: Appointment,
    @Relation(
        parentColumn = "petIdFk",
        entityColumn = "petId"
    )
    val pet: Pet,
    @Relation(
        parentColumn = "clientIdFk",
        entityColumn = "clientId"
    )
    val client: Client
)

@Immutable
data class TopSellingProduct(
    val name: String,
    val totalSold: Double,
    val totalRevenue: Double
)

@Immutable
data class DailySalesTotal(
    val dayIndex: Int,
    val totalSales: Double
)

@Immutable
data class CategoryProfitRow(
    val category: String,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@Immutable
data class FinancialSummaryRow(
    val salesTotal: Double,
    val grossProfit: Double
)

@Immutable
data class StockHealthRow(
    val optimalCount: Int,
    val lowStockCount: Int
)

@Immutable
data class InventoryCountsRow(
    val totalCount: Int = 0,
    val productCount: Int = 0,
    val serviceCount: Int = 0,
    val lowStockCount: Int = 0
)

@Immutable
data class InventoryReportSummaryRow(
    val productCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalUnits: Double = 0.0
)

@Immutable
data class CashClosingSalesRow(
    val salesCount: Int,
    val salesTotal: Double
)

@Immutable
data class CashClosingDebtRow(
    val debtIncreases: Double,
    val debtAdjustments: Double
)

@Immutable
data class ClientPaymentSummaryRow(
    val paymentCount: Int = 0,
    val totalPaid: Double = 0.0,
    val lastPaymentDate: Long? = null
)

@Immutable
data class ClientDebtHistorySummaryRow(
    val eventCount: Int = 0,
    val debtIncreases: Double = 0.0
)

@Immutable
data class GlobalSearchRow(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String
)

@Immutable
data class DebtCollectionRow(
    @Embedded val client: Client,
    val totalSold: Double,
    val totalPaid: Double,
    val balance: Double
)

@Immutable
data class DebtCollectionSummary(
    val clientCount: Int,
    val totalPending: Double,
    val totalPaid: Double
)

@Immutable
data class ProductProfitReportRow(
    val productId: String,
    val name: String,
    val isService: Boolean,
    val totalSold: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@Immutable
data class ClientPurchaseReportRow(
    val clientId: String,
    val clientName: String,
    val totalPurchased: Double,
    val saleCount: Int
)

@Immutable
data class SaleListItem(
    val saleId: String,
    val date: Long,
    val totalAmount: Double,
    val clientIdFk: String?,
    val clientName: String?,
    val itemCount: Int
)

@Immutable
data class SalePeriodRow(
    val periodKey: String,
    val sampleDate: Long,
    val lastDate: Long
)

@Immutable
data class SaleDetailLine(
    val crossRefId: String,
    val productId: String,
    val productName: String?,
    val quantitySold: Double,
    val priceAtTimeOfSale: Double,
    val notes: String?,
    val overridePrice: Double?
)


@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Upsert
    suspend fun upsertAll(transactions: List<Transaction>)
}
