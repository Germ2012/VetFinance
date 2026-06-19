package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: SupplierDebt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<SupplierDebt>)

    @Upsert
    suspend fun upsertAll(debts: List<SupplierDebt>)

    @Update
    suspend fun update(debt: SupplierDebt)

    @Query("SELECT * FROM supplier_debts WHERE debtId = :debtId LIMIT 1")
    suspend fun getById(debtId: String): SupplierDebt?

    @Query("""
        SELECT sd.*, s.name AS supplierName
        FROM supplier_debts AS sd
        LEFT JOIN suppliers AS s ON sd.supplierIdFk = s.supplierId
        WHERE sd.dueDate >= :startDate AND sd.dueDate < :endDate
        ORDER BY sd.dueDate ASC
    """)
    fun getDebtsForDateRange(startDate: Long, endDate: Long): Flow<List<SupplierDebtWithSupplier>>

    @Query("""
        SELECT sd.*, s.name AS supplierName
        FROM supplier_debts AS sd
        LEFT JOIN suppliers AS s ON sd.supplierIdFk = s.supplierId
        WHERE sd.isPaid = 0 AND sd.dueDate <= :dateLimit
        ORDER BY sd.dueDate ASC
    """)
    fun getUpcomingUnpaidDebts(dateLimit: Long): Flow<List<SupplierDebtWithSupplier>>

    @Query("SELECT * FROM supplier_debts")
    fun getAllSupplierDebtsSimple(): Flow<List<SupplierDebt>>
}
