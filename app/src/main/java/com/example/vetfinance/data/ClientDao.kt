package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Upsert
    suspend fun upsertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE clientId = :clientId LIMIT 1")
    suspend fun getClientById(clientId: String): Client?

    @Query("SELECT * FROM clients WHERE clientId = :clientId LIMIT 1")
    fun getClientByIdFlow(clientId: String): Flow<Client?>

    @Query("SELECT * FROM clients WHERE lower(name) = lower(:name) ORDER BY name ASC LIMIT 1")
    suspend fun getClientByName(name: String): Client?

    @Query("SELECT COUNT(*) FROM payments WHERE clientIdFk = :clientId")
    suspend fun countPaymentsForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM pets WHERE ownerIdFk = :clientId")
    suspend fun countPetsForClient(clientId: String): Int

    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("SELECT * FROM clients WHERE debtAmount > 0 ORDER BY name ASC")
    fun getDebtClientsPagedSource(): PagingSource<Int, Client>

    @RawQuery(observedEntities = [Client::class])
    fun searchDebtClientsPagedSource(query: SupportSQLiteQuery): PagingSource<Int, Client>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getClientsPagedSource(): PagingSource<Int, Client>

    @RawQuery(observedEntities = [Client::class])
    fun searchClientsPagedSource(query: SupportSQLiteQuery): PagingSource<Int, Client>

    @RawQuery(observedEntities = [Client::class])
    fun searchClientSuggestions(query: SupportSQLiteQuery): Flow<List<Client>>

    @Query("""
        SELECT
            c.*,
            COALESCE(salesByClient.totalSold, 0.0) AS totalSold,
            COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
            c.debtAmount AS balance
        FROM clients AS c
        LEFT JOIN (
            SELECT clientIdFk, SUM(totalAmount) AS totalSold
            FROM sales
            GROUP BY clientIdFk
        ) AS salesByClient ON salesByClient.clientIdFk = c.clientId
        LEFT JOIN (
            SELECT clientIdFk, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY clientIdFk
        ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
        WHERE c.debtAmount > 0.0
        ORDER BY c.debtAmount DESC, c.name ASC
    """)
    fun getPendingCollectionRows(): Flow<List<DebtCollectionRow>>

    @Query("""
        SELECT
            c.*,
            COALESCE(salesByClient.totalSold, 0.0) AS totalSold,
            COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
            c.debtAmount AS balance
        FROM clients AS c
        LEFT JOIN (
            SELECT clientIdFk, SUM(totalAmount) AS totalSold
            FROM sales
            GROUP BY clientIdFk
        ) AS salesByClient ON salesByClient.clientIdFk = c.clientId
        LEFT JOIN (
            SELECT clientIdFk, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY clientIdFk
        ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
        WHERE c.debtAmount > 0.0
        ORDER BY c.debtAmount DESC, c.name COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun getDebtCollectionPreviewRows(limit: Int): Flow<List<DebtCollectionRow>>

    @Query("""
        SELECT
            c.*,
            COALESCE(salesByClient.totalSold, 0.0) AS totalSold,
            COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
            c.debtAmount AS balance
        FROM clients AS c
        LEFT JOIN (
            SELECT clientIdFk, SUM(totalAmount) AS totalSold
            FROM sales
            GROUP BY clientIdFk
        ) AS salesByClient ON salesByClient.clientIdFk = c.clientId
        LEFT JOIN (
            SELECT clientIdFk, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY clientIdFk
        ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
        WHERE
            (:includeZeroDebt = 1 OR c.debtAmount > 0.0)
            AND c.debtAmount >= :minimumDebt
        ORDER BY
            CASE WHEN :sortMode = 'Menor deuda' THEN c.debtAmount END ASC,
            CASE WHEN :sortMode = 'Nombre' THEN c.name END COLLATE NOCASE ASC,
            CASE WHEN :sortMode = 'Mayor deuda' THEN c.debtAmount END DESC,
            c.name COLLATE NOCASE ASC
    """)
    fun getDebtCollectionRowsPagedSource(
        includeZeroDebt: Int,
        minimumDebt: Double,
        sortMode: String
    ): PagingSource<Int, DebtCollectionRow>

    @RawQuery(observedEntities = [Client::class, Sale::class, Payment::class])
    fun searchDebtCollectionRowsPagedSource(query: SupportSQLiteQuery): PagingSource<Int, DebtCollectionRow>

    @Query("""
        SELECT
            COUNT(*) AS clientCount,
            COALESCE(SUM(balance), 0.0) AS totalPending,
            COALESCE(SUM(totalPaid), 0.0) AS totalPaid
        FROM (
            SELECT
                c.clientId,
                COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
                c.debtAmount AS balance
            FROM clients AS c
            LEFT JOIN (
                SELECT clientIdFk, SUM(amount) AS totalPaid
                FROM payments
                GROUP BY clientIdFk
            ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
            WHERE
                (:includeZeroDebt = 1 OR c.debtAmount > 0.0)
                AND c.debtAmount >= :minimumDebt
        )
    """)
    fun getDebtCollectionSummary(
        includeZeroDebt: Int,
        minimumDebt: Double
    ): Flow<DebtCollectionSummary>

    @RawQuery(observedEntities = [Client::class, Payment::class])
    fun searchDebtCollectionSummary(query: SupportSQLiteQuery): Flow<DebtCollectionSummary>

    @Query("SELECT SUM(debtAmount) FROM clients")
    fun getTotalDebt(): Flow<Double?>

    @Query("""
        SELECT
            c.clientId,
            c.name AS clientName,
            COALESCE(SUM(s.totalAmount), 0.0) AS totalPurchased,
            COUNT(s.saleId) AS saleCount
        FROM clients AS c
        JOIN sales AS s ON c.clientId = s.clientIdFk
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY c.clientId
        ORDER BY totalPurchased DESC
        LIMIT :limit
    """)
    fun getClientPurchaseReports(startDate: Long, endDate: Long, limit: Int): Flow<List<ClientPurchaseReportRow>>
}
