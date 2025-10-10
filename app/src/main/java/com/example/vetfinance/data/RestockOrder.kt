package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "restock_orders")
data class RestockOrder(
    @PrimaryKey val orderId: String = UUID.randomUUID().toString(),
    val supplierIdFk: String,
    var orderDate: Long,
    val totalAmount: Double
)
