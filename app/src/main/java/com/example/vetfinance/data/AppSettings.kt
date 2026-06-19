package com.example.vetfinance.data

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val clinicName: String = "",
    val currency: String = "Gs.",
    val treatmentAlertDays: Int = 3,
    val supplierDebtAlertDays: Int = 7,
    val remindersEnabled: Boolean = true,
    val largeText: Boolean = false,
    val backupFrequencyDays: Int = 7,
    val lastBackupAt: Long? = null,
    val securityPin: String = ""
)
