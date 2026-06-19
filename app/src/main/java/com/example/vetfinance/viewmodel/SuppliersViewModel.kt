package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.Supplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showSupplierDialog = MutableStateFlow(false)
    val showSupplierDialog: StateFlow<Boolean> = _showSupplierDialog.asStateFlow()

    private val _editingSupplier = MutableStateFlow<Supplier?>(null)
    val editingSupplier: StateFlow<Supplier?> = _editingSupplier.asStateFlow()

    fun onShowSupplierDialog(supplier: Supplier? = null) {
        _editingSupplier.value = supplier
        _showSupplierDialog.value = true
    }

    fun onDismissSupplierDialog() {
        _editingSupplier.value = null
        _showSupplierDialog.value = false
    }

    fun addOrUpdateSupplier(supplier: Supplier) = viewModelScope.launch {
        try {
            if (_editingSupplier.value == null) {
                repository.insertSupplier(supplier)
                appEventBus.reportOperationSuccess("Proveedor guardado.")
            } else {
                repository.updateSupplier(supplier)
                appEventBus.reportOperationSuccess("Proveedor actualizado.")
            }
            onDismissSupplierDialog()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }
}
