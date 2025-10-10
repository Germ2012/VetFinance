package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "appointment_logs")
data class AppointmentLog(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val originalAppointmentDate: Long,
    val clientName: String,
    val petName: String,
    val cancellationReason: String,
    val cancelledOnDate: Long = System.currentTimeMillis()
)
