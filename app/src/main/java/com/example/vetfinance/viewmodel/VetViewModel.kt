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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/** ID constante para el cliente "público en general" usado en ventas rápidas. */
private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"

/** Define los períodos de tiempo disponibles para los reportes de ventas generales. */
enum class Period(@StringRes val displayResId: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

/** Define los períodos de tiempo para el reporte de Top Productos. */
enum class TopProductsPeriod(@StringRes val displayResId: Int) {
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    YEAR(R.string.top_products_period_year)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository,
    @ApplicationContext private val context: Context // Inyectar Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    //---Borrados
    fun deleteProduct(product: Product) = viewModelScope.launch { repository.deleteProduct(product) }
    fun deleteSale(sale: SaleWithProducts) = viewModelScope.launch { repository.deleteSale(sale) }
    fun deleteClient(client: Client) = viewModelScope.launch { repository.deleteClient(client) }

    // --- ESTADOS DE FILTROS Y BÚSQUEDA ---
    private val _inventoryFilter = MutableStateFlow(context.getString(R.string.inventory_filter_all)) // Usar context
    val inventoryFilter: StateFlow<String> = _inventoryFilter.asStateFlow()
    private val _petSearchQuery = MutableStateFlow("")
    val petSearchQuery: StateFlow<String> = _petSearchQuery.asStateFlow()
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()
    private val _selectedSaleDateFilter = MutableStateFlow<Long?>(null)
    val selectedSaleDateFilter: StateFlow<Long?> = _selectedSaleDateFilter.asStateFlow()

    // --- BÚSQUEDA DE PRODUCTOS EN DIÁLOGO ---
    private val _productNameSuggestions = MutableStateFlow<List<Product>>(emptyList())
    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestions.asStateFlow()

    // --- ESTADOS DE DATOS CRUDOS ---
    val clients: StateFlow<List<Client>> = repository.getAllClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventory: StateFlow<List<Product>> = repository.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()
    private val _sales = MutableStateFlow<List<SaleWithProducts>>(emptyList())

    // --- DATOS DERIVADOS Y PAGINADOS ---
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
        products.filter { !it.isService && it.selling_method != SellingMethod.DOSE_ONLY && it.stock < 4 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CALENDARIO Y CITAS ---
    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> = _selectedCalendarDate.flatMapLatest { date ->
        repository.getAppointmentsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- REPORTES ---
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

    // --- ESTADOS DE LA UI (DIÁLOGOS) ---
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

    // --- Fractional Sale Dialog States ---
    private val _showFractionalSaleDialog = MutableStateFlow(false)
    val showFractionalSaleDialog: StateFlow<Boolean> = _showFractionalSaleDialog.asStateFlow()
    private val _productForFractionalSale = MutableStateFlow<Product?>(null)
    val productForFractionalSale: StateFlow<Product?> = _productForFractionalSale.asStateFlow()

    // --- LÓGICA DEL CARRITO DE COMPRAS ---
    private val _shoppingCart = MutableStateFlow<Map<Product, Double>>(emptyMap()) // <-- CAMBIAR a Double (Already Double)
    val shoppingCart: StateFlow<Map<Product, Double>> = _shoppingCart.asStateFlow() // <-- CAMBIAR a Double (Already Double)
    private val _saleTotal = MutableStateFlow(0.0)
    val saleTotal: StateFlow<Double> = _saleTotal.asStateFlow()

    init {
        viewModelScope.launch {
            combine(petsWithOwners, upcomingTreatments, inventory) { _, _, _ -> Unit }
                .first()
            _isLoading.value = false
        }
        viewModelScope.launch { repository.getAllSales().collect { _sales.value = it } }
        viewModelScope.launch { if (repository.getAllClients().firstOrNull()?.none { it.clientId == GENERAL_CLIENT_ID } == true) addSampleData() }
    }

    // --- MANEJO DE EVENTOS DE LA UI ---
    fun onInventoryFilterChanged(newFilter: String) { _inventoryFilter.value = newFilter }
    fun onPetSearchQueryChange(query: String) { _petSearchQuery.value = query }
    fun clearPetSearchQuery() { _petSearchQuery.value = "" }
    fun onCalendarDateSelected(date: LocalDate) { _selectedCalendarDate.value = date }
    fun onClientSearchQueryChange(query: String) { _clientSearchQuery.value = query }
    fun clearClientSearchQuery() { _clientSearchQuery.value = "" }
    fun onProductSearchQueryChange(query: String) { _productSearchQuery.value = query }
    fun clearProductSearchQuery() { _productSearchQuery.value = "" }
    fun onSaleDateFilterSelected(date: Long?) { _selectedSaleDateFilter.value = date }
    fun clearSaleDateFilter() { _selectedSaleDateFilter.value = null }

    fun onProductNameChange(name: String) {
        if (name.isBlank()) {
            _productNameSuggestions.value = emptyList()
            return
        }
        _productNameSuggestions.value = inventory.value.filter {
            it.name.contains(name, ignoreCase = true)
        }
    }
    fun clearProductNameSuggestions() { _productNameSuggestions.value = emptyList() }

    // --- MANEJO DE EVENTOS PARA TOP PRODUCTS ---
    fun onTopProductsPeriodSelected(period: TopProductsPeriod) { _topProductsPeriod.value = period; _topProductsDate.value = LocalDate.now() }
    fun onTopProductsDateSelected(dateMillis: Long) { _topProductsDate.value = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    fun onTopProductSelected(product: TopSellingProduct?) { _selectedTopProduct.value = if (_selectedTopProduct.value == product) null else product }

    // --- GESTIÓN DE DIÁLOGOS ---
    fun onShowAddProductDialog() { _showAddProductDialog.value = true }
    fun onDismissAddProductDialog() { _showAddProductDialog.value = false; clearProductNameSuggestions() }
    fun onShowAddClientDialog() { _showAddClientDialog.value = true }
    fun onDismissAddClientDialog() { _showAddClientDialog.value = false }
    fun onShowPaymentDialog(client: Client) { _clientForPayment.value = client; _showPaymentDialog.value = true }
    fun onDismissPaymentDialog() { _clientForPayment.value = null; _showPaymentDialog.value = false }
    fun onShowAddAppointmentDialog() { _showAddAppointmentDialog.value = true }
    fun onDismissAddAppointmentDialog() { _showAddAppointmentDialog.value = false }

    fun openFractionalSaleDialog(product: Product) {
        _productForFractionalSale.value = product
        _showFractionalSaleDialog.value = true
    }
    fun dismissFractionalSaleDialog() {
        _productForFractionalSale.value = null
        _showFractionalSaleDialog.value = false
    }

    // --- OPERACIONES CRUD ---
    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch { _isLoading.value = true; try { action() } finally { _isLoading.value = false } }
    
    fun addProduct(name: String, price: Double, stock: Double, cost: Double, isService: Boolean, sellingMethod: SellingMethod) = executeWithLoading { // <-- CAMBIAR stock a Double y añadir sellingMethod (Already Double, sellingMethod already present)
        repository.insertProduct(Product(name = name, price = price, stock = stock, cost = cost, isService = isService, selling_method = sellingMethod)) // <-- AÑADIR selling_method (Already present)
        onDismissAddProductDialog()
    }
    fun updateProduct(product: Product) = executeWithLoading { repository.updateProduct(product) } // Ensure product passed has Double stock and SellingMethod
    fun addClient(name: String, phone: String, debt: Double) = executeWithLoading { repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, debtAmount = debt)); onDismissAddClientDialog() }
    fun updateClient(client: Client) = executeWithLoading { repository.updateClient(client) }
    fun addPet(pet: Pet) = executeWithLoading { repository.insertPet(pet) }
    fun updatePet(pet: Pet) = executeWithLoading { repository.updatePet(pet) }
    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }
    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch { repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it } }
    fun addTreatment(pet: Pet, description: String, weight: Double?, temperature: Double?, symptoms: String?, diagnosis: String?, treatmentPlan: String?, nextDate: Long?) = executeWithLoading {
        val newTreatment = Treatment(petIdFk = pet.petId, description = description, weight = weight, temperature = temperature, symptoms = symptoms, diagnosis = diagnosis, treatmentPlan = treatmentPlan, nextTreatmentDate = nextDate)
        repository.insertTreatment(newTreatment)
    }
    fun markTreatmentAsCompleted(treatment: Treatment) = executeWithLoading { repository.markTreatmentAsCompleted(treatment.treatmentId) }
    fun makePayment(amount: Double) = executeWithLoading { _clientForPayment.value?.let { repository.makePayment(it, amount); onDismissPaymentDialog() } }
    fun addAppointment(appointment: Appointment) = executeWithLoading { repository.insertAppointment(appointment) }
    fun updateAppointment(appointment: Appointment) = executeWithLoading { repository.updateAppointment(appointment) }
    fun deleteAppointment(appointment: Appointment) = executeWithLoading { repository.deleteAppointment(appointment) }

    // --- LÓGICA DEL CARRITO DE COMPRAS (Funciones) ---
    fun addToCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        val currentQuantity = currentCart[product] ?: 0.0
        // Lógica para no exceder el stock
        if (product.selling_method != SellingMethod.DOSE_ONLY && !product.isService && currentQuantity >= product.stock) return
        currentCart[product] = currentQuantity + 1.0
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun removeFromCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        val currentQuantity = currentCart[product] ?: 0.0
        if (currentQuantity > 1.0) {
            currentCart[product] = currentQuantity - 1.0
        } else {
            currentCart.remove(product)
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun addOrUpdateProductInCart(product: Product, quantity: Double) {
        val currentCart = _shoppingCart.value.toMutableMap()
        if (quantity <= 0.0) {
            currentCart.remove(product)
        } else {
            // Stock check only if not a service and not DOSE_ONLY
            if (!product.isService && product.selling_method != SellingMethod.DOSE_ONLY) {
                if (quantity > product.stock) {
                    // Optionally, provide user feedback about insufficient stock here (e.g., via a Toast or a SnackBar event)
                    // For now, just cap at available stock. Or prevent adding if more granular control is needed.
                    currentCart[product] = product.stock
                } else {
                    currentCart[product] = quantity
                }
            } else {
                 currentCart[product] = quantity
            }
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun clearCart() { _shoppingCart.value = emptyMap(); _saleTotal.value = 0.0 }
    private fun recalculateTotal() { _saleTotal.value = _shoppingCart.value.entries.sumOf { (product, quantity) -> product.price * quantity } }
    
    fun finalizeSale(onFinished: () -> Unit) = executeWithLoading {
        if (_shoppingCart.value.isNotEmpty()) {
            repository.insertSale(
                Sale(clientIdFk = GENERAL_CLIENT_ID, totalAmount = _saleTotal.value),
                _shoppingCart.value // <-- El valor ya es Map<Product, Double>
            )
            clearCart()
            onFinished()
        }
    }

    // --- CÁLCULO DE REPORTES ---
    fun getSalesSummary(period: Period): Double { // Remains the same as it sums totalAmount from Sale entity
        val now = LocalDate.now()
        val startOfPeriod = when (period) {
            Period.DAY -> now
            Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> now.withDayOfMonth(1)
        }
        val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return _sales.value.filter { it.sale.date >= startEpoch }.sumOf { it.sale.totalAmount }
    }

    fun getGrossProfitSummary(period: Period): Double { // Needs to use Double for quantity from crossRefs
        val now = LocalDate.now()
        val startOfPeriod = when (period) {
            Period.DAY -> now
            Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> now.withDayOfMonth(1)
        }
        val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val relevantSales = _sales.value.filter { it.sale.date >= startEpoch }
        val totalRevenue = relevantSales.sumOf { it.sale.totalAmount }
        // SaleProductCrossRef.quantity is now Double, so this should work correctly.
        val totalCost = relevantSales.sumOf { sale -> sale.crossRefs.sumOf { ref -> (sale.products.find { it.id == ref.productId }?.cost ?: 0.0) * ref.quantity } }
        return totalRevenue - totalCost
    }

    // --- IMPORTACIÓN Y EXPORTACIÓN ---
    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    // --- DATOS DE EJEMPLO ---
    private suspend fun addSampleData() = repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = context.getString(R.string.sample_data_general_client_name), phone = null, debtAmount = 0.0))
}
