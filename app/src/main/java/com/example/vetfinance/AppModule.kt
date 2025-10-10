package com.example.vetfinance

import android.content.Context
import androidx.room.Room
import com.example.vetfinance.data.AppDatabase
import com.example.vetfinance.data.AppointmentDao
import com.example.vetfinance.data.AppointmentLogDao // Importación añadida
import com.example.vetfinance.data.ClientDao
import com.example.vetfinance.data.PaymentDao
import com.example.vetfinance.data.PetDao
import com.example.vetfinance.data.ProductDao
import com.example.vetfinance.data.PurchaseDao
import com.example.vetfinance.data.RestockDao
import com.example.vetfinance.data.SaleDao
import com.example.vetfinance.data.SupplierDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vet_database"
        ).fallbackToDestructiveMigration().build()
    }

    // --- DAO Providers ---
    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun providePetDao(db: AppDatabase): PetDao = db.petDao()

    @Provides
    fun provideTreatmentDao(db: AppDatabase): TreatmentDao = db.treatmentDao()

    @Provides
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao = db.appointmentDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun provideRestockDao(db: AppDatabase): RestockDao = db.restockDao()

    // DAO Provider añadido del segundo código
    @Provides
    fun provideAppointmentLogDao(db: AppDatabase): AppointmentLogDao = db.appointmentLogDao()


    // --- Repository Provider (Actualizado) ---
    @Provides
    @Singleton
    fun provideRepository(
        db: AppDatabase,
        productDao: ProductDao,
        saleDao: SaleDao,
        transactionDao: TransactionDao,
        clientDao: ClientDao,
        paymentDao: PaymentDao,
        petDao: PetDao,
        treatmentDao: TreatmentDao,
        appointmentDao: AppointmentDao,
        supplierDao: SupplierDao,
        purchaseDao: PurchaseDao,
        restockDao: RestockDao,
        appointmentLogDao: AppointmentLogDao, // Parámetro añadido
        @ApplicationContext context: Context
    ): VetRepository {
        return VetRepository(
            db,
            productDao,
            saleDao,
            transactionDao,
            clientDao,
            paymentDao,
            petDao,
            treatmentDao,
            appointmentDao,
            supplierDao,
            purchaseDao,
            restockDao,
            appointmentLogDao, // Argumento añadido
            context
        )
    }
}