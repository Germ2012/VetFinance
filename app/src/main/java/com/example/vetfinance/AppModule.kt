package com.example.vetfinance

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vet_database"
        ).fallbackToDestructiveMigration().build() // fallbackToDestructiveMigration para evitar problemas de migración durante el desarrollo
    }

    @Provides
    @Singleton
    fun provideRepository(db: AppDatabase, @ApplicationContext context: Context): VetRepository {
        return VetRepository(
            db = db, // <-- LÍNEA AÑADIDA
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