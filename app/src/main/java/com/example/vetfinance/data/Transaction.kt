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
    val type: String, // Este campo almacenará el nombre del enum (INCOME, EXPENSE)
    val amount: Double,
    val description: String?
)

enum class TransactionType(@StringRes val displayResId: Int) {
    INCOME(R.string.transaction_type_income), // Ingreso
    EXPENSE(R.string.transaction_type_expense) // Egreso
}