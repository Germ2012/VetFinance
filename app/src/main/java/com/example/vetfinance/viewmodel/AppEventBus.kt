package com.example.vetfinance.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEventBus @Inject constructor() {
    private val _operationErrorMessage = MutableStateFlow<String?>(null)
    val operationErrorMessage: StateFlow<String?> = _operationErrorMessage.asStateFlow()

    private val _operationSuccessMessage = MutableStateFlow<String?>(null)
    val operationSuccessMessage: StateFlow<String?> = _operationSuccessMessage.asStateFlow()

    private val _pagingRefreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pagingRefreshEvents: SharedFlow<Unit> = _pagingRefreshEvents.asSharedFlow()

    fun clearOperationErrorMessage() {
        _operationErrorMessage.value = null
    }

    fun clearOperationSuccessMessage() {
        _operationSuccessMessage.value = null
    }

    fun reportOperationError(error: Throwable) {
        _operationErrorMessage.value = error.message ?: "Ocurrio un error inesperado."
    }

    fun reportOperationError(message: String) {
        _operationErrorMessage.value = message
    }

    fun reportOperationSuccess(message: String) {
        _operationSuccessMessage.value = null
        _operationSuccessMessage.value = message
    }

    fun requestPagingRefresh() {
        _pagingRefreshEvents.tryEmit(Unit)
    }

    suspend fun emitPagingRefresh() {
        _pagingRefreshEvents.emit(Unit)
    }
}
