package com.example.vetfinance

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vetfinance.data.AppDatabase
import com.example.vetfinance.viewmodel.VetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Migraciones
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE clients ADD COLUMN address TEXT")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pets ADD COLUMN breed TEXT")
        }
    }
    //... (y así sucesivamente para todas las migraciones)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vet_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Añade aquí todas tus migraciones
            .build()
    }

    @Provides
    @Singleton
    fun provideRepository(db: AppDatabase, @ApplicationContext context: Context): VetRepository {
        return VetRepository(
            db.productDao(),
            db.saleDao(),
            db.transactionDao(),
            db.clientDao(),
            db.paymentDao(),
            db.petDao(),
            db.treatmentDao(),
            db.appointmentDao(),
            context
        )
    }
}