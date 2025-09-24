// ruta: app/src/main/java/com/example/vetfinance/viewmodel/VetViewModel.kt
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

private const val GENERAL_CLIENT_ID = "00000000-0000-0000-0000-000000000001"


enum class Period(val displayName: String) {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes")
}

@HiltViewModel
class VetViewModel @Inject constructor(
    private val repository: VetRepository
) : ViewModel() {

    // --- ESTADOS DE DATOS PRINCIPALES ---
    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _sales = MutableStateFlow<List<SaleWithProducts>>(emptyList())
    val sales: StateFlow<List<SaleWithProducts>> = _sales.asStateFlow()
    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()

    // --- ESTADOS DE BÚSQUEDA ---
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()
    private val _petSearchQuery = MutableStateFlow("")
    val petSearchQuery: StateFlow<String> = _petSearchQuery.asStateFlow()

    // --- ESTADOS DE LA UI (DIÁLOGOS, FILTROS, ETC.) ---
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
    private val _selectedSaleDateFilter = MutableStateFlow<Long?>(null)
    val selectedSaleDateFilter: StateFlow<Long?> = _selectedSaleDateFilter.asStateFlow()

    // --- ESTADOS DEL CARRITO DE COMPRAS ---
    private val _shoppingCart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val shoppingCart: StateFlow<Map<Product, Int>> = _shoppingCart.asStateFlow()
    private val _saleTotal = MutableStateFlow(0.0)
    val saleTotal: StateFlow<Double> = _saleTotal.asStateFlow()

    // --- ESTADOS DE MASCOTAS Y TRATAMIENTOS ---
    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()
    val upcomingTreatments: StateFlow<List<Treatment>> = repository.getUpcomingTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FLUJOS DE DATOS DERIVADOS (FILTRADOS) ---
    val filteredSales: StateFlow<List<SaleWithProducts>> =
        combine(_sales, _selectedSaleDateFilter) { sales, selectedDateMillis ->
            if (selectedDateMillis == null) {
                sales
            } else {
                val selectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                sales.filter {
                    val saleDate = Instant.ofEpochMilli(it.sale.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    saleDate.isEqual(selectedDate)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtClients: StateFlow<List<Client>> = clients.map { allClients ->
        allClients.filter { it.debtAmount > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInventory: StateFlow<List<Product>> =
        combine(inventory, _productSearchQuery) { products, query ->
            if (query.isBlank()) products else products.filter { it.name.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDebtClients: StateFlow<List<Client>> =
        combine(debtClients, _clientSearchQuery) { clients, query ->
            if (query.isBlank()) clients else clients.filter { it.name.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> =
        combine(petsWithOwners, _petSearchQuery) { pets, query ->
            if (query.isBlank()) pets else pets.filter {
                it.pet.name.contains(query, ignoreCase = true) || it.owner.name.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.getAllSales().collect { _sales.value = it } }
        viewModelScope.launch {
            // Using firstOrNull() is safer but might not be what you intend.
            // A better check might be repository.getProductCount() == 0
            if (repository.getAllProducts().firstOrNull()?.isEmpty() == true) {
                addSampleData()
            } else if (clients.value.none { it.clientId == GENERAL_CLIENT_ID }) {
                // Ensures the general client exists if the DB is not empty
                addSampleData()
            }
        }
    }

    // --- MANEJO DE BÚSQUEDAS Y FILTROS ---
    fun onProductSearchQueryChange(query: String) { _productSearchQuery.value = query }
    fun clearProductSearchQuery() { _productSearchQuery.value = "" }
    fun onClientSearchQueryChange(query: String) { _clientSearchQuery.value = query }
    fun clearClientSearchQuery() { _clientSearchQuery.value = "" }
    fun onPetSearchQueryChange(query: String) { _petSearchQuery.value = query }
    fun clearPetSearchQuery() { _petSearchQuery.value = "" }
    fun onSaleDateFilterSelected(dateMillis: Long?) { _selectedSaleDateFilter.value = dateMillis }
    fun clearSaleDateFilter() { _selectedSaleDateFilter.value = null }

    // --- Lógica de Negocio delegada al Repositorio ---
    fun addPet(name: String, owner: Client) = viewModelScope.launch { repository.insertPet(Pet(name = name, ownerIdFk = owner.clientId)) }
    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch { repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it } }
    fun addTreatment(pet: Pet, description: String, nextDate: Long?) = viewModelScope.launch { repository.insertTreatment(Treatment(petIdFk = pet.petId, description = description, nextTreatmentDate = nextDate)) }
    fun markTreatmentAsCompleted(treatment: Treatment) = viewModelScope.launch { repository.markTreatmentAsCompleted(treatment.treatmentId) }
    fun onShowAddProductDialog() { _showAddProductDialog.value = true }
    fun onDismissAddProductDialog() { _showAddProductDialog.value = false; _productErrorMessage.value = null }
    fun onShowAddClientDialog() { _showAddClientDialog.value = true }
    fun onDismissAddClientDialog() { _showAddClientDialog.value = false }
    fun onShowPaymentDialog(client: Client) { _clientForPayment.value = client; _showPaymentDialog.value = true }
    fun onDismissPaymentDialog() { _clientForPayment.value = null; _showPaymentDialog.value = false }
    fun makePayment(amount: Double) = viewModelScope.launch { _clientForPayment.value?.let { repository.makePayment(it, amount); onDismissPaymentDialog() } }
    fun loadPaymentsForClient(clientId: String) = viewModelScope.launch { repository.getPaymentsForClient(clientId).collect { _paymentHistory.value = it } }
    fun addToCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        val currentQuantity = currentCart[product] ?: 0
        if (!product.isService && currentQuantity >= product.stock) return
        currentCart[product] = currentQuantity + 1
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun removeFromCart(product: Product) {
        val currentCart = _shoppingCart.value.toMutableMap()
        val currentQuantity = currentCart[product] ?: 1
        if (currentQuantity > 1) {
            currentCart[product] = currentQuantity - 1
        } else {
            currentCart.remove(product)
        }
        _shoppingCart.value = currentCart
        recalculateTotal()
    }

    fun clearCart() {
        _shoppingCart.value = emptyMap()
        _saleTotal.value = 0.0
    }

    private fun recalculateTotal() {
        _saleTotal.value = _shoppingCart.value.entries.sumOf { (product, quantity) ->
            product.price * quantity
        }
    }
    fun finalizeSale() = viewModelScope.launch {
        val clientId = GENERAL_CLIENT_ID
        val newSale = Sale(
            clientIdFk = clientId,
            totalAmount = _saleTotal.value
        )
        repository.insertSale(newSale, _shoppingCart.value)
        clearCart()
    }

    fun getSalesSummary(period: Period): Double {
        val now = LocalDate.now()
        val startOfPeriod: LocalDate = when (period) {
            Period.DAY -> now
            Period.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> now.withDayOfMonth(1)
        }
        val startEpoch = startOfPeriod.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return _sales.value
            .filter { it.sale.date >= startEpoch }
            .sumOf { it.sale.totalAmount }
    }
    fun addProduct(name: String, price: Double, stock: Int, isService: Boolean) = viewModelScope.launch {
        if (repository.productExists(name)) {
            _productErrorMessage.value = "Un producto con este nombre ya existe."
        } else {
            repository.insertProduct(Product(name = name, price = price, stock = stock, isService = isService))
            onDismissAddProductDialog()
        }
    }
    fun addClient(name: String, phone: String, debtAmount: Double) = viewModelScope.launch {
        val clientPhone = phone.ifBlank { null }
        repository.insertClient(Client(name = name, phone = clientPhone, debtAmount = debtAmount))
        onDismissAddClientDialog()
    }

    // --- IMPORTACIÓN Y EXPORTACIÓN ---
    suspend fun exportarDatosCompletos(): Map<String, String> {
        return repository.exportarDatosCompletos()
    }

    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String {
        return repository.importarDatosDesdeZIP(uri, context)
    }

    // --- FUNCIONES PRIVADAS Y DE EJEMPLO ---
    private suspend fun addSampleData() {
        // This function populates the database with sample data if it's empty,
        // like creating a "General Client".
        val generalClientExists = clients.value.any { it.clientId == GENERAL_CLIENT_ID}
        if (!generalClientExists) {
            repository.insertClient(Client(clientId = GENERAL_CLIENT_ID, name = "Cliente General", phone = null, debtAmount = 0.0))
        }
    }
}