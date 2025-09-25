package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

/**
 * Representa a una mascota en la base de datos.
 *
 * @property petId El identificador único para la mascota, generado automáticamente.
 * @property name El nombre de la mascota.
 * @property ownerIdFk La clave foránea que vincula a la mascota con su dueño (un [Client]).
 * @property birthDate La fecha de nacimiento de la mascota, almacenada como milisegundos (opcional).
 * @property breed La raza de la mascota (opcional).
 * @property allergies Información sobre las alergias de la mascota (opcional).
 */
@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["ownerIdFk"],
            onDelete = ForeignKey.CASCADE // Si se borra un cliente, se borran sus mascotas.
        )
    ],
    indices = [Index("ownerIdFk")]
)
data class Pet(
    @PrimaryKey
    val petId: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerIdFk: String,
    val birthDate: Long? = null,
    val breed: String? = null,
    val allergies: String? = null
)

/**
 * Clase de relación que une una [Pet] con su [Client] (dueño).
 * Room utiliza esta clase para cargar fácilmente una mascota y su dueño en una sola consulta.
 *
 * @property pet La entidad de la mascota.
 * @property owner La entidad del dueño asociado.
 */
data class PetWithOwner(
    @Embedded val pet: Pet,
    @Relation(
        parentColumn = "ownerIdFk",
        entityColumn = "clientId"
    )
    val owner: Client
)