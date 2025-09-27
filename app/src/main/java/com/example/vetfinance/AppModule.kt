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

    // Define tus migraciones aquí si las necesitas
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Ejemplo: database.execSQL("ALTER TABLE clients ADD COLUMN address TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vet_database"
        )
            // .addMigrations(MIGRATION_1_2) // Descomenta y añade tus migraciones si las tienes
            .build()
    }

    @Provides
    @Singleton
    fun provideRepository(db: AppDatabase, @ApplicationContext context: Context): VetRepository {
        // CORRECCIÓN CLAVE: Pasamos todos los DAOs desde la instancia 'db'
        return VetRepository(
            productDao = db.productDao(),
            saleDao = db.saleDao(),
            transactionDao = db.transactionDao(),
            clientDao = db.clientDao(),
            paymentDao = db.paymentDao(),
            petDao = db.petDao(),
            treatmentDao = db.treatmentDao(),
            appointmentDao = db.appointmentDao(),
            context = context
        )
    }
}