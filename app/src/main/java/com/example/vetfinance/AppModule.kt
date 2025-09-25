package com.example.vetfinance

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vetfinance.data.AppDatabase
import com.example.vetfinance.data.ClientDao
import com.example.vetfinance.data.PaymentDao
import com.example.vetfinance.data.PetDao
import com.example.vetfinance.data.ProductDao
import com.example.vetfinance.data.SaleDao
import com.example.vetfinance.data.TransactionDao
import com.example.vetfinance.data.TreatmentDao
import com.example.vetfinance.data.AppointmentDao
import com.example.vetfinance.viewmodel.VetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Definición de la migración de la versión 10 a la 11
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Añade la columna 'cost' a la tabla 'products' con un valor por defecto de 0.0
        db.execSQL("ALTER TABLE products ADD COLUMN cost REAL NOT NULL DEFAULT 0.0")
        // Columnas faltantes en la tabla 'treatments'
        db.execSQL("ALTER TABLE treatments ADD COLUMN weight REAL")
        db.execSQL("ALTER TABLE treatments ADD COLUMN temperature REAL")
        db.execSQL("ALTER TABLE treatments ADD COLUMN symptoms TEXT")
        db.execSQL("ALTER TABLE treatments ADD COLUMN diagnosis TEXT")
        db.execSQL("ALTER TABLE treatments ADD COLUMN treatmentPlan TEXT")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "vet_finance_db"
        )
        .addMigrations(MIGRATION_10_11) // Se añade la migración
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    @Singleton
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    @Singleton
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    @Singleton
    fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides
    @Singleton
    fun providePetDao(db: AppDatabase): PetDao = db.petDao()

    @Provides
    @Singleton
    fun provideTreatmentDao(db: AppDatabase): TreatmentDao = db.treatmentDao()
    @Provides
    @Singleton
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao = db.appointmentDao()


    @Provides
    @Singleton
    fun provideVetRepository(
        db: AppDatabase, // Se añade la DB
        productDao: ProductDao,
        saleDao: SaleDao,
        transactionDao: TransactionDao,
        clientDao: ClientDao,
        paymentDao: PaymentDao,
        petDao: PetDao,
        treatmentDao: TreatmentDao,
        appointmentDao: AppointmentDao
    ): VetRepository {
        // Se pasa la DB al constructor del Repositorio
        return VetRepository(db, productDao, saleDao, transactionDao, clientDao, paymentDao, petDao, treatmentDao, appointmentDao)
    }
}