package com.example.vetfinance.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [Transaction::class, Pet::class, Treatment::class, Product::class, Sale::class, Client::class, SaleProductCrossRef::class, Payment::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun clientDao(): ClientDao
    abstract fun paymentDao(): PaymentDao
    abstract fun petDao(): PetDao
    abstract fun treatmentDao(): TreatmentDao
}