package com.example.vetfinance.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.vetfinance.R
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"], // Corregido de "saleId" a "id" para que coincida con Sale.kt
            childColumns = ["saleIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["saleIdFk"])]
)
data class Transaction(
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val saleIdFk: Long?, // Corregido a Long? para coincidir con el id de Sale
    val date: Long,
    val type: String,
    val amount: Double,
    val description: String?
)

enum class TransactionType(@StringRes val displayResId: Int) {
    INCOME(R.string.transaction_type_income),
    EXPENSE(R.string.transaction_type_expense)
}