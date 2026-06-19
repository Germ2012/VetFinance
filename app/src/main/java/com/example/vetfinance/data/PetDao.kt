package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: Pet)

    @Update
    suspend fun update(pet: Pet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<Pet>)

    @Transaction
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>>

    @Query("SELECT * FROM pets")
    fun getAllPetsSimple(): Flow<List<Pet>>
}
