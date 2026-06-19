package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vetfinance.data.Appointment
import com.example.vetfinance.data.AppointmentWithDetails
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.PetWithOwner
import com.example.vetfinance.data.Supplier
import com.example.vetfinance.data.SupplierDebt
import com.example.vetfinance.data.SupplierDebtWithSupplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    private val _showAddAppointmentDialog = MutableStateFlow(false)
    val showAddAppointmentDialog: StateFlow<Boolean> = _showAddAppointmentDialog.asStateFlow()

    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val petsWithOwners: StateFlow<List<PetWithOwner>> = repository.getAllPetsWithOwners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointmentsOnSelectedDate: StateFlow<List<AppointmentWithDetails>> = _selectedCalendarDate
        .flatMapLatest { date -> repository.getAppointmentsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierDebtsOnSelectedDate: StateFlow<List<SupplierDebtWithSupplier>> = _selectedCalendarDate
        .flatMapLatest { date -> repository.getSupplierDebtsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onCalendarDateSelected(date: LocalDate) {
        _selectedCalendarDate.value = date
    }

    fun onShowAddAppointmentDialog() {
        _showAddAppointmentDialog.value = true
    }

    fun onDismissAddAppointmentDialog() {
        _showAddAppointmentDialog.value = false
    }

    fun addAppointment(appointment: Appointment) = executeSafely {
        repository.insertAppointment(appointment)
        appEventBus.reportOperationSuccess("Cita agendada.")
    }

    fun updateAppointmentStatus(appointment: Appointment, status: String) = executeSafely {
        repository.updateAppointment(appointment.copy(status = status))
        appEventBus.reportOperationSuccess("Estado de cita actualizado.")
    }

    fun addSupplierDebt(
        supplierId: String?,
        description: String,
        amount: Double,
        dueDate: Long,
        note: String? = null
    ) = executeSafely {
        repository.insertSupplierDebt(
            SupplierDebt(
                supplierIdFk = supplierId,
                description = description,
                amount = amount,
                dueDate = dueDate,
                createdAt = System.currentTimeMillis(),
                isPaid = false,
                note = note?.ifBlank { null }
            )
        )
        appEventBus.reportOperationSuccess("Deuda de proveedor registrada.")
    }

    fun markSupplierDebtAsPaid(debtId: String) = executeSafely {
        repository.markSupplierDebtAsPaid(debtId)
        appEventBus.reportOperationSuccess("Deuda de proveedor marcada como pagada.")
    }

    private fun executeSafely(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
        } catch (error: Exception) {
            appEventBus.reportOperationError(error)
        }
    }
}
