// ruta: app/src/main/java/com/example/vetfinance/data/Appointment.kt
package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["petId"],
            childColumns = ["petIdFk"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petIdFk"), Index("clientIdFk")]
)
data class Appointment(
    @PrimaryKey
    val appointmentId: String = UUID.randomUUID().toString(),
    val petIdFk: String,
    val clientIdFk: String,
    val appointmentDate: Long,
    val description: String,
    var isCompleted: Boolean = false
)