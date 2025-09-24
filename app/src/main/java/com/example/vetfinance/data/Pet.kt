package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Index
import java.util.UUID
@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["ownerIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerIdFk")]
)
data class Pet(
    @PrimaryKey
    val petId: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerIdFk: String, // Clave foránea para el dueño (Cliente)
    val birthDate: Long? = null,
    val breed: String? = null,
    val allergies: String? = null
)

// Clase auxiliar para obtener una mascota junto con su dueño
data class PetWithOwner(
    @Embedded val pet: Pet,
    @Relation(
        parentColumn = "ownerIdFk",
        entityColumn = "clientId"
    )
    val owner: Client
)