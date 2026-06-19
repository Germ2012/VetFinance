package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDebtHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ClientDebtHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(history: List<ClientDebtHistory>)

    @Upsert
    suspend fun upsertAll(history: List<ClientDebtHistory>)

    @Query("SELECT * FROM client_debt_history WHERE clientIdFk = :clientId ORDER BY eventDate DESC")
    fun getHistoryForClient(clientId: String): Flow<List<ClientDebtHistory>>

    @Query("SELECT * FROM client_debt_history WHERE clientIdFk = :clientId ORDER BY eventDate DESC")
    fun getHistoryForClientPagedSource(clientId: String): PagingSource<Int, ClientDebtHistory>

    @Query("""
        SELECT
            COUNT(*) AS eventCount,
            COALESCE(SUM(CASE WHEN amountChange > 0.0 THEN amountChange ELSE 0.0 END), 0.0) AS debtIncreases
        FROM client_debt_history
        WHERE clientIdFk = :clientId
    """)
    fun getSummaryForClient(clientId: String): Flow<ClientDebtHistorySummaryRow>

    @Query("SELECT COUNT(*) FROM client_debt_history WHERE clientIdFk = :clientId")
    suspend fun countHistoryForClient(clientId: String): Int

    @Query("SELECT * FROM client_debt_history")
    fun getAllDebtHistorySimple(): Flow<List<ClientDebtHistory>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN amountChange > 0.0 THEN amountChange ELSE 0.0 END), 0.0) AS debtIncreases,
            COALESCE(SUM(CASE WHEN eventType = :adjustmentType THEN amountChange ELSE 0.0 END), 0.0) AS debtAdjustments
        FROM client_debt_history
        WHERE eventDate BETWEEN :startDate AND :endDate
    """)
    fun getDebtTotalsForRange(
        startDate: Long,
        endDate: Long,
        adjustmentType: String
    ): Flow<CashClosingDebtRow>
}
