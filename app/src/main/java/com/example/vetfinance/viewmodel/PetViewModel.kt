package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Pet
import com.example.vetfinance.data.PetWithOwner
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.data.Treatment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val PET_SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PetViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _petSearchQuery = MutableStateFlow("")
    val petSearchQuery: StateFlow<String> = _petSearchQuery.asStateFlow()

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _clientNameSuggestionQuery = MutableStateFlow("")
    private val _productNameSuggestionQuery = MutableStateFlow("")
    private val _containedProductSearchQuery = MutableStateFlow("")
    private val _selectedContainedProductId = MutableStateFlow<String?>(null)
    private val _treatmentHistory = MutableStateFlow<List<Treatment>>(emptyList())
    val treatmentHistory: StateFlow<List<Treatment>> = _treatmentHistory.asStateFlow()

    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientNameSuggestions: StateFlow<List<Client>> = _clientNameSuggestionQuery
        .debounce(PET_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchClientSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestionQuery
        .debounce(PET_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchProductSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val containedProductSuggestions: StateFlow<List<Product>> = _containedProductSearchQuery
        .debounce(PET_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.searchContainedProductCandidates(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedContainedProduct: StateFlow<Product?> = _selectedContainedProductId
        .flatMapLatest { productId ->
            if (productId.isNullOrBlank()) flowOf(null) else repository.getProductByIdFlow(productId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val debouncedPetSearchQuery = _petSearchQuery
        .debounce(PET_SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    val filteredPetsWithOwners: StateFlow<List<PetWithOwner>> = combine(
        petsWithOwners,
        debouncedPetSearchQuery
    ) { pets, query ->
        if (query.isBlank()) {
            pets
        } else {
            pets.filter { it.pet.name.contains(query, true) || it.owner.name.contains(query, true) }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onPetSearchQueryChange(query: String) {
        _petSearchQuery.value = query
    }

    fun clearPetSearchQuery() {
        _petSearchQuery.value = ""
    }

    fun onClientNameChange(name: String) {
        _clientNameSuggestionQuery.value = name
    }

    fun clearClientNameSuggestions() {
        _clientNameSuggestionQuery.value = ""
    }

    fun onProductNameChange(name: String) {
        _productNameSuggestionQuery.value = name
    }

    fun clearProductNameSuggestions() {
        _productNameSuggestionQuery.value = ""
    }

    fun onContainedProductSearchChange(query: String) {
        _containedProductSearchQuery.value = query
    }

    fun onContainedProductSelected(productId: String?) {
        _selectedContainedProductId.value = productId
    }

    fun clearContainedProductSelection() {
        _containedProductSearchQuery.value = ""
        _selectedContainedProductId.value = null
    }

    fun onShowAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun onDismissAddProductDialog() {
        _showAddProductDialog.value = false
        clearProductNameSuggestions()
        clearContainedProductSelection()
    }

    fun loadTreatmentsForPet(petId: String) = viewModelScope.launch {
        repository.getTreatmentsForPet(petId).collect { _treatmentHistory.value = it }
    }

    fun addClient(name: String, phone: String, debt: Double) = executeSafely {
        repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, address = null, debtAmount = debt))
        appEventBus.reportOperationSuccess("Cliente guardado.")
        clearClientNameSuggestions()
    }

    fun addPet(pet: Pet) = executeSafely {
        repository.insertPet(pet)
        appEventBus.reportOperationSuccess("Mascota guardada.")
    }

    fun updatePet(pet: Pet) = executeSafely {
        repository.updatePet(pet)
        appEventBus.reportOperationSuccess("Mascota actualizada.")
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

    fun updateTreatment(treatment: Treatment) = executeSafely {
        repository.updateTreatment(treatment)
        appEventBus.reportOperationSuccess("Consulta actualizada.")
    }

    fun deleteTreatment(treatment: Treatment) = executeSafely {
        repository.deleteTreatment(treatment)
        appEventBus.reportOperationSuccess("Consulta eliminada.")
    }

    fun insertOrUpdateProduct(product: Product) = executeSafely {
        val isNewProduct = product.productId.isBlank()
        repository.insertOrUpdateProduct(product)
        appEventBus.reportOperationSuccess(if (isNewProduct) "Producto guardado." else "Producto actualizado.")
        onDismissAddProductDialog()
    }

    private fun executeSafely(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }
}
