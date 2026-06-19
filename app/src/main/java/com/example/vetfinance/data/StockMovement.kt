package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

const val STOCK_MOVEMENT_SALE = "SALE"
const val STOCK_MOVEMENT_SALE_REVERSAL = "SALE_REVERSAL"
const val STOCK_MOVEMENT_RESTOCK = "RESTOCK"
const val STOCK_MOVEMENT_CONTAINER_OPEN = "CONTAINER_OPEN"
const val STOCK_MOVEMENT_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT"

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("productIdFk")]
)
@Immutable
data class StockMovement(
    @PrimaryKey
    val movementId: String = UUID.randomUUID().toString(),
    val productIdFk: String?,
    val productNameSnapshot: String,
    val movementDate: Long,
    val movementType: String,
    val quantityChange: Double,
    val stockAfter: Double,
    val note: String? = null,
    val unitCost: Double? = null
)
