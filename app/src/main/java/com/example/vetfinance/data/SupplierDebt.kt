package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "supplier_debts",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["supplierId"],
            childColumns = ["supplierIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("supplierIdFk")]
)
@Immutable
data class SupplierDebt(
    @PrimaryKey val debtId: String = UUID.randomUUID().toString(),
    val supplierIdFk: String?,
    val description: String,
    val amount: Double,
    val dueDate: Long,
    val createdAt: Long,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val note: String? = null
)

@Immutable
data class SupplierDebtWithSupplier(
    val debtId: String,
    val supplierIdFk: String?,
    val description: String,
    val amount: Double,
    val dueDate: Long,
    val createdAt: Long,
    val isPaid: Boolean,
    val paidAt: Long?,
    val note: String?,
    val supplierName: String?
)
