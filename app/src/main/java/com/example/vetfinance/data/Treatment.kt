package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["petId"],
            childColumns = ["petIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petIdFk")]
)
data class Treatment(
    @PrimaryKey
    val treatmentId: String = UUID.randomUUID().toString(),
    val petIdFk: String,
    val description: String,
    val treatmentDate: Long = System.currentTimeMillis(),

    // Campos para el recordatorio
    val nextTreatmentDate: Long? = null,
    val isNextTreatmentCompleted: Boolean = false
)