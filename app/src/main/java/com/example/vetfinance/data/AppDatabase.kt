package com.example.vetfinance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Transaction::class, Pet::class, Treatment::class, Product::class,
        Sale::class, Client::class, SaleProductCrossRef::class, Payment::class,
        Appointment::class, Supplier::class, Purchase::class, PurchaseProductCrossRef::class,
        RestockOrder::class, RestockOrderItem::class,
        AppointmentLog::class
    ],
    version = 22,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun clientDao(): ClientDao
    abstract fun paymentDao(): PaymentDao
    abstract fun petDao(): PetDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun restockDao(): RestockDao
    abstract fun appointmentLogDao(): AppointmentLogDao

}