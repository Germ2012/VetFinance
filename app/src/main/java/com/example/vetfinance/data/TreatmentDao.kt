package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: Treatment)

    @Update
    suspend fun update(treatment: Treatment)

    @Delete
    suspend fun delete(treatment: Treatment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treatments: List<Treatment>)

    @Query("SELECT * FROM treatments WHERE petIdFk = :petId ORDER BY treatmentDate DESC")
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate IS NOT NULL AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    fun getUpcomingTreatments(): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate BETWEEN :startDate AND :endDate AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    suspend fun getUpcomingTreatmentsForRange(startDate: Long, endDate: Long): List<Treatment>

    @Query("UPDATE treatments SET isNextTreatmentCompleted = 1 WHERE treatmentId = :treatmentId")
    suspend fun markAsCompleted(treatmentId: String)

    @Query("SELECT * FROM treatments")
    fun getAllTreatmentsSimple(): Flow<List<Treatment>>
}
