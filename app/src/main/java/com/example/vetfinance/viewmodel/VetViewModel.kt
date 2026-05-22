package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.R
import com.example.vetfinance.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"


enum class ReportPeriodType(@StringRes val displayResId: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

data class HistoricalPeriod(
    val id: String, // e.g., "2025-10-06", "2025-W41", "2025-10"
    val displayName: String, // e.g., "06/10/2025", "Semana 41 (06/10 - 12/10)", "Octubre 2025"
    val startDate: Long,
    val endDate: Long
)

data class GlobalSearchResult(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String
)

data class DebtCollectionRow(
    val client: Client,
    val totalSold: Double,
    val totalPaid: Double,
    val balance: Double
)

enum class TopProductsPeriod(@StringRes val displayResId: Int) {
    WEEK(R.string.topproducts_period_week),
    MONTH(R.string.topproducts_period_month),
    YEAR(R.string.topproducts_period_year)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository
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

    private val _productNameSuggestions = MutableStateFlow<List<Product>>(emptyList())
    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestions.asStateFlow()
    private val _clientNameSuggestions = MutableStateFlow<List<Client>>(emptyList())
    val clientNameSuggestions: StateFlow<List<Client>> = _clientNameSuggestions.asStateFlow()

    val clients: StateFlow<List<Client>> = repository.getAllClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    val frequentSaleProducts: StateFlow<List<Product>> = combine(inventory, _sales) { products, sales ->
        val productScores = mutableMapOf<String, Double>()
        sales.forEach { sale ->
            sale.crossRefs.forEach { ref ->
                productScores[ref.productId] = (productScores[ref.productId] ?: 0.0) + ref.quantitySold
            }
        }
        products
            .filter { productScores.containsKey(it.productId) }
            .sortedByDescending { productScores[it.productId] ?: 0.0 }
            .take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCollectionRows: StateFlow<List<DebtCollectionRow>> = combine(clients, _sales, _payments) { clientList, sales, payments ->
        val salesByClient = sales
            .groupBy { it.sale.clientIdFk }
            .mapValues { (_, clientSales) -> clientSales.sumOf { it.sale.totalAmount } }
        val paymentsByClient = payments
            .groupBy { it.clientIdFk }
            .mapValues { (_, clientPayments) -> clientPayments.sumOf { it.amount } }

        clientList
            .filter { it.debtAmount > 0.0 }
            .map { client ->
                DebtCollectionRow(
                    client = client,
                    totalSold = salesByClient[client.clientId] ?: 0.0,
                    totalPaid = paymentsByClient[client.clientId] ?: 0.0,
                    balance = client.debtAmount
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = combine(
        clients,
        petsWithOwners,
        inventory,
        _globalSearchQuery
    ) { clientList, petList, productList, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val clientResults = clientList
                .filter { it.name.contains(query, true) || (it.phone?.contains(query, true) == true) }
                .map { GlobalSearchResult(it.clientId, "client", "Cliente: ${it.name}", it.phone ?: "Sin telefono") }
            val petResults = petList
                .filter { it.pet.name.contains(query, true) || it.owner.name.contains(query, true) }
                .map { GlobalSearchResult(it.pet.petId, "pet", "Mascota: ${it.pet.name}", "Dueno: ${it.owner.name}") }
            val productResults = productList
                .filter { it.name.contains(query, true) }
                .map { product ->
                    val type = if (product.isService) "Servicio" else "Producto"
                    GlobalSearchResult(product.productId, "product", "$type: ${product.name}", product.category ?: product.sellingMethod)
                }
            (clientResults + petResults + productResults).take(12)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtClientsPaginated: Flow<PagingData<Client>> = clientSearchQuery.flatMapLatest { repository.getDebtClientsPaginated(it) }.cachedIn(viewModelScope)
    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> = combine(petsWithOwners, _petSearchQuery) { pets, query ->
        if (query.isBlank()) pets else pets.filter { it.pet.name.contains(query, true) || it.owner.name.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInventory: StateFlow<List<Product>> = combine(inventory, _productSearchQuery) { products, query ->
        if (query.isBlank()) products else products.filter { it.name.contains(query, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSales: StateFlow<List<SaleWithProducts>> = combine(_sales, _selectedSaleDateFilter) { sales, date ->
        if (date == null) sales else {
            val startOfDay = LocalDate.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            sales.filter { it.sale.date in startOfDay..endOfDay }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = inventory.map { products ->
        products.filter { product ->
            if (product.isService || product.isContainer || product.sellingMethod == SELLING_METHOD_DOSE_ONLY) {
                false
            } else {
                val threshold = product.lowStockThreshold ?: if (product.sellingMethod == SELLING_METHOD_BY_UNIT) 4.0 else 0.0
                threshold > 0.0 && product.stock < threshold
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val salesSummaryToday: StateFlow<Double> = _sales.map { sales ->
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        sales.filter { it.sale.date >= startOfDay }.sumOf { it.sale.totalAmount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private val _topProductsPeriod = MutableStateFlow(TopProductsPeriod.MONTH)
    val topProductsPeriod: StateFlow<TopProductsPeriod> = _topProductsPeriod.asStateFlow()
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

    val topSellingProducts: StateFlow<List<TopSellingProduct>> = topProductsDateRange.flatMapLatest { (start, end) ->
        repository.getTopSellingProducts(startDate = start, endDate = end, limit = 10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Estados para la funcionalidad de Reportes ---
    private val _reportPeriodType = MutableStateFlow(ReportPeriodType.DAY)
    val reportPeriodType: StateFlow<ReportPeriodType> = _reportPeriodType.asStateFlow()

    private val _selectedHistoricalPeriod = MutableStateFlow<HistoricalPeriod?>(null)
    val selectedHistoricalPeriod: StateFlow<HistoricalPeriod?> = _selectedHistoricalPeriod.asStateFlow()

    val availableHistoricalPeriods: StateFlow<List<HistoricalPeriod>> = combine(_sales, reportPeriodType) { sales, type ->
        generateHistoricalPeriods(sales, type)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesSummary: StateFlow<Double> = selectedHistoricalPeriod.map { period ->
        calculateSalesSummary(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grossProfitSummary: StateFlow<Double> = selectedHistoricalPeriod.map { period ->
        calculateGrossProfitSummary(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


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

    private val _shoppingCart = MutableStateFlow<List<CartItem>>(emptyList())
    val shoppingCart: StateFlow<List<CartItem>> = _shoppingCart.asStateFlow()
    private val _saleTotal = MutableStateFlow(0.0)
    val saleTotal: StateFlow<Double> = _saleTotal.asStateFlow()

    private val _showFractionalSaleDialog = MutableStateFlow(false)
    val showFractionalSaleDialog: StateFlow<Boolean> = _showFractionalSaleDialog.asStateFlow()
    private val _productForFractionalSale = MutableStateFlow<Product?>(null)
    val productForFractionalSale: StateFlow<Product?> = _productForFractionalSale.asStateFlow()

    private val _showDoseSaleDialog = MutableStateFlow(false)
    val showDoseSaleDialog: StateFlow<Boolean> = _showDoseSaleDialog.asStateFlow()
    private val _productForDoseSale = MutableStateFlow<Product?>(null)
    val productForDoseSale: StateFlow<Product?> = _productForDoseSale.asStateFlow()

    private val _saleTypeDialogProduct = MutableStateFlow<Product?>(null)
    val saleTypeDialogProduct: StateFlow<Product?> = _saleTypeDialogProduct.asStateFlow()


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
                    addSampleData()
                }
            }
        }
        viewModelScope.launch {
            availableHistoricalPeriods.collect { periods ->
                if (_selectedHistoricalPeriod.value == null) {
                    _selectedHistoricalPeriod.value = periods.firstOrNull()
                }
            }
        }
    }
    fun onReportPeriodTypeChanged(newType: ReportPeriodType) {
        _reportPeriodType.value = newType
        // Resetea el período seleccionado y deja que el colector lo actualice
        _selectedHistoricalPeriod.value = availableHistoricalPeriods.value.firstOrNull()
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


    private fun calculateSalesSummary(period: HistoricalPeriod?): Double {
        if (period == null) return 0.0
        return _sales.value
            .filter { it.sale.date in period.startDate..period.endDate }
            .sumOf { it.sale.totalAmount }
    }

    private fun calculateGrossProfitSummary(period: HistoricalPeriod?): Double {
        if (period == null) return 0.0
        val relevantSales = _sales.value.filter { it.sale.date in period.startDate..period.endDate }
        val totalRevenue = relevantSales.sumOf { it.sale.totalAmount }
        val totalCost = relevantSales.sumOf { sale ->
            sale.crossRefs.sumOf { ref ->
                val product = sale.products.find { it.productId == ref.productId }
                (product?.cost ?: 0.0) * ref.quantitySold
            }
        }
        return totalRevenue - totalCost
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
    fun onProductNameChange(name: String) { if (name.isBlank()) { _productNameSuggestions.value = emptyList(); return }; _productNameSuggestions.value = inventory.value.filter { it.name.contains(name, ignoreCase = true) } }
    fun clearProductNameSuggestions() { _productNameSuggestions.value = emptyList() }
    fun onClientNameChange(name: String) {
        if (name.isBlank()) {
            _clientNameSuggestions.value = emptyList()
            return
        }
        _clientNameSuggestions.value = clients.value
            .filter { it.name.contains(name, ignoreCase = true) || (it.phone?.contains(name) == true) }
            .take(6)
    }
    fun clearClientNameSuggestions() { _clientNameSuggestions.value = emptyList() }
    fun onTopProductsPeriodSelected(period: TopProductsPeriod) { _topProductsPeriod.value = period; _topProductsDate.value = LocalDate.now() }
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
    fun openFractionalSaleDialog(product: Product) { _productForFractionalSale.value = product; _showFractionalSaleDialog.value = true }
    fun dismissFractionalSaleDialog() { _productForFractionalSale.value = null; _showFractionalSaleDialog.value = false }
    fun openDoseSaleDialog(product: Product) { _productForDoseSale.value = product; _showDoseSaleDialog.value = true }
    fun dismissDoseSaleDialog() { _productForDoseSale.value = null; _showDoseSaleDialog.value = false }

    fun openSaleTypeDialog(product: Product) {
        _saleTypeDialogProduct.value = product
    }
    fun closeSaleTypeDialog() {
        _saleTypeDialogProduct.value = null
    }

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
        repository.saveAppSettings(settings)
        _appSettings.value = settings
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

    private fun shouldValidateStockInCart(product: Product): Boolean {
        return !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY
    }

    private fun availableStockForCart(product: Product): Double {
        if (!shouldValidateStockInCart(product)) return Double.MAX_VALUE
        if (product.isContainer) return product.stock

        val stockFromClosedContainers = inventory.value
            .filter { it.isContainer && it.containedProductId == product.productId && (it.containerSize ?: 0.0) > 0.0 }
            .sumOf { container ->
                val fullContainers = kotlin.math.floor(container.stock)
                fullContainers * (container.containerSize ?: 0.0)
            }

        return product.stock + stockFromClosedContainers
    }

    fun addToCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableList()
        val existingItem = currentCart.find { it.product.productId == product.productId }

        if (product.sellingMethod != SELLING_METHOD_BY_UNIT) return

        val availableStock = availableStockForCart(product)
        if (shouldValidateStockInCart(product) && availableStock < 1.0) {
            reportOperationError("Stock insuficiente para ${product.name}. Disponible: ${availableStock.formatForMessage()}.")
            return
        }

        // Solo incrementamos cantidad si es "Por Unidad" y ya existe
        if (existingItem != null) {
            val newQuantity = existingItem.quantity + 1.0
            if (!shouldValidateStockInCart(product) || newQuantity <= availableStock) {
                val index = currentCart.indexOf(existingItem)
                currentCart[index] = existingItem.copy(quantity = newQuantity)
            } else {
                reportOperationError("Stock insuficiente para ${product.name}. Disponible: ${availableStock.formatForMessage()}, solicitado: ${newQuantity.formatForMessage()}.")
            }
        } else {
            currentCart.add(CartItem(product = product, quantity = 1.0))
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentCart = _shoppingCart.value.toMutableList()
        val existingItem = currentCart.find { it.cartItemId == cartItem.cartItemId } ?: return

        if (existingItem.product.sellingMethod == SELLING_METHOD_BY_UNIT && existingItem.quantity > 1) {
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = existingItem.quantity - 1)
        } else {
            currentCart.remove(existingItem)
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun addOrUpdateProductInCart(product: Product, quantity: Double) {
        val currentCart = _shoppingCart.value.toMutableList()
        // Para ventas fraccionadas, asumimos que solo hay una entrada por producto.
        val existingItemIndex = currentCart.indexOfFirst { it.product.productId == product.productId }

        if (quantity > 0) {
            val availableStock = availableStockForCart(product)
            val validQuantity = if (shouldValidateStockInCart(product)) {
                if (availableStock <= 0.0) {
                    reportOperationError("Stock insuficiente para ${product.name}.")
                    0.0
                } else {
                    if (quantity > availableStock) {
                        reportOperationError("Stock insuficiente para ${product.name}. Se agrego solo ${availableStock.formatForMessage()}.")
                    }
                    quantity.coerceAtMost(availableStock)
                }
            } else {
                quantity
            }
            if (validQuantity > 0) {
                val newItem = CartItem(product = product, quantity = validQuantity)
                if (existingItemIndex != -1) {
                    currentCart[existingItemIndex] = newItem
                } else {
                    currentCart.add(newItem)
                }
            }
        } else {
            if (existingItemIndex != -1) {
                currentCart.removeAt(existingItemIndex)
            }
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun addOrUpdateDoseInCart(product: Product, notes: String, price: Double) {
        val currentCart = _shoppingCart.value.toMutableList()
        currentCart.add(CartItem(product = product, quantity = 1.0, notes = notes.ifBlank { null }, overridePrice = price))
        _shoppingCart.value = currentCart
        recalculateTotal()
        dismissDoseSaleDialog()
    }

    fun updateCartItemPrice(cartItem: CartItem, finalPrice: Double?, reason: String?) {
        val currentCart = _shoppingCart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.cartItemId == cartItem.cartItemId }
        if (index == -1) return

        val normalizedPrice = finalPrice?.takeIf { it > 0.0 }
        val normalizedReason = reason?.ifBlank { null }
        currentCart[index] = currentCart[index].copy(
            overridePrice = normalizedPrice,
            notes = normalizedReason
        )
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun clearCart() { _shoppingCart.value = emptyList(); _saleTotal.value = 0.0 }

    private fun recalculateTotal() {
        _saleTotal.value = _shoppingCart.value.sumOf {
            it.overridePrice ?: (it.product.price * it.quantity)
        }
    }

    fun finalizeSale(clientName: String? = null, selectedClientId: String? = null, onFinished: () -> Unit) = executeWithLoading {
        if (_shoppingCart.value.isNotEmpty()) {
            val saleClient = selectedClientId
                ?.let { clientId -> clients.value.find { it.clientId == clientId } }
                ?: repository.findOrCreateClientForSale(clientName)
            repository.insertSale(
                Sale(date = System.currentTimeMillis(), totalAmount = _saleTotal.value, clientIdFk = saleClient.clientId),
                _shoppingCart.value
            )
            clearCart()
            reportOperationSuccess("Venta registrada correctamente.")
            onFinished()
        }
    }
    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    private suspend fun addSampleData() = repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, address = null, debtAmount = 0.0))

    private fun Double.formatForMessage(): String {
        return if (this % 1.0 == 0.0) this.toLong().toString() else String.format(Locale.getDefault(), "%.3f", this)
    }

    private fun Double.formatMoneyForMessage(): String {
        return String.format(Locale.getDefault(), "%,.0f", this)
    }
}
