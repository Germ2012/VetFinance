package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(entity = Client::class, parentColumns = ["clientId"], childColumns = ["clientIdFk"]),
        ForeignKey(entity = Pet::class, parentColumns = ["petId"], childColumns = ["petIdFk"])
    ],
    // AÑADIDO: Índices para las llaves foráneas
    indices = [Index("clientIdFk"), Index("petIdFk")]
)
data class Appointment(
    @PrimaryKey
    val appointmentId: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val petIdFk: String,
    val appointmentDate: Long,
    val description: String?
)