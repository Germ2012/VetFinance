package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["petId"], childColumns = ["petIdFk"]),
        ForeignKey(entity = Product::class, parentColumns = ["productId"], childColumns = ["serviceId"])
    ],
    // AÑADIDO: Índices para las llaves foráneas
    indices = [Index("petIdFk"), Index("serviceId")]
)
data class Treatment(
    @PrimaryKey
    val treatmentId: String = UUID.randomUUID().toString(),
    val petIdFk: String,
    val serviceId: String?,
    val treatmentDate: Long,
    val description: String?,
    val nextTreatmentDate: Long?,
    val isNextTreatmentCompleted: Boolean = false,
    val symptoms: String?,
    val diagnosis: String?,
    val treatmentPlan: String?,
    val weight: Double?,
    val temperature: String?
)