package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["ownerIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerIdFk")]
)
data class Pet(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerIdFk: String,
    val birthDate: Long? = null,
    val breed: String? = null,
    val allergies: String? = null
)

data class PetWithOwner(
    @Embedded val pet: Pet,
    @Relation(
        parentColumn = "ownerIdFk",
        entityColumn = "clientId"
    )
    val owner: Client
)