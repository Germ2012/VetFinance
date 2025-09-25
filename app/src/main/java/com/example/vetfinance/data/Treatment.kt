package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa una entrada clínica o tratamiento para una mascota.
 *
 * @property treatmentId El identificador único para el tratamiento, generado automáticamente.
 * @property petIdFk La clave foránea que vincula el tratamiento con la [Pet] que lo recibió.
 * @property description Una descripción general de lo que se realizó en el tratamiento.
 * @property treatmentDate La fecha y hora en que se realizó el tratamiento, almacenada como milisegundos.
 * @property weight El peso de la mascota en el momento de la consulta (opcional).
 * @property temperature La temperatura de la mascota en el momento de la consulta (opcional).
 * @property symptoms Descripción de los síntomas observados (opcional).
 * @property diagnosis El diagnóstico realizado por el veterinario (opcional).
 * @property treatmentPlan El plan de tratamiento recomendado (opcional).
 * @property nextTreatmentDate La fecha para el próximo tratamiento programado (opcional).
 * @property isNextTreatmentCompleted Indica si el tratamiento programado ya fue completado.
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

    // Nuevos campos clínicos (opcionales)
    val weight: Double? = null,
    val temperature: Double? = null,
    val symptoms: String? = null,
    val diagnosis: String? = null,
    val treatmentPlan: String? = null,

    // Campos para el recordatorio
    val nextTreatmentDate: Long? = null,
    val isNextTreatmentCompleted: Boolean = false
)