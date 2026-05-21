package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class RestockHistoryItem(
    val productName: String,
    val supplierName: String?,
    val orderDate: Long,
    val costAtTime: Double,
    val quantity: Double
)

data class ProductCostHistoryItem(
    val supplierName: String?,
    val orderDate: Long,
    val costAtTime: Double,
    val quantity: Double
)

@Dao
interface RestockDao {
    @Insert
    suspend fun insertOrder(order: RestockOrder)

    @Insert
    suspend fun insertOrderItems(items: List<RestockOrderItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<RestockOrder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrderItems(items: List<RestockOrderItem>)

    @Transaction
    @Query("""
        SELECT
            p.name as productName,
            s.name as supplierName,
            ro.orderDate as orderDate,
            roi.costPerUnit as costAtTime, 
            roi.quantity as quantity
        FROM restock_order_items AS roi
        INNER JOIN restock_orders AS ro ON roi.orderIdFk = ro.orderId 
        INNER JOIN products AS p ON roi.productIdFk = p.productId 
        LEFT JOIN suppliers AS s ON ro.supplierIdFk = s.supplierId
        WHERE ro.orderDate >= :startDate AND ro.orderDate <= :endDate 
        ORDER BY ro.orderDate DESC
    """)
    suspend fun getRestockHistoryForDateRange(startDate: Long, endDate: Long): List<RestockHistoryItem>

    @Query("""
        SELECT
            s.name as supplierName,
            ro.orderDate as orderDate,
            roi.costPerUnit as costAtTime,
            roi.quantity as quantity
        FROM restock_order_items AS roi
        INNER JOIN restock_orders AS ro ON roi.orderIdFk = ro.orderId
        LEFT JOIN suppliers AS s ON ro.supplierIdFk = s.supplierId
        WHERE roi.productIdFk = :productId
        ORDER BY ro.orderDate DESC
    """)
    fun getProductCostHistory(productId: String): Flow<List<ProductCostHistoryItem>>

    @Query("SELECT * FROM restock_orders")
    fun getAllRestockOrdersSimple(): Flow<List<RestockOrder>>

    @Query("SELECT * FROM restock_order_items")
    fun getAllRestockOrderItemsSimple(): Flow<List<RestockOrderItem>>
}
