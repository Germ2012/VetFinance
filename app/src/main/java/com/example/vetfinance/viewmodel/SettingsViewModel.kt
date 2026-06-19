package com.example.vetfinance.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vetfinance.data.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: VetRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val _appSettings = MutableStateFlow(repository.getAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    fun updateAppSettings(settings: AppSettings) {
        val normalizedSettings = settings.copy(securityPin = settings.securityPin.filter { it.isDigit() })
        repository.saveAppSettings(normalizedSettings)
        _appSettings.value = normalizedSettings
        appEventBus.reportOperationSuccess("Ajustes guardados.")
    }
}
