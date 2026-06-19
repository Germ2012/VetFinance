package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.Client
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CLIENT_ENTRY_SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ClientEntryViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _clientNameSuggestionQuery = MutableStateFlow("")

    val clientNameSuggestions: StateFlow<List<Client>> = _clientNameSuggestionQuery
        .debounce(CLIENT_ENTRY_SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.searchClientSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onClientNameChange(name: String) {
        _clientNameSuggestionQuery.value = name
    }

    fun clearClientNameSuggestions() {
        _clientNameSuggestionQuery.value = ""
    }

    fun addClient(name: String, phone: String, debt: Double) = viewModelScope.launch {
        try {
            repository.insertClient(Client(name = name, phone = phone.ifBlank { null }, address = null, debtAmount = debt))
            appEventBus.reportOperationSuccess("Cliente guardado.")
            clearClientNameSuggestions()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }
}
