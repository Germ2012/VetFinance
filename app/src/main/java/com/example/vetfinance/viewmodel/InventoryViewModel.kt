package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.data.InventoryCountsRow
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.ProductCostHistoryItem
import com.example.vetfinance.data.StockMovement
import com.example.vetfinance.domain.usecase.GetFilteredInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INVENTORY_SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: VetRepository,
    private val getFilteredInventoryUseCase: GetFilteredInventoryUseCase,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _showAddProductDialog = MutableStateFlow(false)
    private val _inventoryFilter = MutableStateFlow("Todos")
    private val _productSearchQuery = MutableStateFlow("")
    private val _productNameSuggestionQuery = MutableStateFlow("")
    private val _productCostHistory = MutableStateFlow<List<ProductCostHistoryItem>>(emptyList())
    private val _productStockMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    private val _appSettings = MutableStateFlow(repository.getAppSettings())

    private var costHistoryJob: Job? = null
    private var stockMovementJob: Job? = null

    val productCostHistory: StateFlow<List<ProductCostHistoryItem>> = _productCostHistory.asStateFlow()
    val productStockMovements: StateFlow<List<StockMovement>> = _productStockMovements.asStateFlow()
    val pagingRefreshEvents: SharedFlow<Unit> = appEventBus.pagingRefreshEvents

    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val suppliers = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val inventoryCounts: StateFlow<InventoryCountsRow> = repository.getInventoryCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryCountsRow())

    private val debouncedProductSearchQuery = _productSearchQuery
        .debounce(INVENTORY_SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestionQuery
        .debounce(INVENTORY_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchProductSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryPaginated: Flow<PagingData<Product>> = combine(
        _inventoryFilter,
        debouncedProductSearchQuery
    ) { filter, query ->
        filter to query
    }
        .distinctUntilChanged()
        .flatMapLatest { (filter, query) -> getFilteredInventoryUseCase(filter, query) }
        .cachedIn(viewModelScope)

    val lowStockProductsByName: StateFlow<List<Product>> = repository.getLowStockProductsByName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val inventoryBaseUiState: Flow<InventoryScreenUiState> = combine(
        _showAddProductDialog,
        _inventoryFilter,
        _productSearchQuery,
        inventoryCounts
    ) { showDialog, filter, query, counts ->
        InventoryScreenUiState(
            showAddProductDialog = showDialog,
            filter = filter,
            searchQuery = query,
            totalCount = counts.totalCount,
            productCount = counts.productCount,
            serviceCount = counts.serviceCount,
            lowStockCount = counts.lowStockCount
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

    init {
        viewModelScope.launch {
            combine(inventoryCounts, suppliers) { _, _ -> Unit }.first()
            _isLoading.value = false
        }
    }

    fun onInventoryFilterChanged(newFilter: String) {
        _inventoryFilter.value = newFilter
    }

    fun onProductSearchQueryChange(query: String) {
        _productSearchQuery.value = query
    }

    fun clearProductSearchQuery() {
        _productSearchQuery.value = ""
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

    fun insertOrUpdateProduct(product: Product) = executeWithLoading {
        val isNewProduct = product.productId.isBlank()
        repository.insertOrUpdateProduct(product)
        appEventBus.reportOperationSuccess(if (isNewProduct) "Producto guardado." else "Producto actualizado.")
        appEventBus.requestPagingRefresh()
    }

    fun deleteProduct(product: Product) = executeWithLoading {
        repository.deleteProduct(product)
        appEventBus.reportOperationSuccess("Producto eliminado.")
        appEventBus.requestPagingRefresh()
    }

    fun adjustProductStock(product: Product, newStock: Double, note: String) = executeWithLoading {
        repository.adjustProductStock(product, newStock, note)
        appEventBus.reportOperationSuccess("Stock ajustado.")
        appEventBus.requestPagingRefresh()
    }

    fun openContainerForBulkSale(containerProduct: Product) = executeWithLoading {
        if (containerProduct.containedProductId != null && containerProduct.containerSize != null) {
            repository.performInventoryTransfer(
                containerId = containerProduct.productId,
                containedId = containerProduct.containedProductId,
                amountToTransfer = containerProduct.containerSize
            )
            appEventBus.reportOperationSuccess("Contenedor abierto y stock actualizado.")
            appEventBus.requestPagingRefresh()
        }
    }

    fun loadProductCostHistory(productId: String) {
        costHistoryJob?.cancel()
        costHistoryJob = viewModelScope.launch {
            repository.getProductCostHistory(productId).collect { _productCostHistory.value = it }
        }
    }

    fun loadProductStockMovements(productId: String) {
        stockMovementJob?.cancel()
        stockMovementJob = viewModelScope.launch {
            repository.getProductStockMovements(productId).collect { _productStockMovements.value = it }
        }
    }

    private fun executeWithLoading(action: suspend () -> Unit) = viewModelScope.launch {
        _isLoading.value = true
        try {
            action()
        } catch (e: Exception) {
            appEventBus.reportOperationError(e)
        } finally {
            _isLoading.value = false
        }
    }
}
