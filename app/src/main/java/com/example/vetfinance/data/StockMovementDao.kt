package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>)

    @Upsert
    suspend fun upsertAll(movements: List<StockMovement>)

    @Query("SELECT * FROM stock_movements WHERE productIdFk = :productId ORDER BY movementDate DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY movementDate DESC")
    fun getAllStockMovementsSimple(): Flow<List<StockMovement>>
}
