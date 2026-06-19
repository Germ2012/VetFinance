package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    @Upsert
    suspend fun upsertAll(payments: List<Payment>)

    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClientPagedSource(clientId: String): PagingSource<Int, Payment>

    @Query("""
        SELECT
            COUNT(*) AS paymentCount,
            COALESCE(SUM(amount), 0.0) AS totalPaid,
            MAX(paymentDate) AS lastPaymentDate
        FROM payments
        WHERE clientIdFk = :clientId
    """)
    fun getPaymentSummaryForClient(clientId: String): Flow<ClientPaymentSummaryRow>

    @Query("SELECT * FROM payments")
    fun getAllPaymentsSimple(): Flow<List<Payment>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM payments
        WHERE paymentDate BETWEEN :startDate AND :endDate
    """)
    fun getPaymentsTotalForRange(startDate: Long, endDate: Long): Flow<Double>
}
