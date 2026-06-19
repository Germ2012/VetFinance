package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppMessagesViewModel @Inject constructor(
    private val appEventBus: AppEventBus
) : ViewModel() {
    val operationErrorMessage: StateFlow<String?> = appEventBus.operationErrorMessage
    val operationSuccessMessage: StateFlow<String?> = appEventBus.operationSuccessMessage

    fun clearOperationErrorMessage() {
        appEventBus.clearOperationErrorMessage()
    }

    fun clearOperationSuccessMessage() {
        appEventBus.clearOperationSuccessMessage()
    }
}
