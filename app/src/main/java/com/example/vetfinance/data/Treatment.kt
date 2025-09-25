package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa un tratamiento médico o procedimiento realizado a una mascota.
 * También incluye campos para programar y dar seguimiento a tratamientos futuros.
 *
 * @property treatmentId El identificador único para el tratamiento, generado automáticamente.
 * @property petIdFk La clave foránea que vincula el tratamiento con la [Pet] que lo recibió.
 * @property description Una descripción de lo que se realizó en el tratamiento.
 * @property treatmentDate La fecha y hora en que se realizó el tratamiento, almacenada como milisegundos.
 * @property nextTreatmentDate La fecha y hora para el próximo tratamiento programado (opcional). Usado para recordatorios.
 * @property isNextTreatmentCompleted Un booleano que indica si el tratamiento programado ya fue completado.
 */
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
    val nextTreatmentDate: Long? = null,
    val isNextTreatmentCompleted: Boolean = false
)