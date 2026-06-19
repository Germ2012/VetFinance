package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.vetfinance.data.DebtCollectionRow
import com.example.vetfinance.data.DebtCollectionSummary
import com.example.vetfinance.data.InventoryReportSummaryRow
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SalePeriodRow
import com.example.vetfinance.data.TopSellingProduct
import com.example.vetfinance.domain.model.CashClosingSummary
import com.example.vetfinance.domain.model.CategoryProfitReport
import com.example.vetfinance.domain.model.ClientPurchaseReport
import com.example.vetfinance.domain.model.FinancialSummary
import com.example.vetfinance.domain.model.ProductProfitReport
import com.example.vetfinance.domain.model.SalesTrendComparisonPoint
import com.example.vetfinance.domain.model.StockHealthSummary
import com.example.vetfinance.domain.usecase.GetCashClosingSummaryUseCase
import com.example.vetfinance.domain.usecase.GetCategoryProfitReportsUseCase
import com.example.vetfinance.domain.usecase.GetClientPurchaseReportsUseCase
import com.example.vetfinance.domain.usecase.GetDebtCollectionPageUseCase
import com.example.vetfinance.domain.usecase.GetDebtCollectionSummaryUseCase
import com.example.vetfinance.domain.usecase.GetFinancialSummaryUseCase
import com.example.vetfinance.domain.usecase.GetProductProfitReportsUseCase
import com.example.vetfinance.domain.usecase.GetSalesTrendComparisonUseCase
import com.example.vetfinance.domain.usecase.GetStockHealthSummaryUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class BackupImportUiState(
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val message: String? = null,
    val finishedToken: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: VetRepository,
    private val getDebtCollectionPageUseCase: GetDebtCollectionPageUseCase,
    private val getDebtCollectionSummaryUseCase: GetDebtCollectionSummaryUseCase,
    private val getCashClosingSummaryUseCase: GetCashClosingSummaryUseCase,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val getSalesTrendComparisonUseCase: GetSalesTrendComparisonUseCase,
    private val getCategoryProfitReportsUseCase: GetCategoryProfitReportsUseCase,
    private val getStockHealthSummaryUseCase: GetStockHealthSummaryUseCase,
    private val getProductProfitReportsUseCase: GetProductProfitReportsUseCase,
    private val getClientPurchaseReportsUseCase: GetClientPurchaseReportsUseCase,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val workManager = WorkManager.getInstance(appContext)
    private val _importWorkId = MutableStateFlow<UUID?>(null)

    val backupImportUiState: StateFlow<BackupImportUiState> = _importWorkId
        .flatMapLatest { workId ->
            if (workId == null) {
                flowOf(BackupImportUiState())
            } else {
                workManager.workInfoFlow(workId).map { workInfo ->
                    workInfo.toBackupImportUiState(workId)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupImportUiState())

    val pagingRefreshEvents: SharedFlow<Unit> = appEventBus.pagingRefreshEvents

    val cashClosingSummary: StateFlow<CashClosingSummary> = getCashClosingSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashClosingSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0))

    val salesTrendComparison: StateFlow<List<SalesTrendComparisonPoint>> = getSalesTrendComparisonUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDebt: StateFlow<Double?> = repository.getTotalDebt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalInventoryValue: StateFlow<Double?> = repository.getTotalInventoryValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stockHealthSummary: StateFlow<StockHealthSummary> = getStockHealthSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockHealthSummary(0, 0))

    val inventoryReportSummary: StateFlow<InventoryReportSummaryRow> = repository.getInventoryReportSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryReportSummaryRow())

    val inventoryReportProductsPaginated: Flow<PagingData<Product>> = repository.getInventoryReportProductsPaginated()
        .cachedIn(viewModelScope)

    val debtCollectionSummary: StateFlow<DebtCollectionSummary> = getDebtCollectionSummaryUseCase(
        searchQuery = "",
        includeZeroDebt = false,
        minimumDebt = 0.0
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtCollectionSummary(0, 0.0, 0.0))

    val debtRowsPaginated: Flow<PagingData<DebtCollectionRow>> = getDebtCollectionPageUseCase(
        searchQuery = "",
        includeZeroDebt = false,
        minimumDebt = 0.0,
        sortMode = "Mayor deuda"
    ).cachedIn(viewModelScope)

    private val _topProductsPeriod = MutableStateFlow(TopProductsPeriod.MONTH)
    val topProductsPeriod: StateFlow<TopProductsPeriod> = _topProductsPeriod.asStateFlow()
    private val _topProductsMetric = MutableStateFlow(TopProductsMetric.QUANTITY)
    val topProductsMetric: StateFlow<TopProductsMetric> = _topProductsMetric.asStateFlow()
    private val _topProductsDate = MutableStateFlow(LocalDate.now())
    val topProductsDate: StateFlow<LocalDate> = _topProductsDate.asStateFlow()
    private val _selectedTopProduct = MutableStateFlow<TopSellingProduct?>(null)
    val selectedTopProduct: StateFlow<TopSellingProduct?> = _selectedTopProduct.asStateFlow()

    private val topProductsDateRange: Flow<Pair<Long, Long>> = combine(
        _topProductsPeriod,
        _topProductsDate
    ) { period, date ->
        val zoneId = ZoneId.systemDefault()
        val start: LocalDate
        val end: LocalDate
        when (period) {
            TopProductsPeriod.WEEK -> {
                val weekFields = WeekFields.of(Locale.getDefault())
                start = date.with(weekFields.dayOfWeek(), 1)
                end = start.plusDays(6)
            }
            TopProductsPeriod.MONTH -> {
                start = date.withDayOfMonth(1)
                end = date.withDayOfMonth(date.lengthOfMonth())
            }
            TopProductsPeriod.YEAR -> {
                start = date.withDayOfYear(1)
                end = date.withDayOfYear(date.lengthOfYear())
            }
        }
        start.atStartOfDay(zoneId).toInstant().toEpochMilli() to
            end.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
    }.distinctUntilChanged()

    val topSellingProducts: StateFlow<List<TopSellingProduct>> = combine(
        topProductsDateRange,
        _topProductsMetric
    ) { range, metric ->
        range to metric
    }.flatMapLatest { (range, metric) ->
        val (start, end) = range
        when (metric) {
            TopProductsMetric.QUANTITY -> repository.getTopSellingProductsByQuantity(startDate = start, endDate = end, limit = 10)
            TopProductsMetric.REVENUE -> repository.getTopSellingProductsByRevenue(startDate = start, endDate = end, limit = 10)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportPeriodType = MutableStateFlow(ReportPeriodType.DAY)
    val reportPeriodType: StateFlow<ReportPeriodType> = _reportPeriodType.asStateFlow()

    private val _selectedHistoricalPeriod = MutableStateFlow<HistoricalPeriod?>(null)
    val selectedHistoricalPeriod: StateFlow<HistoricalPeriod?> = _selectedHistoricalPeriod.asStateFlow()

    val availableHistoricalPeriods: StateFlow<List<HistoricalPeriod>> = reportPeriodType
        .flatMapLatest { type ->
            repository.getSalePeriods(type.name).map { rows ->
                generateHistoricalPeriods(rows, type)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<FinancialSummary> = selectedHistoricalPeriod.flatMapLatest { period ->
        if (period == null) {
            flowOf(FinancialSummary(0.0, 0.0))
        } else {
            getFinancialSummaryUseCase(period.startDate, period.endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary(0.0, 0.0))

    val salesSummary: StateFlow<Double> = financialSummary.map { it.salesTotal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grossProfitSummary: StateFlow<Double> = financialSummary.map { it.grossProfit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryProfitReports: StateFlow<List<CategoryProfitReport>> = selectedHistoricalPeriod.flatMapLatest { period ->
        if (period == null) {
            flowOf(emptyList())
        } else {
            getCategoryProfitReportsUseCase(period.startDate, period.endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productProfitReports: StateFlow<List<ProductProfitReport>> = selectedHistoricalPeriod.flatMapLatest { period ->
        if (period == null) {
            flowOf(emptyList())
        } else {
            getProductProfitReportsUseCase(period.startDate, period.endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientPurchaseReports: StateFlow<List<ClientPurchaseReport>> = selectedHistoricalPeriod.flatMapLatest { period ->
        if (period == null) {
            flowOf(emptyList())
        } else {
            getClientPurchaseReportsUseCase(period.startDate, period.endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onReportPeriodTypeChanged(newType: ReportPeriodType) {
        _reportPeriodType.value = newType
        _selectedHistoricalPeriod.value = null
    }

    fun onHistoricalPeriodSelected(period: HistoricalPeriod) {
        _selectedHistoricalPeriod.value = period
    }

    fun onTopProductsPeriodSelected(period: TopProductsPeriod) {
        _topProductsPeriod.value = period
        _topProductsDate.value = LocalDate.now()
    }

    fun onTopProductsMetricSelected(metric: TopProductsMetric) {
        _topProductsMetric.value = metric
        _selectedTopProduct.value = null
    }

    fun onTopProductsDateSelected(dateMillis: Long) {
        _topProductsDate.value = Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun onTopProductSelected(product: TopSellingProduct?) {
        _selectedTopProduct.value = if (_selectedTopProduct.value == product) null else product
    }

    fun markBackupCreated() {
        repository.markBackupCreated()
        appEventBus.reportOperationSuccess("Fecha de respaldo actualizada.")
    }

    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()

    fun importarDatosDesdeZIP(uri: Uri) {
        val request = OneTimeWorkRequestBuilder<BackupImportWorker>()
            .setInputData(workDataOf(BackupImportWorker.KEY_URI to uri.toString()))
            .build()

        _importWorkId.value = request.id
        workManager.enqueueUniqueWork(
            BackupImportWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun WorkInfo?.toBackupImportUiState(workId: UUID): BackupImportUiState {
        if (this == null) {
            return BackupImportUiState(isRunning = true, progress = 0, message = "Preparando importacion...")
        }

        val progressValue = progress.getInt(BackupImportWorker.KEY_PROGRESS, 0)
        val progressMessage = progress.getString(BackupImportWorker.KEY_MESSAGE)
        val outputMessage = outputData.getString(BackupImportWorker.KEY_MESSAGE)
        return when (state) {
            WorkInfo.State.ENQUEUED -> BackupImportUiState(
                isRunning = true,
                progress = progressValue,
                message = "Importacion en cola..."
            )
            WorkInfo.State.RUNNING -> BackupImportUiState(
                isRunning = true,
                progress = progressValue.coerceIn(0, 99),
                message = progressMessage ?: "Importando respaldo..."
            )
            WorkInfo.State.SUCCEEDED -> BackupImportUiState(
                progress = 100,
                message = outputMessage ?: "Importacion completada.",
                finishedToken = "${workId}:success"
            )
            WorkInfo.State.FAILED -> BackupImportUiState(
                message = outputMessage ?: "La importacion fallo.",
                finishedToken = "${workId}:failed"
            )
            WorkInfo.State.BLOCKED -> BackupImportUiState(
                isRunning = true,
                progress = progressValue,
                message = "Esperando tareas previas..."
            )
            WorkInfo.State.CANCELLED -> BackupImportUiState(
                message = "Importacion cancelada.",
                finishedToken = "${workId}:cancelled"
            )
        }
    }

    private fun WorkManager.workInfoFlow(workId: UUID): Flow<WorkInfo?> = callbackFlow {
        val liveData = getWorkInfoByIdLiveData(workId)
        val observer = Observer<WorkInfo> { workInfo ->
            trySend(workInfo)
        }
        liveData.observeForever(observer)
        awaitClose { liveData.removeObserver(observer) }
    }

    private fun generateHistoricalPeriods(periodRows: List<SalePeriodRow>, type: ReportPeriodType): List<HistoricalPeriod> {
        if (periodRows.isEmpty()) return emptyList()
        val zoneId = ZoneId.systemDefault()
        val locale = Locale("es", "ES")
        val weekFields = WeekFields.of(locale)

        return periodRows.map { row ->
            val saleDate = Instant.ofEpochMilli(row.sampleDate).atZone(zoneId).toLocalDate()
            val periodStart = when (type) {
                ReportPeriodType.DAY -> saleDate
                ReportPeriodType.WEEK -> saleDate.with(weekFields.dayOfWeek(), 1)
                ReportPeriodType.MONTH -> saleDate.withDayOfMonth(1)
            }
            val (startDate, endDate, displayName) = when (type) {
                ReportPeriodType.DAY -> {
                    val date = periodStart
                    Triple(
                        date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        date.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli(),
                        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
                    )
                }
                ReportPeriodType.WEEK -> {
                    val weekStart = periodStart
                    val weekEnd = weekStart.plusDays(6)
                    val weekOfYear = weekStart.get(weekFields.weekOfWeekBasedYear())
                    val year = weekStart.year
                    val formatter = DateTimeFormatter.ofPattern("dd/MM", locale)
                    Triple(
                        weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        weekEnd.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli(),
                        "Semana $weekOfYear ($year, ${weekStart.format(formatter)} - ${weekEnd.format(formatter)})"
                    )
                }
                ReportPeriodType.MONTH -> {
                    val monthStart = periodStart
                    val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
                    Triple(
                        monthStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        monthEnd.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli(),
                        monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.uppercase() }
                    )
                }
            }
            HistoricalPeriod(
                id = periodStart.toString(),
                displayName = displayName,
                startDate = startDate,
                endDate = endDate
            )
        }.distinctBy { it.id }
    }
}
