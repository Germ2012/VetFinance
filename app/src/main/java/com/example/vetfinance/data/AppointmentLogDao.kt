package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppointmentLog)

    @Query("SELECT * FROM appointment_logs WHERE originalAppointmentDate >= :startDate AND originalAppointmentDate < :endDate ORDER BY originalAppointmentDate DESC")
    fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentLog>>
}
