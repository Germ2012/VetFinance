package com.example.vetfinance.data

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @RawQuery(observedEntities = [Client::class, Pet::class, Product::class])
    fun searchGlobal(query: SupportSQLiteQuery): Flow<List<GlobalSearchRow>>
}
