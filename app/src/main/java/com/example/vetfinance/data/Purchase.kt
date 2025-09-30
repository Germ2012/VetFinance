package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchases",
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
data class Purchase(
    @PrimaryKey
    val purchaseId: String = UUID.randomUUID().toString(),
    val supplierIdFk: String?,
    val purchaseDate: Long,
    val totalAmount: Double,
    val isCredit: Boolean,
    val dueDate: Long?,
    var isPaid: Boolean = false
)