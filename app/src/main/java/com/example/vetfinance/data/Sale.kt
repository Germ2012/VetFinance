package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("clientIdFk")]
)
data class Sale(
    @PrimaryKey
    val saleId: String = UUID.randomUUID().toString(),
    val date: Long,
    val totalAmount: Double,
    val clientIdFk: String?
)
