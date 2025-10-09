package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

data class RestockHistoryItem(
    val productName: String,
    val costAtTime: Double,
    val quantity: Double // Corregido: de Int a Double
)

@Dao
interface RestockDao {
    @Insert
    suspend fun insertOrder(order: RestockOrder)

    @Insert
    suspend fun insertOrderItems(items: List<RestockOrderItem>)

    @Transaction
    @Query("""
        SELECT
            p.name as productName,
            roi.costPerUnit as costAtTime, -- Corregido: costAtTime -> costPerUnit
            roi.quantity as quantity
        FROM restock_order_items AS roi
        INNER JOIN restock_orders AS ro ON roi.orderIdFk = ro.orderId -- Corregido: JOIN en orderIdFk
        INNER JOIN products AS p ON roi.productIdFk = p.productId -- Corregido: JOIN en productIdFk
        WHERE ro.orderDate >= :startDate AND ro.orderDate <= :endDate -- Corregido: date -> orderDate
    """)
    suspend fun getRestockHistoryForDateRange(startDate: Long, endDate: Long): List<RestockHistoryItem>
}