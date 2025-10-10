package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"], // Es más común usar una clave primaria compuesta aquí
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["saleId"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE // <-- MODIFICACIÓN AÑADIDA
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE // <-- MODIFICACIÓN AÑADIDA
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
    val overridePrice: Double? = null
)
