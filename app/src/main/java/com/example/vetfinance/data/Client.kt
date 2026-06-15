package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "clients",
    indices = [
        Index("name"),
        Index("phone")
    ]
)
data class Client(
    @PrimaryKey
    val clientId: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String?,
    val address: String?,
    val debtAmount: Double = 0.0
)
