package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "purchase_product_cross_ref",
    primaryKeys = ["purchaseId", "productId"],
    foreignKeys = [
        ForeignKey(entity = Purchase::class, parentColumns = ["purchaseId"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Product::class, parentColumns = ["productId"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("productId")]
)
data class PurchaseProductCrossRef(
    val purchaseId: String,
    val productId: String,
    val quantity: Double,
    val costAtTimeOfPurchase: Double
)