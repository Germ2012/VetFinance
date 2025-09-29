package com.example.vetfinance.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.vetfinance.R
import java.util.UUID

const val TRANSACTION_TYPE_INCOME = "Ingreso"
const val TRANSACTION_TYPE_EXPENSE = "Egreso"

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
data class Transaction(
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val saleIdFk: String?,
    val date: Long,
    val type: String,
    val amount: Double,
    val description: String?
)

enum class TransactionType(@StringRes val displayResId: Int) {
    INCOME(R.string.transaction_type_income),
    EXPENSE(R.string.transaction_type_expense)
}