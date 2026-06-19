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
        AppointmentLog::class, ClientDebtHistory::class, SupplierDebt::class,
        StockMovement::class
    ],
    version = 26,
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
    abstract fun clientDebtHistoryDao(): ClientDebtHistoryDao
    abstract fun supplierDebtDao(): SupplierDebtDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun searchDao(): SearchDao

    companion object {

        val SEARCH_INDEX_CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                ensureSearchIndexes(db)
            }
        }

        private fun ensureSearchIndexes(db: SupportSQLiteDatabase) {
            val useFts5 = supportsFts5(db)
            createProductsFts(db, useFts5)
            createClientsFts(db, useFts5)
            seedSearchIndexes(db)
        }

        private fun supportsFts5(db: SupportSQLiteDatabase): Boolean {
            db.query("PRAGMA compile_options").use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(0).contains("ENABLE_FTS5", ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun createProductsFts(db: SupportSQLiteDatabase, useFts5: Boolean) {
            if (useFts5) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `products_fts` USING fts5(
                        `productId` UNINDEXED,
                        `name`,
                        `category`,
                        `supplierIdFk` UNINDEXED,
                        tokenize = 'unicode61 remove_diacritics 2'
                    )
                """.trimIndent())
            } else {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `products_fts` USING fts4(
                        `productId`,
                        `name`,
                        `category`,
                        `supplierIdFk`,
                        tokenize = unicode61
                    )
                """.trimIndent())
            }
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `products_fts_insert`
                AFTER INSERT ON `products`
                BEGIN
                    INSERT INTO `products_fts`(`productId`, `name`, `category`, `supplierIdFk`)
                    VALUES (new.`productId`, new.`name`, COALESCE(new.`category`, ''), COALESCE(new.`supplierIdFk`, ''));
                END
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `products_fts_update`
                AFTER UPDATE OF `name`, `category`, `supplierIdFk` ON `products`
                BEGIN
                    DELETE FROM `products_fts` WHERE `productId` = old.`productId`;
                    INSERT INTO `products_fts`(`productId`, `name`, `category`, `supplierIdFk`)
                    VALUES (new.`productId`, new.`name`, COALESCE(new.`category`, ''), COALESCE(new.`supplierIdFk`, ''));
                END
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `products_fts_delete`
                AFTER DELETE ON `products`
                BEGIN
                    DELETE FROM `products_fts` WHERE `productId` = old.`productId`;
                END
            """.trimIndent())
        }

        private fun createClientsFts(db: SupportSQLiteDatabase, useFts5: Boolean) {
            if (useFts5) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `clients_fts` USING fts5(
                        `clientId` UNINDEXED,
                        `name`,
                        `phone`,
                        tokenize = 'unicode61 remove_diacritics 2'
                    )
                """.trimIndent())
            } else {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `clients_fts` USING fts4(
                        `clientId`,
                        `name`,
                        `phone`,
                        tokenize = unicode61
                    )
                """.trimIndent())
            }
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `clients_fts_insert`
                AFTER INSERT ON `clients`
                BEGIN
                    INSERT INTO `clients_fts`(`clientId`, `name`, `phone`)
                    VALUES (new.`clientId`, new.`name`, COALESCE(new.`phone`, ''));
                END
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `clients_fts_update`
                AFTER UPDATE OF `name`, `phone` ON `clients`
                BEGIN
                    DELETE FROM `clients_fts` WHERE `clientId` = old.`clientId`;
                    INSERT INTO `clients_fts`(`clientId`, `name`, `phone`)
                    VALUES (new.`clientId`, new.`name`, COALESCE(new.`phone`, ''));
                END
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `clients_fts_delete`
                AFTER DELETE ON `clients`
                BEGIN
                    DELETE FROM `clients_fts` WHERE `clientId` = old.`clientId`;
                END
            """.trimIndent())
        }

        private fun seedSearchIndexes(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM `products_fts` WHERE `productId` NOT IN (SELECT `productId` FROM `products`)")
            db.execSQL("DELETE FROM `clients_fts` WHERE `clientId` NOT IN (SELECT `clientId` FROM `clients`)")
            db.execSQL("""
                INSERT INTO `products_fts`(`productId`, `name`, `category`, `supplierIdFk`)
                SELECT p.`productId`, p.`name`, COALESCE(p.`category`, ''), COALESCE(p.`supplierIdFk`, '')
                FROM `products` AS p
                WHERE NOT EXISTS (
                    SELECT 1 FROM `products_fts` AS f WHERE f.`productId` = p.`productId`
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `clients_fts`(`clientId`, `name`, `phone`)
                SELECT c.`clientId`, c.`name`, COALESCE(c.`phone`, '')
                FROM `clients` AS c
                WHERE NOT EXISTS (
                    SELECT 1 FROM `clients_fts` AS f WHERE f.`clientId` = c.`clientId`
                )
            """.trimIndent())
        }


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

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `client_debt_history` (
                        `historyId` TEXT NOT NULL,
                        `clientIdFk` TEXT NOT NULL,
                        `eventDate` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `amountChange` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `note` TEXT,
                        PRIMARY KEY(`historyId`),
                        FOREIGN KEY(`clientIdFk`) REFERENCES `clients`(`clientId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_client_debt_history_clientIdFk` ON `client_debt_history` (`clientIdFk`)")
                db.execSQL("""
                    INSERT INTO `client_debt_history`
                    (`historyId`, `clientIdFk`, `eventDate`, `eventType`, `amountChange`, `balanceAfter`, `note`)
                    SELECT lower(hex(randomblob(16))), `clientId`, CAST(strftime('%s','now') AS INTEGER) * 1000,
                        'INITIAL', `debtAmount`, `debtAmount`, 'Saldo existente al actualizar la app'
                    FROM `clients`
                    WHERE `debtAmount` > 0
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `supplier_debts` (
                        `debtId` TEXT NOT NULL,
                        `supplierIdFk` TEXT,
                        `description` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isPaid` INTEGER NOT NULL,
                        `paidAt` INTEGER,
                        `note` TEXT,
                        PRIMARY KEY(`debtId`),
                        FOREIGN KEY(`supplierIdFk`) REFERENCES `suppliers`(`supplierId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_debts_supplierIdFk` ON `supplier_debts` (`supplierIdFk`)")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `appointments` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE `pets` ADD COLUMN `observations` TEXT")
                db.execSQL("ALTER TABLE `products` ADD COLUMN `category` TEXT")
                db.execSQL("ALTER TABLE `products` ADD COLUMN `unitMeasure` TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `movementId` TEXT NOT NULL,
                        `productIdFk` TEXT,
                        `productNameSnapshot` TEXT NOT NULL,
                        `movementDate` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `quantityChange` REAL NOT NULL,
                        `stockAfter` REAL NOT NULL,
                        `note` TEXT,
                        `unitCost` REAL,
                        PRIMARY KEY(`movementId`),
                        FOREIGN KEY(`productIdFk`) REFERENCES `products`(`productId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productIdFk` ON `stock_movements` (`productIdFk`)")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_supplierIdFk` ON `products` (`supplierIdFk`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_name` ON `clients` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_phone` ON `clients` (`phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_date` ON `sales` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_paymentDate` ON `payments` (`paymentDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_clientIdFk` ON `payments` (`clientIdFk`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_nextTreatmentDate` ON `treatments` (`nextTreatmentDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_appointmentDate` ON `appointments` (`appointmentDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_client_debt_history_eventDate` ON `client_debt_history` (`eventDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_saleId` ON `sales_products_cross_ref` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_products_cross_ref_productId` ON `sales_products_cross_ref` (`productId`)")
            }
        }

        private fun migrateCompositeSaleDetailsToIndependentRows(db: SupportSQLiteDatabase) {


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
