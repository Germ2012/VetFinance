package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val clientId: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val debtAmount: Double = 0.0,
    )