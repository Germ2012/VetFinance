package com.example.vetfinance

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sales_products_cross_ref_new (
                    crossRefId TEXT NOT NULL,
                    saleId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    quantitySold REAL NOT NULL,
                    priceAtTimeOfSale REAL NOT NULL,
                    notes TEXT,
                    overridePrice REAL,
                    PRIMARY KEY(crossRefId),
                    FOREIGN KEY(saleId) REFERENCES sales(saleId) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(productId) REFERENCES products(productId) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO sales_products_cross_ref_new (
                    crossRefId, saleId, productId, quantitySold, priceAtTimeOfSale, notes, overridePrice
                )
                SELECT saleId || ':' || productId, saleId, productId, quantitySold, priceAtTimeOfSale, notes, overridePrice
                FROM sales_products_cross_ref
                """.trimIndent()
            )
            db.execSQL("DROP TABLE sales_products_cross_ref")
            db.execSQL("ALTER TABLE sales_products_cross_ref_new RENAME TO sales_products_cross_ref")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_products_cross_ref_productId ON sales_products_cross_ref(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_products_cross_ref_saleId ON sales_products_cross_ref(saleId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS payments_new (
                    paymentId TEXT NOT NULL,
                    clientIdFk TEXT NOT NULL,
                    amount REAL NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    PRIMARY KEY(paymentId),
                    FOREIGN KEY(clientIdFk) REFERENCES clients(clientId) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO payments_new (paymentId, clientIdFk, amount, paymentDate)
                SELECT paymentId, clientIdFk, amount, paymentDate
                FROM payments
                """.trimIndent()
            )
            db.execSQL("DROP TABLE payments")
            db.execSQL("ALTER TABLE payments_new RENAME TO payments")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_clientIdFk ON payments(clientIdFk)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS treatments_new (
                    treatmentId TEXT NOT NULL,
                    petIdFk TEXT NOT NULL,
                    serviceId TEXT,
                    treatmentDate INTEGER NOT NULL,
                    description TEXT,
                    nextTreatmentDate INTEGER,
                    isNextTreatmentCompleted INTEGER NOT NULL,
                    symptoms TEXT,
                    diagnosis TEXT,
                    treatmentPlan TEXT,
                    weight REAL,
                    temperature TEXT,
                    PRIMARY KEY(treatmentId),
                    FOREIGN KEY(petIdFk) REFERENCES pets(petId) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(serviceId) REFERENCES products(productId) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO treatments_new (
                    treatmentId, petIdFk, serviceId, treatmentDate, description, nextTreatmentDate,
                    isNextTreatmentCompleted, symptoms, diagnosis, treatmentPlan, weight, temperature
                )
                SELECT treatmentId, petIdFk, serviceId, treatmentDate, description, nextTreatmentDate,
                    isNextTreatmentCompleted, symptoms, diagnosis, treatmentPlan, weight, temperature
                FROM treatments
                """.trimIndent()
            )
            db.execSQL("DROP TABLE treatments")
            db.execSQL("ALTER TABLE treatments_new RENAME TO treatments")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_treatments_petIdFk ON treatments(petIdFk)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_treatments_serviceId ON treatments(serviceId)")
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
            .addMigrations(MIGRATION_22_23)
            .fallbackToDestructiveMigration()
            .build()
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
