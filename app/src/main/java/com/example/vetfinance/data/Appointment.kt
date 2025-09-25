package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa una cita agendada en la base de datos.
 *
 * @property appointmentId El identificador único para la cita, generado automáticamente.
 * @property petIdFk La clave foránea que vincula la cita con la [Pet] correspondiente.
 * @property clientIdFk La clave foránea que vincula la cita con el [Client] (dueño) correspondiente.
 * @property appointmentDate La fecha y hora para la cual está programada la cita, almacenada como milisegundos.
 * @property description El motivo o la descripción de la cita.
 * @property isCompleted Un booleano que indica si la cita ya se ha completado.
 */
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