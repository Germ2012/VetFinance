package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = Client::class, parentColumns = ["clientId"], childColumns = ["clientIdFk"])
    ]
)
data class Payment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val amount: Double,
    val paymentDate: Date
)