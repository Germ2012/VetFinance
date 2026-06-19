package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.data.CartItem
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import com.example.vetfinance.data.Sale
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.domain.usecase.GetFilteredInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.floor

private const val SALE_SEARCH_DEBOUNCE_MS = 300L

data class SaleEntryUiState(
    val cart: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val showFractionalSaleDialog: Boolean = false,
    val productForFractionalSale: Product? = null,
    val showDoseSaleDialog: Boolean = false,
    val productForDoseSale: Product? = null,
    val saleTypeDialogProduct: Product? = null,
    val isFinalizing: Boolean = false
)

sealed interface SaleUiEvent {
    data class SaleFinished(val message: String) : SaleUiEvent
    data class Error(val message: String) : SaleUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SaleViewModel @Inject constructor(
    private val repository: VetRepository,
    private val getFilteredInventoryUseCase: GetFilteredInventoryUseCase,
    private val appEventBus: AppEventBus
) : ViewModel() {

    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pagingRefreshEvents: SharedFlow<Unit> = appEventBus.pagingRefreshEvents

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _saleInventoryFilter = MutableStateFlow("Todos")
    val saleInventoryFilter: StateFlow<String> = _saleInventoryFilter.asStateFlow()

    private val _productNameSuggestionQuery = MutableStateFlow("")
    private val _containedProductSearchQuery = MutableStateFlow("")
    private val _selectedContainedProductId = MutableStateFlow<String?>(null)
    val productNameSuggestions: StateFlow<List<Product>> = _productNameSuggestionQuery
        .debounce(SALE_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchProductSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val containedProductSuggestions: StateFlow<List<Product>> = _containedProductSearchQuery
        .debounce(SALE_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.searchContainedProductCandidates(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedContainedProduct: StateFlow<Product?> = _selectedContainedProductId
        .flatMapLatest { productId ->
            if (productId.isNullOrBlank()) flowOf(null) else repository.getProductByIdFlow(productId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _clientNameSuggestionQuery = MutableStateFlow("")
    val clientNameSuggestions: StateFlow<List<Client>> = _clientNameSuggestionQuery
        .debounce(SALE_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchClientSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val debouncedProductSearchQuery = _productSearchQuery
        .debounce(SALE_SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    val saleInventoryPaginated: Flow<PagingData<Product>> = combine(
        _saleInventoryFilter,
        debouncedProductSearchQuery
    ) { filter, query ->
        filter to query
    }.distinctUntilChanged().flatMapLatest { (filter, query) ->
        getFilteredInventoryUseCase(filter, query)
    }.cachedIn(viewModelScope)

    private val containerStockMap: StateFlow<Map<String, Double>> = inventory
        .map { products: List<Product> ->
            products.filter { it.isContainer && !it.containedProductId.isNullOrBlank() && (it.containerSize ?: 0.0) > 0.0 }
                .groupBy { it.containedProductId!! }
                .mapValues { entry ->
                    entry.value.sumOf { floor(it.stock) * (it.containerSize ?: 0.0) }
                }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _uiState = MutableStateFlow(SaleEntryUiState())
    val uiState: StateFlow<SaleEntryUiState> = _uiState.asStateFlow()

    val saleTypeBulkProduct: StateFlow<Product?> = uiState
        .map { state -> state.saleTypeDialogProduct?.containedProductId }
        .distinctUntilChanged()
        .flatMapLatest { productId ->
            if (productId.isNullOrBlank()) flowOf(null) else repository.getProductByIdFlow(productId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _events = MutableSharedFlow<SaleUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onShowAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun onDismissAddProductDialog() {
        _showAddProductDialog.value = false
        clearProductNameSuggestions()
        clearContainedProductSelection()
    }

    fun onProductSearchQueryChange(query: String) {
        _productSearchQuery.value = query
    }

    fun clearProductSearchQuery() {
        _productSearchQuery.value = ""
    }

    fun onSaleInventoryFilterSelected(newFilter: String) {
        _saleInventoryFilter.value = newFilter
    }

    fun clearSaleInventoryFilter() {
        _saleInventoryFilter.value = "Todos"
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

    fun onClientNameChange(name: String) {
        _clientNameSuggestionQuery.value = name
    }

    fun clearClientNameSuggestions() {
        _clientNameSuggestionQuery.value = ""
    }

    fun insertOrUpdateProduct(product: Product) = viewModelScope.launch {
        try {
            val isNewProduct = product.productId.isBlank()
            repository.insertOrUpdateProduct(product)
            appEventBus.reportOperationSuccess(if (isNewProduct) "Producto guardado." else "Producto actualizado.")
            appEventBus.requestPagingRefresh()
            onDismissAddProductDialog()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }

    fun openFractionalSaleDialog(product: Product) {
        _uiState.update {
            it.copy(
                productForFractionalSale = product,
                showFractionalSaleDialog = true
            )
        }
    }

    fun dismissFractionalSaleDialog() {
        _uiState.update {
            it.copy(
                productForFractionalSale = null,
                showFractionalSaleDialog = false
            )
        }
    }

    fun openDoseSaleDialog(product: Product) {
        _uiState.update {
            it.copy(
                productForDoseSale = product,
                showDoseSaleDialog = true
            )
        }
    }

    fun dismissDoseSaleDialog() {
        _uiState.update {
            it.copy(
                productForDoseSale = null,
                showDoseSaleDialog = false
            )
        }
    }

    fun openSaleTypeDialog(product: Product) {
        _uiState.update { it.copy(saleTypeDialogProduct = product) }
    }

    fun closeSaleTypeDialog() {
        _uiState.update { it.copy(saleTypeDialogProduct = null) }
    }

    fun addToCart(product: Product) {
        if (product.sellingMethod != SELLING_METHOD_BY_UNIT) return

        val currentCart = _uiState.value.cart.toMutableList()
        val existingItem = currentCart.find { it.product.productId == product.productId }
        val availableStock = availableStockForCart(product)

        if (shouldValidateStockInCart(product) && availableStock < 1.0) {
            reportError("Stock insuficiente para ${product.name}. Disponible: ${availableStock.formatForMessage()}.")
            return
        }

        if (existingItem != null) {
            val newQuantity = existingItem.quantity + 1.0
            if (!shouldValidateStockInCart(product) || newQuantity <= availableStock) {
                val index = currentCart.indexOf(existingItem)
                currentCart[index] = existingItem.copy(quantity = newQuantity)
            } else {
                reportError("Stock insuficiente para ${product.name}. Disponible: ${availableStock.formatForMessage()}, solicitado: ${newQuantity.formatForMessage()}.")
            }
        } else {
            currentCart.add(CartItem(product = product, quantity = 1.0))
        }

        setCart(currentCart)
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingItem = currentCart.find { it.cartItemId == cartItem.cartItemId } ?: return

        if (existingItem.product.sellingMethod == SELLING_METHOD_BY_UNIT && existingItem.quantity > 1) {
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = existingItem.quantity - 1)
        } else {
            currentCart.remove(existingItem)
        }

        setCart(currentCart)
    }

    fun addOrUpdateProductInCart(product: Product, quantity: Double) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingItemIndex = currentCart.indexOfFirst { it.product.productId == product.productId }

        if (quantity > 0) {
            val availableStock = availableStockForCart(product)
            val validQuantity = if (shouldValidateStockInCart(product)) {
                if (availableStock <= 0.0) {
                    reportError("Stock insuficiente para ${product.name}.")
                    0.0
                } else {
                    if (quantity > availableStock) {
                        reportError("Stock insuficiente para ${product.name}. Se agrego solo ${availableStock.formatForMessage()}.")
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
        } else if (existingItemIndex != -1) {
            currentCart.removeAt(existingItemIndex)
        }

        setCart(currentCart)
    }

    fun addOrUpdateDoseInCart(product: Product, notes: String, price: Double) {
        val currentCart = _uiState.value.cart.toMutableList()
        currentCart.add(
            CartItem(
                product = product,
                quantity = 1.0,
                notes = notes.ifBlank { null },
                overridePrice = price
            )
        )
        setCart(currentCart)
        dismissDoseSaleDialog()
    }

    fun updateCartItemPrice(cartItem: CartItem, finalPrice: Double?, reason: String?) {
        val currentCart = _uiState.value.cart.toMutableList()
        val index = currentCart.indexOfFirst { it.cartItemId == cartItem.cartItemId }
        if (index == -1) return

        currentCart[index] = currentCart[index].copy(
            overridePrice = finalPrice?.takeIf { it > 0.0 },
            notes = reason?.ifBlank { null }
        )
        setCart(currentCart)
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList(), total = 0.0) }
    }

    fun finalizeSale(clientName: String? = null, selectedClientId: String? = null) {
        val currentState = _uiState.value
        if (currentState.cart.isEmpty() || currentState.isFinalizing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFinalizing = true) }
            try {
                val saleClient = selectedClientId
                    ?.let { repository.getClientById(it) }
                    ?: repository.findOrCreateClientForSale(clientName)

                repository.insertSale(
                    Sale(
                        date = System.currentTimeMillis(),
                        totalAmount = currentState.total,
                        clientIdFk = saleClient.clientId
                    ),
                    currentState.cart
                )
                clearCart()
                _events.emit(SaleUiEvent.SaleFinished("Venta registrada correctamente."))
            } catch (e: Exception) {
                _events.emit(SaleUiEvent.Error(e.message ?: "No se pudo registrar la venta."))
            } finally {
                _uiState.update { it.copy(isFinalizing = false) }
            }
        }
    }

    private fun setCart(cart: List<CartItem>) {
        _uiState.update { it.copy(cart = cart, total = cart.sumOf(::cartItemTotal)) }
    }

    private fun cartItemTotal(item: CartItem): Double {
        return item.overridePrice ?: (item.product.price * item.quantity)
    }

    private fun shouldValidateStockInCart(product: Product): Boolean {
        return !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY
    }

    private fun availableStockForCart(product: Product): Double {
        if (!shouldValidateStockInCart(product)) return Double.MAX_VALUE
        if (product.isContainer) return product.stock

        val stockFromClosedContainers = containerStockMap.value[product.productId] ?: 0.0

        return product.stock + stockFromClosedContainers
    }

    private fun reportError(message: String) {
        _events.tryEmit(SaleUiEvent.Error(message))
    }

    private fun Double.formatForMessage(): String {
        return if (this % 1.0 == 0.0) {
            this.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.3f", this)
        }
    }
}
