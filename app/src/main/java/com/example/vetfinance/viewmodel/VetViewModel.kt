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
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"

enum class Period(@StringRes val displayResId: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

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

    fun deleteProduct(product: Product) = viewModelScope.launch { repository.deleteProduct(product) }
    fun deleteSale(sale: SaleWithProducts) = viewModelScope.launch { repository.deleteSale(sale) }
    fun deleteClient(client: Client) = viewModelScope.launch { repository.deleteClient(client) }
    fun openContainerForBulkSale(containerProduct: Product) = viewModelScope.launch {
        if (containerProduct.containedProductId != null && containerProduct.containerSize != null) {
            repository.performInventoryTransfer(
                containerId = containerProduct.productId,
                containedId = containerProduct.containedProductId!!,
                amountToTransfer = containerProduct.containerSize!!
            )
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

    val clients: StateFlow<List<Client>> = repository.getAllClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventory: StateFlow<List<Product>> = repository.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()
    private val _sales = repository.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            !product.isService && (
                    (product.sellingMethod == SELLING_METHOD_BY_UNIT && product.stock < 4) ||
                            (product.sellingMethod == SELLING_METHOD_BY_WEIGHT_OR_AMOUNT && product.lowStockThreshold != null && product.stock < product.lowStockThreshold!!)
                    )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> = _selectedCalendarDate.flatMapLatest { date ->
        repository.getAppointmentsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingAppointments: StateFlow<List<AppointmentWithDetails>> = flow {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val threeDaysFromNow = LocalDate.now().plusDays(3).atStartOfDay(zoneId).toInstant().toEpochMilli()
        emitAll(repository.getAppointmentsForDate(now, threeDaysFromNow))
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


    init {
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
    }

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
    fun onProductNameChange(name: String) { if (name.isBlank()) { _productNameSuggestions.value = emptyList(); return }; _productNameSuggestions.value = inventory.value.filter { it.name.contains(name, ignoreCase = true) } }
    fun clearProductNameSuggestions() { _productNameSuggestions.value = emptyList() }
    fun onTopProductsPeriodSelected(period: TopProductsPeriod) { _topProductsPeriod.value = period; _topProductsDate.value = LocalDate.now() }
    fun onTopProductsDateSelected(dateMillis: Long) { _topProductsDate.value = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    fun onTopProductSelected(product: TopSellingProduct?) { _selectedTopProduct.value = if (_selectedTopProduct.value == product) null else product }
    fun onShowAddProductDialog() { _showAddProductDialog.value = true }
    fun onDismissAddProductDialog() { _showAddProductDialog.value = false; clearProductNameSuggestions() }
    fun onShowAddClientDialog() { _showAddClientDialog.value = true }
    fun onDismissAddClientDialog() { _showAddClientDialog.value = false }
    fun onShowPaymentDialog(client: Client) { _clientForPayment.value = client; _showPaymentDialog.value = true }
    fun onDismissPaymentDialog() { _clientForPayment.value = null; _showPaymentDialog.value = false }
    fun onShowAddAppointmentDialog() { _showAddAppointmentDialog.value = true }
    fun onDismissAddAppointmentDialog() { _showAddAppointmentDialog.value = false }
    fun openFractionalSaleDialog(product: Product) { _productForFractionalSale.value = product; _showFractionalSaleDialog.value = true }
    fun dismissFractionalSaleDialog() { _productForFractionalSale.value = null; _showFractionalSaleDialog.value = false }
    fun openDoseSaleDialog(product: Product) { _productForDoseSale.value = product; _showDoseSaleDialog.value = true }
    fun dismissDoseSaleDialog() { _productForDoseSale.value = null; _showDoseSaleDialog.value = false }

    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch { _isLoading.value = true; try { action() } finally { _isLoading.value = false } }
    fun addProduct(name: String, price: Double, stock: Double, cost: Double, isService: Boolean, sellingMethod: String) = executeWithLoading { repository.insertProduct(Product(name = name, price = price, stock = stock, cost = cost, isService = isService, sellingMethod = sellingMethod)); onDismissAddProductDialog() }
    fun updateProduct(product: Product) = executeWithLoading { repository.updateProduct(product) }
    fun addClient(name: String, phone: String, debt: Double) = executeWithLoading { repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, address = null, debtAmount = debt)); onDismissAddClientDialog() }
    fun updateClient(client: Client) = executeWithLoading { repository.updateClient(client) }
    fun addPet(pet: Pet) = executeWithLoading { repository.insertPet(pet) }
    fun updatePet(pet: Pet) = executeWithLoading { repository.updatePet(pet) }
    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }
    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch { repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it } }
    fun addTreatment(pet: Pet, description: String?, weight: Double?, temperature: String?, symptoms: String?, diagnosis: String?, treatmentPlan: String?, nextDate: Long?) = executeWithLoading {
        val newTreatment = Treatment( petIdFk = pet.petId, serviceId = null, treatmentDate = System.currentTimeMillis(), description = description, weight = weight, temperature = temperature, symptoms = symptoms, diagnosis = diagnosis, treatmentPlan = treatmentPlan, nextTreatmentDate = nextDate)
        repository.insertTreatment(newTreatment)
    }
    fun markTreatmentAsCompleted(treatment: Treatment) = executeWithLoading { repository.markTreatmentAsCompleted(treatment.treatmentId) }
    fun makePayment(amount: Double) = executeWithLoading { _clientForPayment.value?.let { repository.makePayment(it, amount); onDismissPaymentDialog() } }
    fun addAppointment(appointment: Appointment) = executeWithLoading { repository.insertAppointment(appointment) }
    fun updateAppointment(appointment: Appointment) = executeWithLoading { repository.updateAppointment(appointment) }
    fun deleteAppointment(appointment: Appointment) = executeWithLoading { repository.deleteAppointment(appointment) }

    fun addToCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableList()
        val existingItem = currentCart.find { it.product.productId == product.productId }

        // Solo incrementamos cantidad si es "Por Unidad" y ya existe
        if (existingItem != null && product.sellingMethod == SELLING_METHOD_BY_UNIT) {
            val newQuantity = existingItem.quantity + 1
            if (newQuantity <= product.stock) {
                val index = currentCart.indexOf(existingItem)
                currentCart[index] = existingItem.copy(quantity = newQuantity)
            }
        } else {
            // Para "Dosis" o "Peso/Monto", o si es un item nuevo, siempre se abre diálogo o se añade
            // La lógica para abrir el diálogo ya está en la UI, aquí solo añadimos uno por defecto
            if (product.sellingMethod == SELLING_METHOD_BY_UNIT) {
                currentCart.add(CartItem(product = product, quantity = 1.0))
            }
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
            val validQuantity = if (!product.isService) quantity.coerceAtMost(product.stock) else quantity
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

    fun clearCart() { _shoppingCart.value = emptyList(); _saleTotal.value = 0.0 }

    private fun recalculateTotal() {
        _saleTotal.value = _shoppingCart.value.sumOf {
            it.overridePrice ?: (it.product.price * it.quantity)
        }
    }

    fun finalizeSale(onFinished: () -> Unit) = executeWithLoading {
        if (_shoppingCart.value.isNotEmpty()) {
            repository.insertSale(
                Sale(date = System.currentTimeMillis(), totalAmount = _saleTotal.value, clientIdFk = GENERAL_CLIENT_ID),
                _shoppingCart.value
            )
            clearCart()
            onFinished()
        }
    }

    fun getSalesSummary(period: Period): Flow<Double> {
        return _sales.map { sales ->
            val now = LocalDate.now()
            val startOfPeriod = when (period) {
                Period.DAY -> now
                Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
                Period.MONTH -> now.withDayOfMonth(1)
            }
            val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            sales.filter { it.sale.date >= startEpoch }.sumOf { it.sale.totalAmount }
        }
    }

    fun getGrossProfitSummary(period: Period): Flow<Double> {
        return _sales.map { sales ->
            val now = LocalDate.now()
            val startOfPeriod = when (period) {
                Period.DAY -> now
                Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
                Period.MONTH -> now.withDayOfMonth(1)
            }
            val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val relevantSales = sales.filter { it.sale.date >= startEpoch }
            val totalRevenue = relevantSales.sumOf { it.sale.totalAmount }
            val totalCost = relevantSales.sumOf { sale ->
                sale.crossRefs.sumOf { ref ->
                    val product = sale.products.find { it.productId == ref.productId }
                    (product?.cost ?: 0.0) * ref.quantitySold
                }
            }
            totalRevenue - totalCost
        }
    }

    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    private suspend fun addSampleData() = repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, address = null, debtAmount = 0.0))
}