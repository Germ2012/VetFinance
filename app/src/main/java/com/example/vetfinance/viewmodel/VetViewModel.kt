package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.R
import com.example.vetfinance.data.*
import com.example.vetfinance.domain.model.CashClosingSummary
import com.example.vetfinance.domain.model.CategoryProfitReport
import com.example.vetfinance.domain.model.ClientPurchaseReport
import com.example.vetfinance.domain.model.FinancialSummary
import com.example.vetfinance.domain.model.GlobalSearchResult
import com.example.vetfinance.domain.model.ProductProfitReport
import com.example.vetfinance.domain.model.SalesTrendComparisonPoint
import com.example.vetfinance.domain.model.StockHealthSummary
import com.example.vetfinance.domain.usecase.GetCashClosingSummaryUseCase
import com.example.vetfinance.domain.usecase.GetCategoryProfitReportsUseCase
import com.example.vetfinance.domain.usecase.GetClientPurchaseReportsUseCase
import com.example.vetfinance.domain.usecase.GetClientsPageUseCase
import com.example.vetfinance.domain.usecase.GetDebtCollectionPageUseCase
import com.example.vetfinance.domain.usecase.GetDebtCollectionSummaryUseCase
import com.example.vetfinance.domain.usecase.GetFinancialSummaryUseCase
import com.example.vetfinance.domain.usecase.GetFilteredInventoryUseCase
import com.example.vetfinance.domain.usecase.GetFrequentSaleProductsUseCase
import com.example.vetfinance.domain.usecase.GetGlobalSearchResultsUseCase
import com.example.vetfinance.domain.usecase.GetPendingCollectionRowsUseCase
import com.example.vetfinance.domain.usecase.GetProductProfitReportsUseCase
import com.example.vetfinance.domain.usecase.GetSalesTrendComparisonUseCase
import com.example.vetfinance.domain.usecase.GetStockHealthSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"
private const val SEARCH_DEBOUNCE_MS = 300L


enum class ReportPeriodType(@StringRes val displayResId: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

@Immutable
data class HistoricalPeriod(
    val id: String, // e.g., "2025-10-06", "2025-W41", "2025-10"
    val displayName: String, // e.g., "06/10/2025", "Semana 41 (06/10 - 12/10)", "Octubre 2025"
    val startDate: Long,
    val endDate: Long
)

@Immutable
data class DebtCollectionFilters(
    val showOnlyWithDebt: Boolean = true,
    val minimumDebt: Double = 0.0,
    val sortMode: String = "Mayor deuda"
)

@Immutable
data class InventoryScreenUiState(
    val showAddProductDialog: Boolean = false,
    val filter: String = "Todos",
    val inventory: List<Product> = emptyList(),
    val searchQuery: String = "",
    val lowStockProducts: List<Product> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val productNameSuggestions: List<Product> = emptyList(),
    val isLoading: Boolean = true
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

enum class TopProductsPeriod(@StringRes val displayResId: Int) {
    WEEK(R.string.topproducts_period_week),
    MONTH(R.string.topproducts_period_month),
    YEAR(R.string.topproducts_period_year)
}

enum class TopProductsMetric {
    QUANTITY,
    REVENUE
}

@Immutable
data class SaleInventoryStats(
    val productCount: Int = 0,
    val serviceCount: Int = 0,
    val doseCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository,
    private val getFilteredInventoryUseCase: GetFilteredInventoryUseCase,
    private val getClientsPageUseCase: GetClientsPageUseCase,
    private val getFrequentSaleProductsUseCase: GetFrequentSaleProductsUseCase,
    private val getPendingCollectionRowsUseCase: GetPendingCollectionRowsUseCase,
    private val getDebtCollectionPageUseCase: GetDebtCollectionPageUseCase,
    private val getDebtCollectionSummaryUseCase: GetDebtCollectionSummaryUseCase,
    private val getCashClosingSummaryUseCase: GetCashClosingSummaryUseCase,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val getSalesTrendComparisonUseCase: GetSalesTrendComparisonUseCase,
    private val getCategoryProfitReportsUseCase: GetCategoryProfitReportsUseCase,
    private val getStockHealthSummaryUseCase: GetStockHealthSummaryUseCase,
    private val getProductProfitReportsUseCase: GetProductProfitReportsUseCase,
    private val getClientPurchaseReportsUseCase: GetClientPurchaseReportsUseCase,
    private val getGlobalSearchResultsUseCase: GetGlobalSearchResultsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationErrorMessage = MutableStateFlow<String?>(null)
    val operationErrorMessage: StateFlow<String?> = _operationErrorMessage.asStateFlow()
    private val _operationSuccessMessage = MutableStateFlow<String?>(null)
    val operationSuccessMessage: StateFlow<String?> = _operationSuccessMessage.asStateFlow()

    fun clearOperationErrorMessage() {
        _operationErrorMessage.value = null
    }

    fun clearOperationSuccessMessage() {
        _operationSuccessMessage.value = null
    }

    private fun reportOperationError(error: Throwable) {
        _operationErrorMessage.value = error.message ?: "Ocurrió un error inesperado."
    }

    private fun reportOperationError(message: String) {
        _operationErrorMessage.value = message
    }

    private fun reportOperationSuccess(message: String) {
        _operationSuccessMessage.value = null
        _operationSuccessMessage.value = message
    }

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    private val _showSupplierDialog = MutableStateFlow(false)
    val showSupplierDialog: StateFlow<Boolean> = _showSupplierDialog.asStateFlow()

    private val _editingSupplier = MutableStateFlow<Supplier?>(null)
    val editingSupplier: StateFlow<Supplier?> = _editingSupplier.asStateFlow()

    fun onShowSupplierDialog(supplier: Supplier? = null) {
        _editingSupplier.value = supplier
        _showSupplierDialog.value = true
    }

    fun onDismissSupplierDialog() {
        _editingSupplier.value = null
        _showSupplierDialog.value = false
    }

    fun addOrUpdateSupplier(supplier: Supplier) = viewModelScope.launch {
        if (_editingSupplier.value == null) {
            repository.insertSupplier(supplier)
            reportOperationSuccess("Proveedor guardado.")
        } else {
            repository.updateSupplier(supplier)
            reportOperationSuccess("Proveedor actualizado.")
        }
        onDismissSupplierDialog()
    }

    private val _restockSearchQuery = MutableStateFlow("")
    val restockSearchQuery: StateFlow<String> = _restockSearchQuery.asStateFlow()

    fun onRestockSearchQueryChange(query: String) {
        _restockSearchQuery.value = query
    }

    private val _restockHistory = MutableStateFlow<List<RestockHistoryItem>>(emptyList())
    val restockHistory: StateFlow<List<RestockHistoryItem>> = _restockHistory.asStateFlow()

    fun fetchRestockHistory(date: LocalDate) {
        viewModelScope.launch {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            _restockHistory.value = repository.getRestockHistoryForDateRange(startOfDay, endOfDay)
        }
    }

    fun executeRestock(supplierId: String, totalCost: Double, itemsToRestock: List<RestockOrderItem>, orderDate: Long, supplierDebtDueDate: Long? = null) = viewModelScope.launch {
        try {
            val orderId = UUID.randomUUID().toString()
            val order = RestockOrder(orderId = orderId, supplierIdFk = supplierId, orderDate = orderDate, totalAmount = totalCost)
            val updatedItems = itemsToRestock.map { it.copy(orderIdFk = orderId) }
            repository.performRestock(order, updatedItems, supplierDebtDueDate)
            reportOperationSuccess("Reabastecimiento registrado.")
        } catch (e: Exception) {
            reportOperationError(e)
        }
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        try { repository.deleteProduct(product); reportOperationSuccess("Producto eliminado.") } catch (e: Exception) { reportOperationError(e) }
    }
    fun deleteSale(sale: SaleWithProducts) = viewModelScope.launch {
        try { repository.deleteSale(sale); reportOperationSuccess("Venta eliminada y stock restaurado.") } catch (e: Exception) { reportOperationError(e) }
    }
    fun deleteClient(client: Client) = viewModelScope.launch {
        try { repository.deleteClient(client); reportOperationSuccess("Cliente eliminado.") } catch (e: Exception) { reportOperationError(e) }
    }
    fun openContainerForBulkSale(containerProduct: Product) = viewModelScope.launch {
        try {
            if (containerProduct.containedProductId != null && containerProduct.containerSize != null) {
                repository.performInventoryTransfer(
                    containerId = containerProduct.productId,
                    containedId = containerProduct.containedProductId,
                    amountToTransfer = containerProduct.containerSize
                )
                reportOperationSuccess("Contenedor abierto y stock actualizado.")
            }
        } catch (e: Exception) {
            reportOperationError(e)
        }
    }
    private val _inventoryFilter = MutableStateFlow("Todos")
    val inventoryFilter: StateFlow<String> = _inventoryFilter.asStateFlow()
    private val _petSearchQuery = MutableStateFlow("")
    val petSearchQuery: StateFlow<String> = _petSearchQuery.asStateFlow()
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()
    private val _selectedSaleDateFilter = MutableStateFlow<Long?>(null)
    val selectedSaleDateFilter: StateFlow<Long?> = _selectedSaleDateFilter.asStateFlow()
    private val _saleInventoryFilter = MutableStateFlow("Todos")
    val saleInventoryFilter: StateFlow<String> = _saleInventoryFilter.asStateFlow()

    private val _productNameSuggestionQuery = MutableStateFlow("")
    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestionQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchProductSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _clientNameSuggestionQuery = MutableStateFlow("")
    val clientNameSuggestions: StateFlow<List<Client>> = _clientNameSuggestionQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchClientSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = repository.getAllClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val petIdToNameMap: StateFlow<Map<String, String>> = petsWithOwners
        .map { pets -> pets.associate { it.pet.petId to it.pet.name } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val inventory: StateFlow<List<Product>> = repository.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()
    private val _debtHistory = MutableStateFlow<List<ClientDebtHistory>>(emptyList())
    val debtHistory: StateFlow<List<ClientDebtHistory>> = _debtHistory.asStateFlow()
    private val _productCostHistory = MutableStateFlow<List<ProductCostHistoryItem>>(emptyList())
    val productCostHistory: StateFlow<List<ProductCostHistoryItem>> = _productCostHistory.asStateFlow()
    private val _productStockMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    val productStockMovements: StateFlow<List<StockMovement>> = _productStockMovements.asStateFlow()
    private val _sales = repository.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _payments = repository.getAllPayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _allDebtHistory = repository.getAllDebtHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    val frequentSaleProducts: StateFlow<List<Product>> = getFrequentSaleProductsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCollectionRows: StateFlow<List<DebtCollectionRow>> = getPendingCollectionRowsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashClosingSummary: StateFlow<CashClosingSummary> = getCashClosingSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashClosingSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0))

    val salesTrendComparison: StateFlow<List<SalesTrendComparisonPoint>> = getSalesTrendComparisonUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val debouncedProductSearchQuery = _productSearchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    private val debouncedClientSearchQuery = _clientSearchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    private val debouncedPetSearchQuery = _petSearchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    private val debouncedGlobalSearchQuery = _globalSearchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = debouncedGlobalSearchQuery
        .flatMapLatest { query -> getGlobalSearchResultsUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryPaginated: Flow<PagingData<Product>> = combine(_inventoryFilter, debouncedProductSearchQuery) { filter, query ->
        filter to query
    }.distinctUntilChanged().flatMapLatest { (filter, query) ->
        getFilteredInventoryUseCase(filter, query)
    }.cachedIn(viewModelScope)

    val clientsPaginated: Flow<PagingData<Client>> = debouncedClientSearchQuery.flatMapLatest { query ->
        getClientsPageUseCase(query)
    }.cachedIn(viewModelScope)

    val debtClientsPaginated: Flow<PagingData<Client>> = debouncedClientSearchQuery.flatMapLatest { repository.getDebtClientsPaginated(it) }.cachedIn(viewModelScope)

    private val _debtCollectionFilters = MutableStateFlow(DebtCollectionFilters())
    val debtCollectionFilters: StateFlow<DebtCollectionFilters> = _debtCollectionFilters.asStateFlow()

    fun onDebtCollectionFiltersChanged(
        showOnlyWithDebt: Boolean,
        minimumDebt: Double,
        sortMode: String
    ) {
        _debtCollectionFilters.value = DebtCollectionFilters(
            showOnlyWithDebt = showOnlyWithDebt,
            minimumDebt = minimumDebt.coerceAtLeast(0.0),
            sortMode = sortMode
        )
    }

    val debtCollectionRowsPaginated: Flow<PagingData<DebtCollectionRow>> = combine(
        debouncedClientSearchQuery,
        _debtCollectionFilters
    ) { query, filters ->
        query to filters
    }.flatMapLatest { (query, filters) ->
        getDebtCollectionPageUseCase(
            searchQuery = query,
            includeZeroDebt = !filters.showOnlyWithDebt,
            minimumDebt = filters.minimumDebt,
            sortMode = filters.sortMode
        )
    }.cachedIn(viewModelScope)

    val debtCollectionSummary: StateFlow<DebtCollectionSummary> = combine(
        debouncedClientSearchQuery,
        _debtCollectionFilters
    ) { query, filters ->
        query to filters
    }.flatMapLatest { (query, filters) ->
        getDebtCollectionSummaryUseCase(
            searchQuery = query,
            includeZeroDebt = !filters.showOnlyWithDebt,
            minimumDebt = filters.minimumDebt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtCollectionSummary(0, 0.0, 0.0))

    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> = combine(petsWithOwners, debouncedPetSearchQuery) { pets, query ->
        if (query.isBlank()) pets else pets.filter { it.pet.name.contains(query, true) || it.owner.name.contains(query, true) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInventory: StateFlow<List<Product>> = combine(inventory, debouncedProductSearchQuery) { products, query ->
        if (query.isBlank()) products else products.filter { it.name.contains(query, true) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSales: StateFlow<List<SaleWithProducts>> = combine(_sales, _selectedSaleDateFilter) { sales, date ->
        if (date == null) sales else {
            val startOfDay = LocalDate.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            sales.filter { it.sale.date in startOfDay..endOfDay }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = inventory.map { products ->
        products.filter { product ->
            if (product.isService || product.isContainer || product.sellingMethod == SELLING_METHOD_DOSE_ONLY) {
                false
            } else {
                val threshold = product.lowStockThreshold ?: if (product.sellingMethod == SELLING_METHOD_BY_UNIT) 4.0 else 0.0
                threshold > 0.0 && product.stock < threshold
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProductsByName: StateFlow<List<Product>> = lowStockProducts
        .map { products ->
            products.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val lowStockProductsByUrgency: StateFlow<List<Product>> = lowStockProducts
        .map { products ->
            products.sortedWith(
                compareBy<Product> { product ->
                    val threshold = product.lowStockThreshold ?: 1.0
                    if (threshold > 0) product.stock / threshold else product.stock
                }.thenBy { it.name.lowercase(Locale.getDefault()) }
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleVisibleInventory: StateFlow<List<Product>> = combine(
        filteredInventory,
        _saleInventoryFilter,
        lowStockProducts,
        frequentSaleProducts
    ) { products, filter, lowStockList, frequentList ->
        val lowStockIds = lowStockList.map { it.productId }.toSet()
        val frequentRankById = frequentList.mapIndexed { index, product -> product.productId to index }.toMap()
        val filtered = products.filter { product ->
            when (filter) {
                "Productos" -> !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY
                "Servicios" -> product.isService
                "Dosis" -> !product.isService && product.sellingMethod == SELLING_METHOD_DOSE_ONLY
                "Bajo stock" -> product.productId in lowStockIds
                else -> true
            }
        }
        if (frequentRankById.isEmpty()) {
            filtered
        } else {
            filtered.sortedWith(
                compareBy<Product> { frequentRankById[it.productId] ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleInventoryStats: StateFlow<SaleInventoryStats> = saleVisibleInventory
        .map { products ->
            SaleInventoryStats(
                productCount = products.count { !it.isService && it.sellingMethod != SELLING_METHOD_DOSE_ONLY },
                serviceCount = products.count { it.isService },
                doseCount = products.count { !it.isService && it.sellingMethod == SELLING_METHOD_DOSE_ONLY }
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SaleInventoryStats())

    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> = _selectedCalendarDate.flatMapLatest { date ->
        repository.getAppointmentsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierDebtsOnSelectedDate: StateFlow<List<SupplierDebtWithSupplier>> = _selectedCalendarDate.flatMapLatest { date ->
        repository.getSupplierDebtsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingAppointments: StateFlow<List<AppointmentWithDetails>> = _appSettings.flatMapLatest { settings ->
        flow {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
            val alertLimit = LocalDate.now().plusDays(settings.treatmentAlertDays.toLong()).atStartOfDay(zoneId).toInstant().toEpochMilli()
            emitAll(repository.getAppointmentsForDate(now, alertLimit).map { appointments ->
                appointments.filter { it.appointment.status == APPOINTMENT_STATUS_PENDING }
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingSupplierDebts: StateFlow<List<SupplierDebtWithSupplier>> = _appSettings.flatMapLatest { settings ->
        flow {
            val dateLimit = LocalDate.now().plusDays(settings.supplierDebtAlertDays.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            emitAll(repository.getUpcomingSupplierDebts(dateLimit))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDebt: StateFlow<Double?> = repository.getTotalDebt().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val totalInventoryValue: StateFlow<Double?> = repository.getTotalInventoryValue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val stockHealthSummary: StateFlow<StockHealthSummary> = getStockHealthSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockHealthSummary(0, 0))

    private val todayRangeMillis = currentDayRangeMillis()
    val salesSummaryToday: StateFlow<Double> = repository
        .getSalesTotalsForRange(todayRangeMillis.first, todayRangeMillis.second)
        .map { it.salesTotal }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _topProductsPeriod = MutableStateFlow(TopProductsPeriod.MONTH)
    val topProductsPeriod: StateFlow<TopProductsPeriod> = _topProductsPeriod.asStateFlow()
    private val _topProductsMetric = MutableStateFlow(TopProductsMetric.QUANTITY)
    val topProductsMetric: StateFlow<TopProductsMetric> = _topProductsMetric.asStateFlow()
    private val _topProductsDate = MutableStateFlow(LocalDate.now())
    val topProductsDate: StateFlow<LocalDate> = _topProductsDate.asStateFlow()
    private val _selectedTopProduct = MutableStateFlow<TopSellingProduct?>(null)
    val selectedTopProduct: StateFlow<TopSellingProduct?> = _selectedTopProduct.asStateFlow()

    private val topProductsDateRange: Flow<Pair<Long, Long>> = combine(_topProductsPeriod, _topProductsDate) { period, date ->
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
        Pair(start.atStartOfDay(zoneId).toInstant().toEpochMilli(), end.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli())
    }.distinctUntilChanged()

    val topSellingProducts: StateFlow<List<TopSellingProduct>> = combine(topProductsDateRange, _topProductsMetric) { range, metric ->
        range to metric
    }.flatMapLatest { (range, metric) ->
        val (start, end) = range
        when (metric) {
            TopProductsMetric.QUANTITY -> repository.getTopSellingProductsByQuantity(startDate = start, endDate = end, limit = 10)
            TopProductsMetric.REVENUE -> repository.getTopSellingProductsByRevenue(startDate = start, endDate = end, limit = 10)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Estados para la funcionalidad de Reportes ---
    private val _reportPeriodType = MutableStateFlow(ReportPeriodType.DAY)
    val reportPeriodType: StateFlow<ReportPeriodType> = _reportPeriodType.asStateFlow()

    private val _selectedHistoricalPeriod = MutableStateFlow<HistoricalPeriod?>(null)
    val selectedHistoricalPeriod: StateFlow<HistoricalPeriod?> = _selectedHistoricalPeriod.asStateFlow()

    val availableHistoricalPeriods: StateFlow<List<HistoricalPeriod>> = combine(_sales, reportPeriodType) { sales, type ->
        generateHistoricalPeriods(sales, type)
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


    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()
    private val _showAddClientDialog = MutableStateFlow(false)
    val showAddClientDialog: StateFlow<Boolean> = _showAddClientDialog.asStateFlow()
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()
    private val _showAddAppointmentDialog = MutableStateFlow(false)
    val showAddAppointmentDialog: StateFlow<Boolean> = _showAddAppointmentDialog.asStateFlow()
    private val _clientForPayment = MutableStateFlow<Client?>(null)
    val clientForPayment: StateFlow<Client?> = _clientForPayment.asStateFlow()

    private val inventoryBaseUiState: Flow<InventoryScreenUiState> = combine(
        _showAddProductDialog,
        _inventoryFilter,
        inventory,
        _productSearchQuery,
        lowStockProductsByUrgency
    ) { showDialog, filter, productList, query, lowStockList ->
        InventoryScreenUiState(
            showAddProductDialog = showDialog,
            filter = filter,
            inventory = productList,
            searchQuery = query,
            lowStockProducts = lowStockList
        )
    }

    val inventoryUiState: StateFlow<InventoryScreenUiState> = combine(
        inventoryBaseUiState,
        suppliers,
        _appSettings,
        productNameSuggestions,
        _isLoading
    ) { base, supplierList, settings, suggestions, loading ->
        base.copy(
            suppliers = supplierList,
            appSettings = settings,
            productNameSuggestions = suggestions,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryScreenUiState())

    private val debtClientsBaseUiState: Flow<DebtClientsScreenUiState> = combine(
        _clientSearchQuery,
        _showPaymentDialog,
        _clientForPayment,
        _isLoading,
        _appSettings
    ) { query, showPayment, paymentClient, loading, settings ->
        DebtClientsScreenUiState(
            searchQuery = query,
            showPaymentDialog = showPayment,
            clientForPayment = paymentClient,
            isLoading = loading,
            appSettings = settings
        )
    }

    val debtClientsUiState: StateFlow<DebtClientsScreenUiState> = combine(
        debtClientsBaseUiState,
        debtCollectionSummary
    ) { base, summary ->
        base.copy(collectionSummary = summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtClientsScreenUiState())

    init {
        viewModelScope.launch {
            repository.getAllSuppliers().collect { _suppliers.value = it } // Initialize suppliers
        }
        viewModelScope.launch {
            combine(petsWithOwners, upcomingTreatments, inventory) { _, _, _ -> Unit }
                .first()
            _isLoading.value = false
        }
        viewModelScope.launch {
            clients.firstOrNull()?.let { clientList ->
                if (clientList.none { it.clientId == GENERAL_CLIENT_ID }) {
                    withContext(Dispatchers.IO) {
                        addSampleData()
                    }
                }
            }
        }
    }
    fun onReportPeriodTypeChanged(newType: ReportPeriodType) {
        _reportPeriodType.value = newType
        _selectedHistoricalPeriod.value = null
    }

    fun onHistoricalPeriodSelected(period: HistoricalPeriod) {
        _selectedHistoricalPeriod.value = period
    }

    private fun generateHistoricalPeriods(sales: List<SaleWithProducts>, type: ReportPeriodType): List<HistoricalPeriod> {
        if (sales.isEmpty()) return emptyList()
        val zoneId = ZoneId.systemDefault()
        val locale = Locale("es", "ES")

        return sales.map { Instant.ofEpochMilli(it.sale.date).atZone(zoneId).toLocalDate() }
            .distinct()
            .sortedDescending()
            .groupBy {
                when (type) {
                    ReportPeriodType.DAY -> it
                    ReportPeriodType.WEEK -> it.with(WeekFields.of(locale).dayOfWeek(), 1)
                    ReportPeriodType.MONTH -> it.withDayOfMonth(1)
                }
            }
            .map { (periodStart, _) ->
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
                        val weekOfYear = weekStart.get(WeekFields.of(locale).weekOfWeekBasedYear())
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


    private fun currentDayRangeMillis(): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        return startOfDay to endOfDay
    }

    fun onInventoryFilterChanged(newFilter: String) { _inventoryFilter.value = newFilter }
    fun onPetSearchQueryChange(query: String) { _petSearchQuery.value = query }
    fun clearPetSearchQuery() { _petSearchQuery.value = "" }
    fun onCalendarDateSelected(date: LocalDate) { _selectedCalendarDate.value = date }
    fun onClientSearchQueryChange(query: String) { _clientSearchQuery.value = query }
    fun clearClientSearchQuery() { _clientSearchQuery.value = "" }
    fun onProductSearchQueryChange(query: String) { _productSearchQuery.value = query }
    fun clearProductSearchQuery() { _productSearchQuery.value = "" }
    fun onGlobalSearchQueryChange(query: String) { _globalSearchQuery.value = query }
    fun clearGlobalSearchQuery() { _globalSearchQuery.value = "" }
    fun onSaleDateFilterSelected(date: Long?) { _selectedSaleDateFilter.value = date }
    fun clearSaleDateFilter() { _selectedSaleDateFilter.value = null }
    fun onSaleInventoryFilterSelected(newFilter: String) { _saleInventoryFilter.value = newFilter }
    fun clearSaleInventoryFilter() { _saleInventoryFilter.value = "Todos" }
    fun onProductNameChange(name: String) { _productNameSuggestionQuery.value = name }
    fun clearProductNameSuggestions() { _productNameSuggestionQuery.value = "" }
    fun onClientNameChange(name: String) { _clientNameSuggestionQuery.value = name }
    fun clearClientNameSuggestions() { _clientNameSuggestionQuery.value = "" }
    fun onTopProductsPeriodSelected(period: TopProductsPeriod) { _topProductsPeriod.value = period; _topProductsDate.value = LocalDate.now() }
    fun onTopProductsMetricSelected(metric: TopProductsMetric) {
        _topProductsMetric.value = metric
        _selectedTopProduct.value = null
    }
    fun onTopProductsDateSelected(dateMillis: Long) { _topProductsDate.value = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    fun onTopProductSelected(product: TopSellingProduct?) { _selectedTopProduct.value = if (_selectedTopProduct.value == product) null else product }
    fun onShowAddProductDialog() { _showAddProductDialog.value = true }
    fun onDismissAddProductDialog() { _showAddProductDialog.value = false; clearProductNameSuggestions() }
    fun onShowAddClientDialog() { _showAddClientDialog.value = true }
    fun onDismissAddClientDialog() { _showAddClientDialog.value = false; clearClientNameSuggestions() }
    fun onShowPaymentDialog(client: Client) { _clientForPayment.value = client; _showPaymentDialog.value = true }
    fun onDismissPaymentDialog() { _clientForPayment.value = null; _showPaymentDialog.value = false }
    fun onShowAddAppointmentDialog() { _showAddAppointmentDialog.value = true }
    fun onDismissAddAppointmentDialog() { _showAddAppointmentDialog.value = false }
    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            action()
        } catch (e: Exception) {
            reportOperationError(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun insertOrUpdateProduct(product: Product) = executeWithLoading {
        val isNewProduct = product.productId.isBlank()
        repository.insertOrUpdateProduct(product)
        reportOperationSuccess(if (isNewProduct) "Producto guardado." else "Producto actualizado.")
    }

    fun updateAppSettings(settings: AppSettings) {
        val normalizedSettings = settings.copy(securityPin = settings.securityPin.filter { it.isDigit() })
        repository.saveAppSettings(normalizedSettings)
        _appSettings.value = normalizedSettings
        reportOperationSuccess("Ajustes guardados.")
    }

    fun markBackupCreated() {
        _appSettings.value = repository.markBackupCreated()
        reportOperationSuccess("Fecha de respaldo actualizada.")
    }

    fun addClient(name: String, phone: String, debt: Double) = executeWithLoading {
        repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, address = null, debtAmount = debt))
        reportOperationSuccess("Cliente guardado.")
        onDismissAddClientDialog()
    }
    fun updateClient(client: Client) = executeWithLoading {
        repository.updateClient(client)
        reportOperationSuccess("Cliente actualizado.")
    }
    fun addPet(pet: Pet) = executeWithLoading {
        repository.insertPet(pet)
        reportOperationSuccess("Mascota guardada.")
    }
    fun updatePet(pet: Pet) = executeWithLoading {
        repository.updatePet(pet)
        reportOperationSuccess("Mascota actualizada.")
    }
    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }
    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch { repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it } }
    fun loadDebtHistoryForClient(clientId: String) = viewModelScope.launch { repository.getDebtHistoryForClient(clientId).collect { _debtHistory.value = it } }
    fun loadProductCostHistory(productId: String) = viewModelScope.launch { repository.getProductCostHistory(productId).collect { _productCostHistory.value = it } }
    fun loadProductStockMovements(productId: String) = viewModelScope.launch { repository.getProductStockMovements(productId).collect { _productStockMovements.value = it } }
    fun addTreatment(pet: Pet, description: String?, weight: Double?, temperature: String?, symptoms: String?, diagnosis: String?, treatmentPlan: String?, nextDate: Long?) = executeWithLoading {
        val newTreatment = Treatment( petIdFk = pet.petId, serviceId = null, treatmentDate = System.currentTimeMillis(), description = description, weight = weight, temperature = temperature, symptoms = symptoms, diagnosis = diagnosis, treatmentPlan = treatmentPlan, nextTreatmentDate = nextDate)
        repository.insertTreatment(newTreatment)
        reportOperationSuccess("Consulta registrada.")
    }
    fun markTreatmentAsCompleted(treatment: Treatment) = executeWithLoading {
        repository.markTreatmentAsCompleted(treatment.treatmentId)
        reportOperationSuccess("Tratamiento marcado como completado.")
    }

    fun updateTreatment(treatment: Treatment) = executeWithLoading {
        repository.updateTreatment(treatment)
        reportOperationSuccess("Consulta actualizada.")
    }
    fun deleteTreatment(treatment: Treatment) = executeWithLoading {
        repository.deleteTreatment(treatment)
        reportOperationSuccess("Consulta eliminada.")
    }

    fun makePayment(amount: Double) = executeWithLoading {
        val client = _clientForPayment.value ?: return@executeWithLoading
        val paid = kotlin.math.min(amount, client.debtAmount)
        val remainingDebt = (client.debtAmount - paid).coerceAtLeast(0.0)
        repository.makePayment(client, amount)
        reportOperationSuccess("Pago registrado. Saldo pendiente: Gs. ${remainingDebt.formatMoneyForMessage()}.")
        onDismissPaymentDialog()
    }
    fun adjustClientDebt(client: Client, newDebt: Double, note: String?) = executeWithLoading {
        repository.adjustClientDebt(client, newDebt, note)
        reportOperationSuccess("Deuda ajustada. Nuevo saldo: Gs. ${newDebt.formatMoneyForMessage()}.")
    }
    fun addAppointment(appointment: Appointment) = executeWithLoading {
        repository.insertAppointment(appointment)
        reportOperationSuccess("Cita agendada.")
    }
    fun updateAppointment(appointment: Appointment) = executeWithLoading {
        repository.updateAppointment(appointment)
        reportOperationSuccess("Cita actualizada.")
    }
    fun updateAppointmentStatus(appointment: Appointment, status: String) = executeWithLoading {
        repository.updateAppointment(appointment.copy(status = status))
        reportOperationSuccess("Estado de cita actualizado.")
    }
    fun deleteAppointment(appointment: Appointment) = executeWithLoading {
        repository.deleteAppointment(appointment)
        reportOperationSuccess("Cita eliminada.")
    }
    fun addSupplierDebt(supplierId: String?, description: String, amount: Double, dueDate: Long, note: String? = null) = executeWithLoading {
        repository.insertSupplierDebt(
            SupplierDebt(
                supplierIdFk = supplierId,
                description = description,
                amount = amount,
                dueDate = dueDate,
                createdAt = System.currentTimeMillis(),
                isPaid = false,
                note = note?.ifBlank { null }
            )
        )
        reportOperationSuccess("Deuda de proveedor registrada.")
    }
    fun markSupplierDebtAsPaid(debtId: String) = executeWithLoading {
        repository.markSupplierDebtAsPaid(debtId)
        reportOperationSuccess("Deuda de proveedor marcada como pagada.")
    }
    fun adjustProductStock(product: Product, newStock: Double, note: String) = executeWithLoading {
        repository.adjustProductStock(product, newStock, note)
        reportOperationSuccess("Stock ajustado.")
    }

    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    private suspend fun addSampleData() = repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, address = null, debtAmount = 0.0))

    private fun Double.formatMoneyForMessage(): String {
        return String.format(Locale.getDefault(), "%,.0f", this)
    }
}
