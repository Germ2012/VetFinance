package com.example.vetfinance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class, Pet::class, Treatment::class, Product::class,
        Sale::class, Client::class, SaleProductCrossRef::class, Payment::class,
        Appointment::class, Supplier::class, Purchase::class, PurchaseProductCrossRef::class,
        RestockOrder::class, RestockOrderItem::class,
        AppointmentLog::class
    ],
    version = 23,
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

    companion object {
        /**
         * Migraciones conservadoras para evitar el borrado silencioso de datos.
         *
         * Antes se usaba fallbackToDestructiveMigration(), lo que podía eliminar toda la
         * base local al cambiar el esquema. Estas migraciones cubren las versiones
         * presentes en app/schemas (19 -> 22). Para versiones anteriores a 19, la app
         * fallará de forma explícita en vez de borrar datos sin avisar.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_products_cross_ref_new` (
                        `crossRefId` TEXT NOT NULL,
                        `saleId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `quantitySold` REAL NOT NULL,
                        `priceAtTimeOfSale` REAL NOT NULL,
                        `notes` TEXT,
                        `overridePrice` REAL,
                        PRIMARY KEY(`crossRefId`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`saleId`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`productId`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO `sales_products_cross_ref_new`
                    (`crossRefId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`)
                    SELECT `saleId` || '_' || `productId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`
                    FROM `sales_products_cross_ref`
                """.trimIndent())
                db.execSQL("DROP TABLE `sales_products_cross_ref`")
                db.execSQL("ALTER TABLE `sales_products_cross_ref_new` RENAME TO `sales_products_cross_ref`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_saleId` ON `sales_products_cross_ref` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_productId` ON `sales_products_cross_ref` (`productId`)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_products_cross_ref_new` (
                        `saleId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `quantitySold` REAL NOT NULL,
                        `priceAtTimeOfSale` REAL NOT NULL,
                        `notes` TEXT,
                        `overridePrice` REAL,
                        PRIMARY KEY(`saleId`, `productId`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`saleId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`productId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO `sales_products_cross_ref_new`
                    (`saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`)
                    SELECT `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`
                    FROM `sales_products_cross_ref`
                """.trimIndent())
                db.execSQL("DROP TABLE `sales_products_cross_ref`")
                db.execSQL("ALTER TABLE `sales_products_cross_ref_new` RENAME TO `sales_products_cross_ref`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_saleId` ON `sales_products_cross_ref` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_productId` ON `sales_products_cross_ref` (`productId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `appointment_logs` (
                        `logId` TEXT NOT NULL,
                        `originalAppointmentDate` INTEGER NOT NULL,
                        `clientName` TEXT NOT NULL,
                        `petName` TEXT NOT NULL,
                        `cancellationReason` TEXT NOT NULL,
                        `cancelledOnDate` INTEGER NOT NULL,
                        PRIMARY KEY(`logId`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No hubo cambios estructurales entre los schemas 21 y 22.
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCompositeSaleDetailsToIndependentRows(db)
            }
        }


        val MIGRATION_20_23 = object : Migration(20, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `appointment_logs` (
                        `logId` TEXT NOT NULL,
                        `originalAppointmentDate` INTEGER NOT NULL,
                        `clientName` TEXT NOT NULL,
                        `petName` TEXT NOT NULL,
                        `cancellationReason` TEXT NOT NULL,
                        `cancelledOnDate` INTEGER NOT NULL,
                        PRIMARY KEY(`logId`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales_products_cross_ref_new` (
                        `crossRefId` TEXT NOT NULL,
                        `saleId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `quantitySold` REAL NOT NULL,
                        `priceAtTimeOfSale` REAL NOT NULL,
                        `notes` TEXT,
                        `overridePrice` REAL,
                        PRIMARY KEY(`crossRefId`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`saleId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`productId`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR REPLACE INTO `sales_products_cross_ref_new`
                    (`crossRefId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`)
                    SELECT `crossRefId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`
                    FROM `sales_products_cross_ref`
                """.trimIndent())
                db.execSQL("DROP TABLE `sales_products_cross_ref`")
                db.execSQL("ALTER TABLE `sales_products_cross_ref_new` RENAME TO `sales_products_cross_ref`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_saleId` ON `sales_products_cross_ref` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_productId` ON `sales_products_cross_ref` (`productId`)")
            }
        }

        val MIGRATION_21_23 = object : Migration(21, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateCompositeSaleDetailsToIndependentRows(db)
            }
        }

        private fun migrateCompositeSaleDetailsToIndependentRows(db: SupportSQLiteDatabase) {
            /*
             * Vuelve a usar un ID propio para cada detalle de venta.
             * Con la clave primaria compuesta (saleId, productId), dos dosis o dos
             * líneas del mismo producto en una misma venta se pisaban entre sí.
             */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `sales_products_cross_ref_new` (
                    `crossRefId` TEXT NOT NULL,
                    `saleId` TEXT NOT NULL,
                    `productId` TEXT NOT NULL,
                    `quantitySold` REAL NOT NULL,
                    `priceAtTimeOfSale` REAL NOT NULL,
                    `notes` TEXT,
                    `overridePrice` REAL,
                    PRIMARY KEY(`crossRefId`),
                    FOREIGN KEY(`saleId`) REFERENCES `sales`(`saleId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`productId`) REFERENCES `products`(`productId`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
            """.trimIndent())
            db.execSQL("""
                INSERT OR REPLACE INTO `sales_products_cross_ref_new`
                (`crossRefId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`)
                SELECT `saleId` || '_' || `productId`, `saleId`, `productId`, `quantitySold`, `priceAtTimeOfSale`, `notes`, `overridePrice`
                FROM `sales_products_cross_ref`
            """.trimIndent())
            db.execSQL("DROP TABLE `sales_products_cross_ref`")
            db.execSQL("ALTER TABLE `sales_products_cross_ref_new` RENAME TO `sales_products_cross_ref`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_saleId` ON `sales_products_cross_ref` (`saleId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_productId` ON `sales_products_cross_ref` (`productId`)")
        }
    }

}