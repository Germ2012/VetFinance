package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.AppSettings
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.DebtCollectionRow
import com.example.vetfinance.data.DebtCollectionSummary
import com.example.vetfinance.data.Pet
import com.example.vetfinance.data.PetWithOwner
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.data.SupplierDebtWithSupplier
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.domain.model.GlobalSearchResult
import com.example.vetfinance.domain.usecase.GetDebtCollectionSummaryUseCase
import com.example.vetfinance.domain.usecase.GetGlobalSearchResultsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val DASHBOARD_SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VetRepository,
    private val getGlobalSearchResultsUseCase: GetGlobalSearchResultsUseCase,
    private val getDebtCollectionSummaryUseCase: GetDebtCollectionSummaryUseCase,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _productNameSuggestionQuery = MutableStateFlow("")
    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestionQuery
        .debounce(DASHBOARD_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchProductSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val debouncedGlobalSearchQuery = _globalSearchQuery
        .debounce(DASHBOARD_SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = debouncedGlobalSearchQuery
        .flatMapLatest { query -> getGlobalSearchResultsUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<Product>> = repository.getServices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProductsByName: StateFlow<List<Product>> = repository.getLowStockProductsByName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCollectionPreviewRows: StateFlow<List<DebtCollectionRow>> = repository.getDebtCollectionPreviewRows(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCollectionSummary: StateFlow<DebtCollectionSummary> = getDebtCollectionSummaryUseCase(
        searchQuery = "",
        includeZeroDebt = false,
        minimumDebt = 0.0
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtCollectionSummary(0, 0.0, 0.0))

    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val petIdToNameMap: StateFlow<Map<String, String>> = petsWithOwners
        .map { pets -> pets.associate { it.pet.petId to it.pet.name } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingAppointments: StateFlow<List<AppointmentWithDetails>> = _appSettings.flatMapLatest { settings ->
        val zoneId = ZoneId.systemDefault()
        val now = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val alertLimit = LocalDate.now()
            .plusDays(settings.treatmentAlertDays.toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        repository.getAppointmentsForDate(now, alertLimit).map { appointments ->
            appointments.filter { it.appointment.status == com.example.vetfinance.data.APPOINTMENT_STATUS_PENDING }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingSupplierDebts: StateFlow<List<SupplierDebtWithSupplier>> = _appSettings.flatMapLatest { settings ->
        val dateLimit = LocalDate.now()
            .plusDays(settings.supplierDebtAlertDays.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        repository.getUpcomingSupplierDebts(dateLimit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayRangeMillis = currentDayRangeMillis()
    val salesSummaryToday: StateFlow<Double> = repository
        .getSalesTotalsForRange(todayRangeMillis.first, todayRangeMillis.second)
        .map { it.salesTotal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _clientForPayment = MutableStateFlow<Client?>(null)
    val clientForPayment: StateFlow<Client?> = _clientForPayment.asStateFlow()

    init {
        viewModelScope.launch {
            salesSummaryToday.first()
            lowStockProductsByName.first()
            pendingCollectionSummary.first()
            upcomingTreatments.first()
            upcomingAppointments.first()
            upcomingSupplierDebts.first()
            _isLoading.value = false
        }
    }

    fun refreshAppSettings() {
        _appSettings.value = repository.getAppSettings()
    }

    fun onGlobalSearchQueryChange(query: String) {
        _globalSearchQuery.value = query
    }

    fun clearGlobalSearchQuery() {
        _globalSearchQuery.value = ""
    }

    fun onProductNameChange(name: String) {
        _productNameSuggestionQuery.value = name
    }

    fun clearProductNameSuggestions() {
        _productNameSuggestionQuery.value = ""
    }

    fun onShowAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun onDismissAddProductDialog() {
        _showAddProductDialog.value = false
        clearProductNameSuggestions()
    }

    fun onShowPaymentDialog(client: Client) {
        _clientForPayment.value = client
        _showPaymentDialog.value = true
    }

    fun onDismissPaymentDialog() {
        _clientForPayment.value = null
        _showPaymentDialog.value = false
    }

    fun insertOrUpdateProduct(product: Product) = executeSafely {
        val isNewProduct = product.productId.isBlank()
        repository.insertOrUpdateProduct(product)
        appEventBus.reportOperationSuccess(if (isNewProduct) "Producto guardado." else "Producto actualizado.")
        onDismissAddProductDialog()
    }

    fun addTreatment(
        pet: Pet,
        description: String?,
        weight: Double?,
        temperature: String?,
        symptoms: String?,
        diagnosis: String?,
        treatmentPlan: String?,
        nextDate: Long?
    ) = executeSafely {
        val newTreatment = Treatment(
            treatmentId = UUID.randomUUID().toString(),
            petIdFk = pet.petId,
            serviceId = null,
            treatmentDate = System.currentTimeMillis(),
            description = description,
            weight = weight,
            temperature = temperature,
            symptoms = symptoms,
            diagnosis = diagnosis,
            treatmentPlan = treatmentPlan,
            nextTreatmentDate = nextDate
        )
        repository.insertTreatment(newTreatment)
        appEventBus.reportOperationSuccess("Consulta registrada.")
    }

    fun markTreatmentAsCompleted(treatment: Treatment) = executeSafely {
        repository.markTreatmentAsCompleted(treatment.treatmentId)
        appEventBus.reportOperationSuccess("Tratamiento marcado como completado.")
    }

    fun markSupplierDebtAsPaid(debtId: String) = executeSafely {
        repository.markSupplierDebtAsPaid(debtId)
        appEventBus.reportOperationSuccess("Deuda de proveedor marcada como pagada.")
    }

    fun makePayment(amount: Double) = executeSafely {
        val client = _clientForPayment.value ?: return@executeSafely
        val paid = kotlin.math.min(amount, client.debtAmount)
        val remainingDebt = (client.debtAmount - paid).coerceAtLeast(0.0)
        repository.makePayment(client, amount)
        appEventBus.reportOperationSuccess("Pago registrado. Saldo pendiente: Gs. ${remainingDebt.formatMoneyForMessage()}.")
        onDismissPaymentDialog()
    }

    private fun executeSafely(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
        } catch (e: Exception) {
            appEventBus.reportOperationError(e)
        }
    }

    private fun currentDayRangeMillis(): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        return startOfDay to endOfDay
    }

    private fun Double.formatMoneyForMessage(): String {
        return String.format(Locale.getDefault(), "%,.0f", this)
    }
}
