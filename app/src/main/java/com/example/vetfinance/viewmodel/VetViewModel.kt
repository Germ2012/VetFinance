
package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// ID constante para el cliente general usado en ventas sin cliente específico.
private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"

// Enum para los periodos de tiempo en los reportes.
enum class Period(val displayName: String) {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes")
}

@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository
) : ViewModel() {

    // --- ESTADO DE CARGA GLOBAL ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- ESTADOS DE DATOS PRINCIPALES (RAW)---
    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _sales = MutableStateFlow<List<SaleWithProducts>>(emptyList())
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()


    // --- FILTROS Y BÚSQUEDA ---
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


    // --- DATOS DERIVADOS (FILTRADOS) ---
    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> =
        combine(petsWithOwners, _petSearchQuery) { pets, query ->
            if (query.isBlank()) pets else pets.filter {
                it.pet.name.contains(query, ignoreCase = true) || it.owner.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInventory: StateFlow<List<Product>> =
        combine(inventory, _productSearchQuery) { products, query ->
            if (query.isBlank()) products else products.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSales: StateFlow<List<SaleWithProducts>> =
        combine(_sales, _selectedSaleDateFilter) { sales, date ->
            if (date == null) {
                sales
            } else {
                val startOfDay = LocalDate.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
                sales.filter { it.sale.date in startOfDay..endOfDay }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- CALENDARIO Y CITAS ---
    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    // 👇 CORRECCIÓN: Se vuelve a añadir la lógica para las citas de la fecha seleccionada
    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> =
        _selectedCalendarDate.flatMapLatest { date ->
            repository.getAppointmentsForDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- REPORTES ---
    val topSellingProducts: StateFlow<List<TopSellingProduct>> = repository.getTopSellingProducts(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDebt: StateFlow<Double?> = repository.getTotalDebt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- ESTADOS DE LA UI (DIÁLOGOS, ERRORES, ETC.) ---
    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()
    private val _showAddClientDialog = MutableStateFlow(false)
    val showAddClientDialog: StateFlow<Boolean> = _showAddClientDialog.asStateFlow()
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()
    private val _productErrorMessage = MutableStateFlow<String?>(null)
    val productErrorMessage: StateFlow<String?> = _productErrorMessage.asStateFlow()
    private val _clientForPayment = MutableStateFlow<Client?>(null)
    val clientForPayment: StateFlow<Client?> = _clientForPayment.asStateFlow()

    // --- CARRITO DE COMPRAS ---
    private val _shoppingCart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val shoppingCart: StateFlow<Map<Product, Int>> = _shoppingCart.asStateFlow()
    private val _saleTotal = MutableStateFlow(0.0)
    val saleTotal: StateFlow<Double> = _saleTotal.asStateFlow()

    init {
        // Carga inicial de datos no paginados
        viewModelScope.launch { repository.getAllSales().collect { _sales.value = it } }
        // Asegura que los datos iniciales (como el Cliente General) existan
        viewModelScope.launch {
            if (repository.getAllClients().firstOrNull()?.none { it.clientId == GENERAL_CLIENT_ID } == true) {
                addSampleData()
            }
        }
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


    // --- GESTIÓN DE DIÁLOGOS ---
    fun onShowAddProductDialog() { _showAddProductDialog.value = true }
    fun onDismissAddProductDialog() { _showAddProductDialog.value = false; _productErrorMessage.value = null }
    fun onShowAddClientDialog() { _showAddClientDialog.value = true }
    fun onDismissAddClientDialog() { _showAddClientDialog.value = false }
    fun onShowPaymentDialog(client: Client) { _clientForPayment.value = client; _showPaymentDialog.value = true }
    fun onDismissPaymentDialog() { _clientForPayment.value = null; _showPaymentDialog.value = false }

    // --- OPERACIONES CRUD (CON ESTADO DE CARGA) ---
    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try { action() } finally { _isLoading.value = false }
    }

    fun addProduct(name: String, price: Double, stock: Int, isService: Boolean) = executeWithLoading {
        repository.insertProduct(Product(name = name, price = price, stock = stock, isService = isService))
        onDismissAddProductDialog()
    }

    fun updateProduct(product: Product) = executeWithLoading { repository.updateProduct(product) }

    fun addClient(name: String, phone: String, debt: Double) = executeWithLoading {
        repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, debtAmount = debt))
        onDismissAddClientDialog()
    }

    fun updateClient(client: Client) = executeWithLoading { repository.updateClient(client) }

    fun addPet(pet: Pet) = executeWithLoading { repository.insertPet(pet) }

    fun updatePet(pet: Pet) = executeWithLoading { repository.updatePet(pet) }

    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }

    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch {
        repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it }
    }

    fun addTreatment(pet: Pet, description: String, nextDate: Long?) = executeWithLoading { repository.insertTreatment(Treatment(petIdFk = pet.petId, description = description, nextTreatmentDate = nextDate)) }

    fun markTreatmentAsCompleted(treatment: Treatment) = executeWithLoading { repository.markTreatmentAsCompleted(treatment.treatmentId) }

    fun makePayment(amount: Double) = executeWithLoading {
        _clientForPayment.value?.let {
            repository.makePayment(it, amount)
            onDismissPaymentDialog()
        }
    }

    fun addAppointment(appointment: Appointment) = executeWithLoading { repository.insertAppointment(appointment) }
    fun updateAppointment(appointment: Appointment) = executeWithLoading { repository.updateAppointment(appointment) }
    fun deleteAppointment(appointment: Appointment) = executeWithLoading { repository.deleteAppointment(appointment) }

    // --- LÓGICA DEL CARRITO Y VENTAS ---
    fun addToCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        val currentQuantity = currentCart[product] ?: 0
        if (!product.isService && currentQuantity >= product.stock) return // Evita vender más del stock
        currentCart[product] = currentQuantity + 1
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun removeFromCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        currentCart[product]?.let { quantity ->
            if (quantity > 1) currentCart[product] = quantity - 1
            else currentCart.remove(product)
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun clearCart() {
        _shoppingCart.value = emptyMap()
        _saleTotal.value = 0.0
    }

    private fun recalculateTotal() {
        _saleTotal.value = _shoppingCart.value.entries.sumOf { (product, quantity) -> product.price * quantity }
    }

    fun finalizeSale(onFinished: () -> Unit) = executeWithLoading {
        val newSale = Sale(clientIdFk = GENERAL_CLIENT_ID, totalAmount = _saleTotal.value)
        repository.insertSale(newSale, _shoppingCart.value)
        clearCart()
        onFinished()
    }

    // --- CÁLCULO DE REPORTES ---
    fun getSalesSummary(period: Period): Double {
        val now = LocalDate.now()
        val startOfPeriod = when (period) {
            Period.DAY -> now
            Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> now.withDayOfMonth(1)
        }
        val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return _sales.value
            .filter { it.sale.date >= startEpoch }
            .sumOf { it.sale.totalAmount }
    }

    // --- IMPORTACIÓN Y EXPORTACIÓN ---
    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    // --- DATOS DE EJEMPLO ---
    private suspend fun addSampleData() {
        repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, debtAmount = 0.0))
    }
}
