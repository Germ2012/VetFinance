package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class PetWithOwner(
    @Embedded val pet: Pet,
    @Relation(
        parentColumn = "ownerIdFk",
        entityColumn = "clientId"
    )
    val owner: Client
)
