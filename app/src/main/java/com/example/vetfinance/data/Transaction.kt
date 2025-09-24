package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["saleId"],
            childColumns = ["saleIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["saleIdFk"])]
)
data class Transaction( // <-- CLASE RENOMBRADA
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val saleIdFk: String?, // <-- HECHO NULABLE
    val date: Long,
    val type: String,
    val amount: Double,
    val description: String?
)

enum class TransactionType {
    INCOME, // Ingreso
    EXPENSE // Egreso
}