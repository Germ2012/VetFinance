package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["petId"], childColumns = ["petIdFk"]),
        ForeignKey(entity = Product::class, parentColumns = ["productId"], childColumns = ["serviceId"])
    ]
)
data class Treatment(
    @PrimaryKey
    val treatmentId: String = UUID.randomUUID().toString(),
    val petIdFk: String,
    val serviceId: String?,
    val treatmentDate: Date,
    val description: String?,
    val nextTreatmentDate: Date?,
    val isNextTreatmentCompleted: Boolean = false,
    val symptoms: String?,
    val diagnosis: String?,
    val treatmentPlan: String?, // Renamed from 'plan'
    val weight: Double?,
    val temperature: String?
)