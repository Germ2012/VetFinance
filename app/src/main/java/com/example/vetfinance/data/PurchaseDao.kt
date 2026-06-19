package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseProductCrossRef(crossRef: PurchaseProductCrossRef)

    @Query("SELECT * FROM purchases WHERE isCredit = 1 AND isPaid = 0 AND dueDate IS NOT NULL AND dueDate <= :dateLimit ORDER BY dueDate ASC")
    fun getUnpaidPurchasesWithUpcomingDueDate(dateLimit: Long): Flow<List<Purchase>>

    @Query("UPDATE purchases SET isPaid = 1 WHERE purchaseId = :purchaseId")
    suspend fun markPurchaseAsPaid(purchaseId: String)
}
