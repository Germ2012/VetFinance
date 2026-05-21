package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["crossRefId"],
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["saleId"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("saleId")]
)
data class SaleProductCrossRef(
    val saleId: String,
    val productId: String,
    val quantitySold: Double,
    val priceAtTimeOfSale: Double,
    val notes: String? = null,
    val overridePrice: Double? = null,
    val crossRefId: String = UUID.randomUUID().toString()
)
