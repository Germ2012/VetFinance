package com.example.vetfinance.domain.usecase

import androidx.paging.PagingData
import com.example.vetfinance.data.CLIENT_DEBT_EVENT_ADJUSTMENT
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.DebtCollectionRow
import com.example.vetfinance.data.DebtCollectionSummary
import com.example.vetfinance.data.Product
import com.example.vetfinance.domain.model.CashClosingSummary
import com.example.vetfinance.domain.model.CategoryProfitReport
import com.example.vetfinance.domain.model.ClientPurchaseReport
import com.example.vetfinance.domain.model.FinancialSummary
import com.example.vetfinance.domain.model.GlobalSearchResult
import com.example.vetfinance.domain.model.ProductProfitReport
import com.example.vetfinance.domain.model.SalesTrendComparisonPoint
import com.example.vetfinance.domain.model.StockHealthSummary
import com.example.vetfinance.viewmodel.VetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import javax.inject.Inject

class GetFilteredInventoryUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(filterType: String, searchQuery: String): Flow<PagingData<Product>> {
        return repository.getProductsPaginated(filterType, searchQuery)
    }
}

class GetClientsPageUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(searchQuery: String): Flow<PagingData<Client>> {
        return repository.getClientsPaginated(searchQuery)
    }
}

class GetFrequentSaleProductsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(limit: Int = 6): Flow<List<Product>> {
        return repository.getFrequentSaleProducts(limit)
    }
}

class GetPendingCollectionRowsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(): Flow<List<DebtCollectionRow>> {
        return repository.getPendingCollectionRows()
    }
}

class GetDebtCollectionPageUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(
        searchQuery: String,
        includeZeroDebt: Boolean,
        minimumDebt: Double,
        sortMode: String
    ): Flow<PagingData<DebtCollectionRow>> {
        return repository.getDebtCollectionRowsPaginated(
            searchQuery = searchQuery,
            includeZeroDebt = includeZeroDebt,
            minimumDebt = minimumDebt,
            sortMode = sortMode
        )
    }
}

class GetDebtCollectionSummaryUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(
        searchQuery: String,
        includeZeroDebt: Boolean,
        minimumDebt: Double
    ): Flow<DebtCollectionSummary> {
        return repository.getDebtCollectionSummary(
            searchQuery = searchQuery,
            includeZeroDebt = includeZeroDebt,
            minimumDebt = minimumDebt
        )
    }
}

class GetGlobalSearchResultsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(query: String, limit: Int = 12): Flow<List<GlobalSearchResult>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return flowOf(emptyList())

        return repository.searchGlobal(normalizedQuery, limit)
            .map { rows ->
                rows.map { row ->
                    GlobalSearchResult(
                        id = row.id,
                        type = row.type,
                        title = row.title,
                        subtitle = row.subtitle
                    )
                }
            }
            .flowOn(Dispatchers.Default)
    }
}

class GetCashClosingSummaryUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(): Flow<CashClosingSummary> {
        val zoneId = ZoneId.systemDefault()
        val startOfDay = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = LocalDate.now().atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        return combine(
            repository.getSalesTotalsForRange(startOfDay, endOfDay),
            repository.getPaymentsTotalForRange(startOfDay, endOfDay),
            repository.getDebtTotalsForRange(startOfDay, endOfDay, CLIENT_DEBT_EVENT_ADJUSTMENT)
        ) { sales, paymentsTotal, debtTotals ->
            CashClosingSummary(
                salesCount = sales.salesCount,
                salesTotal = sales.salesTotal,
                paymentsTotal = paymentsTotal,
                debtIncreases = debtTotals.debtIncreases,
                debtAdjustments = debtTotals.debtAdjustments,
                operationalTotal = sales.salesTotal + paymentsTotal
            )
        }.flowOn(Dispatchers.Default)
    }
}

class GetFinancialSummaryUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<FinancialSummary> {
        return repository.getFinancialSummary(startDate, endDate)
            .map { row ->
                FinancialSummary(
                    salesTotal = row.salesTotal,
                    grossProfit = row.grossProfit
                )
            }
            .flowOn(Dispatchers.Default)
    }
}

class GetSalesTrendComparisonUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(): Flow<List<SalesTrendComparisonPoint>> {
        val locale = java.util.Locale("es", "ES")
        val zoneId = ZoneId.systemDefault()
        val weekStart = LocalDate.now().with(WeekFields.of(locale).dayOfWeek(), 1)
        val weekEnd = weekStart.plusDays(6)
        val previousWeekStart = weekStart.minusWeeks(1)
        val previousWeekEnd = previousWeekStart.plusDays(6)
        val currentStartMillis = weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currentEndMillis = weekEnd.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        val previousStartMillis = previousWeekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val previousEndMillis = previousWeekEnd.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)

        return combine(
            repository.getDailySalesTotals(currentStartMillis, currentEndMillis),
            repository.getDailySalesTotals(previousStartMillis, previousEndMillis)
        ) { currentRows, previousRows ->
            val currentByDay = currentRows.associateBy { it.dayIndex }
            val previousByDay = previousRows.associateBy { it.dayIndex }
            (0..6).map { dayIndex ->
                SalesTrendComparisonPoint(
                    label = weekStart.plusDays(dayIndex.toLong()).format(dayFormatter)
                        .replaceFirstChar { it.uppercase(locale) },
                    currentSales = currentByDay[dayIndex]?.totalSales ?: 0.0,
                    previousSales = previousByDay[dayIndex]?.totalSales ?: 0.0
                )
            }
        }.flowOn(Dispatchers.Default)
    }
}

class GetCategoryProfitReportsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(startDate: Long, endDate: Long, limit: Int = 8): Flow<List<CategoryProfitReport>> {
        return repository.getCategoryProfitRows(startDate, endDate, limit)
            .map { rows ->
                rows.map { row ->
                    CategoryProfitReport(
                        category = row.category,
                        revenue = row.revenue,
                        cost = row.cost,
                        profit = row.profit,
                        marginPercent = if (row.revenue > 0.0) (row.profit / row.revenue) * 100.0 else 0.0
                    )
                }
            }
            .flowOn(Dispatchers.Default)
    }
}

class GetStockHealthSummaryUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(): Flow<StockHealthSummary> {
        return repository.getStockHealthSummary()
            .map { row ->
                StockHealthSummary(
                    optimalCount = row.optimalCount,
                    lowStockCount = row.lowStockCount
                )
            }
            .flowOn(Dispatchers.Default)
    }
}

class GetProductProfitReportsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<ProductProfitReport>> {
        return repository.getProductProfitReports(startDate, endDate).map { rows ->
            rows.map { row ->
                ProductProfitReport(
                    productId = row.productId,
                    name = row.name,
                    isService = row.isService,
                    quantitySold = row.totalSold,
                    revenue = row.revenue,
                    cost = row.cost,
                    profit = row.profit,
                    marginPercent = if (row.revenue > 0.0) (row.profit / row.revenue) * 100.0 else 0.0
                )
            }
        }.flowOn(Dispatchers.Default)
    }
}

class GetClientPurchaseReportsUseCase @Inject constructor(
    private val repository: VetRepository
) {
    operator fun invoke(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<ClientPurchaseReport>> {
        return repository.getClientPurchaseReports(startDate, endDate, limit).map { rows ->
            rows.map { row ->
                ClientPurchaseReport(
                    clientId = row.clientId,
                    clientName = row.clientName,
                    totalPurchased = row.totalPurchased,
                    saleCount = row.saleCount
                )
            }
        }.flowOn(Dispatchers.Default)
    }
}
