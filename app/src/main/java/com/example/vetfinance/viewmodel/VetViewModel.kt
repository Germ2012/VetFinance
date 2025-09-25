package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** ID constante para el cliente "público en general" usado en ventas rápidas. */
private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"

/** Define los períodos de tiempo disponibles para los reportes. */
enum class Period(val displayName: String) {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes")
}

/**
 * ViewModel principal de la aplicación.
 *
 * Actúa como el intermediario entre la capa de datos (VetRepository) y la UI. Se encarga de:
 * - Exponer el estado de la aplicación a la UI a través de `StateFlow`.
 * - Manejar la lógica de negocio y los eventos de la UI.
 * - Sobrevivir a los cambios de configuración (como rotaciones de pantalla).
 *
 * @property repository El repositorio que provee acceso a los datos de la aplicación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository
) : ViewModel() {

    // --- ESTADO DE CARGA GLOBAL ---
    /** Indica si hay una operación de larga duración en progreso (ej. guardado en BD). */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    //---Borrados
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun deleteSale(sale: SaleWithProducts) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    // --- ESTADOS DE FILTROS Y BÚSQUEDA ---
    /** Filtro actual para la lista de inventario ("Todos", "Productos", "Servicios"). */
    private val _inventoryFilter = MutableStateFlow("Todos")
    val inventoryFilter: StateFlow<String> = _inventoryFilter.asStateFlow()

    /** Consulta de búsqueda actual para la lista de mascotas. */
    private val _petSearchQuery = MutableStateFlow("")
    val petSearchQuery: StateFlow<String> = _petSearchQuery.asStateFlow()

    /** Consulta de búsqueda actual para la lista de clientes. */
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()

    /** Consulta de búsqueda actual para la lista de productos en la pantalla de ventas. */
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    /** Filtro de fecha (en milisegundos) para la lista de ventas. */
    private val _selectedSaleDateFilter = MutableStateFlow<Long?>(null)
    val selectedSaleDateFilter: StateFlow<Long?> = _selectedSaleDateFilter.asStateFlow()

    // --- ESTADOS DE DATOS CRUDOS (Directos del Repositorio) ---
    /** Flujo con la lista completa de todos los clientes. */
    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flujo con la lista completa de mascotas y sus respectivos dueños. */
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flujo con la lista completa de productos y servicios. */
    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flujo con el historial de tratamientos de una mascota específica. Se carga bajo demanda. */
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()

    /** Flujo con los próximos tratamientos agendados que no han sido completados. */
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flujo con el historial de pagos de un cliente específico. Se carga bajo demanda. */
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()

    /** Flujo con todas las ventas y sus productos asociados. */
    private val _sales = MutableStateFlow<List<SaleWithProducts>>(emptyList())

    // --- DATOS DERIVADOS Y PAGINADOS (Reactivos a los filtros) ---
    /**
     * Flujo paginado de clientes con deuda.
     * Reacciona a los cambios en `clientSearchQuery` para filtrar los resultados.
     */
    val debtClientsPaginated: Flow<PagingData<Client>> =
        clientSearchQuery.flatMapLatest { query ->
            repository.getDebtClientsPaginated(query)
        }.cachedIn(viewModelScope)

    /**
     * Lista de mascotas y dueños filtrada por la consulta de búsqueda.
     * Deriva de `petsWithOwners` y `_petSearchQuery`.
     */
    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> =
        combine(petsWithOwners, _petSearchQuery) { pets, query ->
            if (query.isBlank()) pets else pets.filter {
                it.pet.name.contains(query, ignoreCase = true) || it.owner.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lista de inventario filtrada por la consulta de búsqueda.
     * Deriva de `inventory` y `_productSearchQuery`.
     */
    val filteredInventory: StateFlow<List<Product>> =
        combine(inventory, _productSearchQuery) { products, query ->
            if (query.isBlank()) products else products.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lista de ventas filtrada por la fecha seleccionada.
     * Deriva de `_sales` y `_selectedSaleDateFilter`.
     */
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

    /**
     * Lista de productos con stock bajo (menos de 4 unidades).
     * Se muestra como una alerta en el Dashboard.
     */
    val lowStockProducts: StateFlow<List<Product>> = inventory.map { products ->
        products.filter { !it.isService && it.stock < 4 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- CALENDARIO Y CITAS ---
    /** La fecha actualmente seleccionada en la pantalla del calendario. */
    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    /**
     * Lista de citas para la fecha seleccionada en el calendario.
     * Se actualiza automáticamente cuando `_selectedCalendarDate` cambia.
     */
    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> =
        _selectedCalendarDate.flatMapLatest { date ->
            repository.getAppointmentsForDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- REPORTES ---
    /** Flujo con el top 10 de productos más vendidos. */
    val topSellingProducts: StateFlow<List<TopSellingProduct>> = repository.getTopSellingProducts(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flujo con la suma total de la deuda de todos los clientes. */
    val totalDebt: StateFlow<Double?> = repository.getTotalDebt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Flujo con el valor total del inventario (suma de precio * stock). */
    val totalInventoryValue: StateFlow<Double?> = repository.getTotalInventoryValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // --- ESTADOS DE LA UI (DIÁLOGOS) ---
    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _showAddClientDialog = MutableStateFlow(false)
    val showAddClientDialog: StateFlow<Boolean> = _showAddClientDialog.asStateFlow()

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _showAddAppointmentDialog = MutableStateFlow(false)
    val showAddAppointmentDialog: StateFlow<Boolean> = _showAddAppointmentDialog.asStateFlow()

    private val _productErrorMessage = MutableStateFlow<String?>(null)
    val productErrorMessage: StateFlow<String?> = _productErrorMessage.asStateFlow()

    /** Cliente actualmente seleccionado para realizar un pago. */
    private val _clientForPayment = MutableStateFlow<Client?>(null)
    val clientForPayment: StateFlow<Client?> = _clientForPayment.asStateFlow()


    // --- LÓGICA DEL CARRITO DE COMPRAS ---
    /** Mapa que representa los productos en el carrito y su cantidad. */
    private val _shoppingCart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val shoppingCart: StateFlow<Map<Product, Int>> = _shoppingCart.asStateFlow()

    /** Suma total del precio de los productos en el carrito. */
    private val _saleTotal = MutableStateFlow(0.0)
    val saleTotal: StateFlow<Double> = _saleTotal.asStateFlow()

    init {
        // Carga inicial de datos que no necesitan ser reactivos a cambios de UI.
        viewModelScope.launch { repository.getAllSales().collect { _sales.value = it } }
        // Se asegura de que el cliente "General" exista al iniciar la app.
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
    fun onShowAddAppointmentDialog() { _showAddAppointmentDialog.value = true }
    fun onDismissAddAppointmentDialog() { _showAddAppointmentDialog.value = false }

    // --- OPERACIONES CRUD ---
    /**
     * Envuelve una operación de base de datos mostrando un indicador de carga.
     */
    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try { action() } finally { _isLoading.value = false }
    }

    fun addProduct(name: String, price: Double, stock: Int, cost: Double, isService: Boolean) = executeWithLoading {
        repository.insertProduct(Product(name = name, price = price, stock = stock, cost = cost, isService = isService))
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

    /** Carga el historial de tratamientos para una mascota específica. */
    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }

    /** Carga el historial de pagos para un cliente específico. */
    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch {
        repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it }
    }

    /**
     * Añade una nueva entrada clínica (tratamiento) a una mascota.
     */
    fun addTreatment(
        pet: Pet,
        description: String,
        weight: Double?,
        temperature: Double?,
        symptoms: String?,
        diagnosis: String?,
        treatmentPlan: String?,
        nextDate: Long?
    ) = executeWithLoading {
        val newTreatment = Treatment(
            petIdFk = pet.petId,
            description = description,
            weight = weight,
            temperature = temperature,
            symptoms = symptoms,
            diagnosis = diagnosis,
            treatmentPlan = treatmentPlan,
            nextTreatmentDate = nextDate
        )
        repository.insertTreatment(newTreatment)
    }

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

    // --- LÓGICA DEL CARRITO DE COMPRAS ---
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

    /**
     * Finaliza una venta, guardando los datos y limpiando el carrito.
     * @param onFinished Lambda que se ejecuta al completarse la operación, usualmente para navegar.
     */
    fun finalizeSale(onFinished: () -> Unit) = executeWithLoading {
        val newSale = Sale(clientIdFk = GENERAL_CLIENT_ID, totalAmount = _saleTotal.value)
        repository.insertSale(newSale, _shoppingCart.value)
        clearCart()
        onFinished()
    }

    // --- CÁLCULO DE REPORTES ---
    /** Calcula el total de ventas para un período de tiempo dado (Día, Semana, Mes). */
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

    /** Calcula el beneficio bruto para un período de tiempo dado (Día, Semana, Mes). */
    fun getGrossProfitSummary(period: Period): Double {
        val now = LocalDate.now()
        val startOfPeriod = when (period) {
            Period.DAY -> now
            Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> now.withDayOfMonth(1)
        }
        val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val relevantSales = _sales.value.filter { it.sale.date >= startEpoch }

        val totalRevenue = relevantSales.sumOf { it.sale.totalAmount }
        val totalCost = relevantSales.sumOf { saleWithProducts ->
            saleWithProducts.crossRefs.sumOf { crossRef ->
                val product = saleWithProducts.products.find { it.id == crossRef.productId }
                val cost = product?.cost ?: 0.0
                cost * crossRef.quantity
            }
        }

        return totalRevenue - totalCost
    }


    // --- IMPORTACIÓN Y EXPORTACIÓN ---
    suspend fun exportarDatosCompletos(): Map<String, String> = repository.exportarDatosCompletos()
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = repository.importarDatosDesdeZIP(uri, context)

    // --- DATOS DE EJEMPLO ---
    private suspend fun addSampleData() {
        repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, debtAmount = 0.0))
    }
}