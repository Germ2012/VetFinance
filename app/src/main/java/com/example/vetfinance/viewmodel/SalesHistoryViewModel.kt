package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.data.AppSettings
import com.example.vetfinance.data.CashClosingSalesRow
import com.example.vetfinance.data.SaleDetailLine
import com.example.vetfinance.data.SaleListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _selectedSaleDateFilter = MutableStateFlow<Long?>(null)
    val selectedSaleDateFilter: StateFlow<Long?> = _selectedSaleDateFilter.asStateFlow()

    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    val pagingRefreshEvents: SharedFlow<Unit> = appEventBus.pagingRefreshEvents

    private val salesListDateRange: StateFlow<Pair<Long?, Long?>> = _selectedSaleDateFilter
        .map { selectedDate ->
            selectedDate?.let { startOfDay ->
                startOfDay to (startOfDay + 24 * 60 * 60 * 1000 - 1)
            } ?: defaultSalesRangeMillis()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultSalesRangeMillis())

    val salesPaginated: Flow<PagingData<SaleListItem>> = salesListDateRange
        .flatMapLatest { (startDate, endDate) -> repository.getSalesPaginated(startDate, endDate) }
        .cachedIn(viewModelScope)

    val salesListSummary: StateFlow<CashClosingSalesRow> = salesListDateRange
        .flatMapLatest { (startDate, endDate) -> repository.getSalesListSummary(startDate, endDate) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashClosingSalesRow(0, 0.0))

    fun deleteSale(sale: SaleListItem) = viewModelScope.launch {
        try {
            repository.deleteSaleById(sale.saleId)
            appEventBus.reportOperationSuccess("Venta eliminada y stock restaurado.")
            appEventBus.requestPagingRefresh()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }

    fun getSaleDetailLines(saleId: String): Flow<List<SaleDetailLine>> =
        repository.getSaleDetailLines(saleId)

    fun onSaleDateFilterSelected(date: Long?) {
        _selectedSaleDateFilter.value = date
    }

    fun clearSaleDateFilter() {
        _selectedSaleDateFilter.value = null
    }

    private fun defaultSalesRangeMillis(): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        return start to end
    }
}
