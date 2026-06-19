package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.RestockHistoryItem
import com.example.vetfinance.data.RestockOrder
import com.example.vetfinance.data.RestockOrderItem
import com.example.vetfinance.data.Supplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RestockViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _restockSearchQuery = MutableStateFlow("")
    val restockSearchQuery: StateFlow<String> = _restockSearchQuery.asStateFlow()

    private val _restockHistory = MutableStateFlow<List<RestockHistoryItem>>(emptyList())
    val restockHistory: StateFlow<List<RestockHistoryItem>> = _restockHistory.asStateFlow()

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.getLowStockProductsByName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onRestockSearchQueryChange(query: String) {
        _restockSearchQuery.value = query
    }

    fun fetchRestockHistory(date: LocalDate) {
        viewModelScope.launch {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            _restockHistory.value = repository.getRestockHistoryForDateRange(startOfDay, endOfDay)
        }
    }

    fun executeRestock(
        supplierId: String,
        totalCost: Double,
        itemsToRestock: List<RestockOrderItem>,
        orderDate: Long,
        supplierDebtDueDate: Long? = null
    ) = viewModelScope.launch {
        try {
            val orderId = UUID.randomUUID().toString()
            val order = RestockOrder(orderId = orderId, supplierIdFk = supplierId, orderDate = orderDate, totalAmount = totalCost)
            val updatedItems = itemsToRestock.map { it.copy(orderIdFk = orderId) }
            repository.performRestock(order, updatedItems, supplierDebtDueDate)
            appEventBus.reportOperationSuccess("Reabastecimiento registrado.")
            appEventBus.requestPagingRefresh()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }
}
