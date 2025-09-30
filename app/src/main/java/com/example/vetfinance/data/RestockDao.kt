package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface RestockDao {
    @Insert
    suspend fun insertOrder(order: RestockOrder)

    @Insert
    suspend fun insertOrderItems(items: List<RestockOrderItem>)
}