package com.example.vetfinance

import android.content.Context
import androidx.room.Room
import com.example.vetfinance.data.AppDatabase
import com.example.vetfinance.data.ClientDao
import com.example.vetfinance.data.PaymentDao
import com.example.vetfinance.data.PetDao
import com.example.vetfinance.data.ProductDao
import com.example.vetfinance.data.SaleDao
import com.example.vetfinance.data.TransactionDao
import com.example.vetfinance.data.TreatmentDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "vet_finance_db"
        ).fallbackToDestructiveMigration().build()
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
    fun provideVetRepository(
        db: AppDatabase, // Se añade la DB
        productDao: ProductDao,
        saleDao: SaleDao,
        transactionDao: TransactionDao,
        clientDao: ClientDao,
        paymentDao: PaymentDao,
        petDao: PetDao,
        treatmentDao: TreatmentDao
    ): VetRepository {
        // Se pasa la DB al constructor del Repositorio
        return VetRepository(db, productDao, saleDao, transactionDao, clientDao, paymentDao, petDao, treatmentDao)
    }
}