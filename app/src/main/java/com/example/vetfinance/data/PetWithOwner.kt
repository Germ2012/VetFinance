package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Relation

data class PetWithOwner(
    @Embedded val pet: Pet,
    @Relation(
        parentColumn = "ownerIdFk",
        entityColumn = "clientId"
    )
    val owner: Client
)
