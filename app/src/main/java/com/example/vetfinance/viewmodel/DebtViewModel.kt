package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.ClientDebtHistory
import com.example.vetfinance.data.ClientDebtHistorySummaryRow
import com.example.vetfinance.data.ClientPaymentSummaryRow
import com.example.vetfinance.data.DebtCollectionRow
import com.example.vetfinance.data.DebtCollectionSummary
import com.example.vetfinance.data.Payment
import com.example.vetfinance.domain.usecase.GetDebtCollectionPageUseCase
import com.example.vetfinance.domain.usecase.GetDebtCollectionSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val DEBT_SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DebtViewModel @Inject constructor(
    private val repository: VetRepository,
    private val getDebtCollectionPageUseCase: GetDebtCollectionPageUseCase,
    private val getDebtCollectionSummaryUseCase: GetDebtCollectionSummaryUseCase,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _clientSearchQuery = MutableStateFlow("")
    private val _debtCollectionFilters = MutableStateFlow(DebtCollectionFilters())
    private val _showPaymentDialog = MutableStateFlow(false)
    private val _clientForPayment = MutableStateFlow<Client?>(null)
    private val _appSettings = MutableStateFlow(repository.getAppSettings())

    val pagingRefreshEvents: SharedFlow<Unit> = appEventBus.pagingRefreshEvents

    private val debouncedClientSearchQuery = _clientSearchQuery
        .debounce(DEBT_SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()

    private val debtCollectionParams = combine(
        debouncedClientSearchQuery,
        _debtCollectionFilters
    ) { query, filters ->
        query to filters
    }.distinctUntilChanged()

    val debtCollectionRowsPaginated: Flow<PagingData<DebtCollectionRow>> = debtCollectionParams
        .flatMapLatest { (query, filters) ->
            getDebtCollectionPageUseCase(
                searchQuery = query,
                includeZeroDebt = !filters.showOnlyWithDebt,
                minimumDebt = filters.minimumDebt,
                sortMode = filters.sortMode
            )
        }
        .cachedIn(viewModelScope)

    private val collectionSummarySource = debtCollectionParams
        .flatMapLatest { (query, filters) ->
            getDebtCollectionSummaryUseCase(
                searchQuery = query,
                includeZeroDebt = !filters.showOnlyWithDebt,
                minimumDebt = filters.minimumDebt
            )
        }

    private val debtCollectionSummary: StateFlow<DebtCollectionSummary> = collectionSummarySource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtCollectionSummary(0, 0.0, 0.0))

    private val baseUiState: Flow<DebtClientsScreenUiState> = combine(
        _clientSearchQuery,
        _showPaymentDialog,
        _clientForPayment,
        _isLoading,
        _appSettings
    ) { query, showPayment, paymentClient, loading, settings ->
        DebtClientsScreenUiState(
            searchQuery = query,
            showPaymentDialog = showPayment,
            clientForPayment = paymentClient,
            isLoading = loading,
            appSettings = settings
        )
    }

    val uiState: StateFlow<DebtClientsScreenUiState> = combine(
        baseUiState,
        debtCollectionSummary
    ) { base, summary ->
        base.copy(collectionSummary = summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtClientsScreenUiState())

    init {
        viewModelScope.launch {
            collectionSummarySource.first()
            _isLoading.value = false
        }
    }

    fun refreshAppSettings() {
        _appSettings.value = repository.getAppSettings()
    }

    fun onClientSearchQueryChange(query: String) {
        _clientSearchQuery.value = query
    }

    fun clearClientSearchQuery() {
        _clientSearchQuery.value = ""
    }

    fun onDebtCollectionFiltersChanged(
        showOnlyWithDebt: Boolean,
        minimumDebt: Double,
        sortMode: String
    ) {
        _debtCollectionFilters.value = DebtCollectionFilters(
            showOnlyWithDebt = showOnlyWithDebt,
            minimumDebt = minimumDebt.coerceAtLeast(0.0),
            sortMode = sortMode
        )
    }

    fun onShowPaymentDialog(client: Client) {
        _clientForPayment.value = client
        _showPaymentDialog.value = true
    }

    fun onDismissPaymentDialog() {
        _clientForPayment.value = null
        _showPaymentDialog.value = false
    }

    fun deleteClient(client: Client) = executeSafely {
        repository.deleteClient(client)
        appEventBus.reportOperationSuccess("Cliente eliminado.")
        appEventBus.requestPagingRefresh()
    }

    fun makePayment(amount: Double) = executeSafely {
        val client = _clientForPayment.value ?: return@executeSafely
        val paid = kotlin.math.min(amount, client.debtAmount)
        val remainingDebt = (client.debtAmount - paid).coerceAtLeast(0.0)
        repository.makePayment(client, amount)
        appEventBus.reportOperationSuccess("Pago registrado. Saldo pendiente: Gs. ${remainingDebt.formatMoneyForMessage()}.")
        onDismissPaymentDialog()
        appEventBus.requestPagingRefresh()
    }

    fun adjustClientDebt(client: Client, newDebt: Double, note: String?) = executeSafely {
        repository.adjustClientDebt(client, newDebt, note)
        appEventBus.reportOperationSuccess("Deuda ajustada. Nuevo saldo: Gs. ${newDebt.formatMoneyForMessage()}.")
        appEventBus.requestPagingRefresh()
    }

    fun clientById(clientId: String): Flow<Client?> =
        repository.getClientByIdFlow(clientId)

    fun paymentsForClientPaginated(clientId: String): Flow<PagingData<Payment>> =
        repository.getPaymentsForClientPaginated(clientId).cachedIn(viewModelScope)

    fun debtHistoryForClientPaginated(clientId: String): Flow<PagingData<ClientDebtHistory>> =
        repository.getDebtHistoryForClientPaginated(clientId).cachedIn(viewModelScope)

    fun paymentSummaryForClient(clientId: String): Flow<ClientPaymentSummaryRow> =
        repository.getPaymentSummaryForClient(clientId)

    fun debtHistorySummaryForClient(clientId: String): Flow<ClientDebtHistorySummaryRow> =
        repository.getDebtHistorySummaryForClient(clientId)

    private fun executeSafely(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }

    private fun Double.formatMoneyForMessage(): String {
        return String.format(Locale.getDefault(), "%,.0f", this)
    }
}
