package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val ownerIdFk: String,
    val birthDate: Long? = null,
    val breed: String? = null,
    val allergies: String? = null
)
