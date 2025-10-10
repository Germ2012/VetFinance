package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "restock_order_items")
data class RestockOrderItem(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val orderIdFk: String,
    val productIdFk: String,
    val quantity: Double,
    val costPerUnit: Double
)
