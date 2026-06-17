package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// --- RELATION CLASSES ---
@Immutable
data class AppointmentWithDetails(
    @Embedded val appointment: Appointment,
    @Relation(
        parentColumn = "petIdFk",
        entityColumn = "petId"
    )
    val pet: Pet,
    @Relation(
        parentColumn = "clientIdFk",
        entityColumn = "clientId"
    )
    val client: Client
)

@Immutable
data class TopSellingProduct(
    val name: String,
    val totalSold: Double,
    val totalRevenue: Double
)

@Immutable
data class DailySalesTotal(
    val dayIndex: Int,
    val totalSales: Double
)

@Immutable
data class CategoryProfitRow(
    val category: String,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@Immutable
data class FinancialSummaryRow(
    val salesTotal: Double,
    val grossProfit: Double
)

@Immutable
data class StockHealthRow(
    val optimalCount: Int,
    val lowStockCount: Int
)

@Immutable
data class CashClosingSalesRow(
    val salesCount: Int,
    val salesTotal: Double
)

@Immutable
data class CashClosingDebtRow(
    val debtIncreases: Double,
    val debtAdjustments: Double
)

@Immutable
data class GlobalSearchRow(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String
)

@Immutable
data class DebtCollectionRow(
    @Embedded val client: Client,
    val totalSold: Double,
    val totalPaid: Double,
    val balance: Double
)

@Immutable
data class DebtCollectionSummary(
    val clientCount: Int,
    val totalPending: Double,
    val totalPaid: Double
)

@Immutable
data class ProductProfitReportRow(
    val productId: String,
    val name: String,
    val isService: Boolean,
    val totalSold: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@Immutable
data class ClientPurchaseReportRow(
    val clientId: String,
    val clientName: String,
    val totalPurchased: Double,
    val saleCount: Int
)


// --- DAOs ---
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE productId = :productId")
    suspend fun getProductById(productId: String): Product?

    @Query("""
        SELECT *
        FROM products
        WHERE
            (
                :filterType = 'Todos'
                OR (:filterType = 'Productos' AND isService = 0 AND sellingMethod != 'Solo Dosis')
                OR (:filterType = 'Servicios' AND isService = 1)
                OR (:filterType = 'Dosis' AND isService = 0 AND sellingMethod = 'Solo Dosis')
                OR (:filterType = 'Bajo stock' AND isService = 0 AND isContainer = 0 AND sellingMethod != 'Solo Dosis'
                    AND COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
                    AND stock < COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END))
            )
            AND (
                :searchQuery = ''
                OR name LIKE '%' || :searchQuery || '%'
                OR category LIKE '%' || :searchQuery || '%'
                OR supplierIdFk LIKE '%' || :searchQuery || '%'
            )
        ORDER BY name ASC
    """)
    fun getProductsPagedSource(filterType: String, searchQuery: String): PagingSource<Int, Product>

    @Query("""
        SELECT *
        FROM products
        WHERE :query != ''
            AND (
                name LIKE '%' || :query || '%'
                OR category LIKE '%' || :query || '%'
            )
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun searchProductSuggestions(query: String, limit: Int = 8): Flow<List<Product>>

    @Query("SELECT SUM(cost * stock) FROM products WHERE isService = 0")
    fun getTotalInventoryValue(): Flow<Double?>

    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                    WHEN COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
                        AND stock < COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
                    THEN 1 ELSE 0
                END
            ), 0) AS lowStockCount,
            COALESCE(SUM(
                CASE
                    WHEN COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) <= 0.0
                        OR stock >= COALESCE(lowStockThreshold, CASE WHEN sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
                    THEN 1 ELSE 0
                END
            ), 0) AS optimalCount
        FROM products
        WHERE isService = 0
            AND isContainer = 0
            AND sellingMethod != 'Solo Dosis'
    """)
    fun getStockHealthSummary(): Flow<StockHealthRow>

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT COUNT(*) FROM sales_products_cross_ref WHERE productId = :productId")
    suspend fun countSaleDetailsForProduct(productId: String): Int

    // --- FUNCTION ADDED ---
    @Query("SELECT * FROM products WHERE isContainer = 1 AND containedProductId = :containedProductId LIMIT 1")
    suspend fun findContainerForProduct(containedProductId: String): Product?
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleProductCrossRef(crossRef: SaleProductCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleProductCrossRefs(crossRefs: List<SaleProductCrossRef>)

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("DELETE FROM sales_products_cross_ref WHERE saleId = :saleId")
    suspend fun deleteSaleProductCrossRefs(saleId: String)

    @Query("SELECT * FROM sales_products_cross_ref WHERE saleId = :saleId")
    suspend fun getSaleDetailsBySaleId(saleId: String): List<SaleProductCrossRef>

    @androidx.room.Transaction
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithProducts(): Flow<List<SaleWithProducts>>

    @Query("""
        SELECT
            p.name,
            SUM(sp.quantitySold) as totalSold,
            SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)) as totalRevenue
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProductsByQuantity(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>

    @Query("""
        SELECT
            p.name,
            SUM(sp.quantitySold) as totalSold,
            SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)) as totalRevenue
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalRevenue DESC
        LIMIT :limit
    """)
    fun getTopSellingProductsByRevenue(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>

    @Query("""
        SELECT
            CAST((date - :startDate) / 86400000 AS INTEGER) AS dayIndex,
            COALESCE(SUM(totalAmount), 0.0) AS totalSales
        FROM sales
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY dayIndex
        ORDER BY dayIndex ASC
    """)
    fun getDailySalesTotals(startDate: Long, endDate: Long): Flow<List<DailySalesTotal>>

    @Query("""
        SELECT
            COUNT(*) AS salesCount,
            COALESCE(SUM(totalAmount), 0.0) AS salesTotal
        FROM sales
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun getSalesTotalsForRange(startDate: Long, endDate: Long): Flow<CashClosingSalesRow>

    @Query("""
        SELECT
            COALESCE((
                SELECT SUM(totalAmount)
                FROM sales
                WHERE date BETWEEN :startDate AND :endDate
            ), 0.0) AS salesTotal,
            COALESCE((
                SELECT SUM(
                    COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)
                    - (COALESCE(p.cost, 0.0) * sp.quantitySold)
                )
                FROM sales_products_cross_ref AS sp
                JOIN sales AS s ON sp.saleId = s.saleId
                LEFT JOIN products AS p ON sp.productId = p.productId
                WHERE s.date BETWEEN :startDate AND :endDate
            ), 0.0) AS grossProfit
    """)
    fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummaryRow>

    @Query("""
        SELECT
            CASE
                WHEN p.isService = 1 THEN 'Servicios'
                WHEN p.category IS NULL OR TRIM(p.category) = '' THEN 'Sin categoria'
                ELSE p.category
            END AS category,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)), 0.0) AS revenue,
            COALESCE(SUM(COALESCE(p.cost, 0.0) * sp.quantitySold), 0.0) AS cost,
            COALESCE(SUM(
                COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)
                - (COALESCE(p.cost, 0.0) * sp.quantitySold)
            ), 0.0) AS profit
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY profit DESC
        LIMIT :limit
    """)
    fun getCategoryProfitRows(startDate: Long, endDate: Long, limit: Int = 8): Flow<List<CategoryProfitRow>>

    @Query("""
        SELECT p.*
        FROM products AS p
        INNER JOIN (
            SELECT productId, SUM(quantitySold) AS totalSold
            FROM sales_products_cross_ref
            GROUP BY productId
        ) AS ranked ON ranked.productId = p.productId
        ORDER BY ranked.totalSold DESC, p.name ASC
        LIMIT :limit
    """)
    fun getFrequentSaleProducts(limit: Int = 6): Flow<List<Product>>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name,
            p.isService,
            COALESCE(SUM(sp.quantitySold), 0.0) AS totalSold,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)), 0.0) AS revenue,
            COALESCE(SUM(COALESCE(p.cost, 0.0) * sp.quantitySold), 0.0) AS cost,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold) - (COALESCE(p.cost, 0.0) * sp.quantitySold)), 0.0) AS profit
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.productId, p.name, p.isService
        ORDER BY profit DESC
    """)
    fun getProductProfitReports(startDate: Long, endDate: Long): Flow<List<ProductProfitReportRow>>

    @Query("SELECT * FROM sales")
    fun getAllSalesSimple(): Flow<List<Sale>>
    @Query("SELECT * FROM sales_products_cross_ref")
    fun getAllSaleProductCrossRefsSimple(): Flow<List<SaleProductCrossRef>>
}

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE clientId = :clientId LIMIT 1")
    suspend fun getClientById(clientId: String): Client?

    @Query("SELECT * FROM clients WHERE lower(name) = lower(:name) ORDER BY name ASC LIMIT 1")
    suspend fun getClientByName(name: String): Client?

    @Query("SELECT COUNT(*) FROM payments WHERE clientIdFk = :clientId")
    suspend fun countPaymentsForClient(clientId: String): Int

    @Query("SELECT COUNT(*) FROM pets WHERE ownerIdFk = :clientId")
    suspend fun countPetsForClient(clientId: String): Int

    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("SELECT * FROM clients WHERE debtAmount > 0 AND name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun getDebtClientsPagedSource(searchQuery: String): PagingSource<Int, Client>

    @Query("SELECT * FROM clients WHERE (:searchQuery = '' OR name LIKE '%' || :searchQuery || '%' OR phone LIKE '%' || :searchQuery || '%') ORDER BY name ASC")
    fun getClientsPagedSource(searchQuery: String): PagingSource<Int, Client>

    @Query("""
        SELECT *
        FROM clients
        WHERE :query != ''
            AND (
                name LIKE '%' || :query || '%'
                OR phone LIKE '%' || :query || '%'
            )
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun searchClientSuggestions(query: String, limit: Int = 6): Flow<List<Client>>

    @Query("""
        SELECT
            c.*,
            COALESCE(salesByClient.totalSold, 0.0) AS totalSold,
            COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
            c.debtAmount AS balance
        FROM clients AS c
        LEFT JOIN (
            SELECT clientIdFk, SUM(totalAmount) AS totalSold
            FROM sales
            GROUP BY clientIdFk
        ) AS salesByClient ON salesByClient.clientIdFk = c.clientId
        LEFT JOIN (
            SELECT clientIdFk, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY clientIdFk
        ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
        WHERE c.debtAmount > 0.0
        ORDER BY c.debtAmount DESC, c.name ASC
    """)
    fun getPendingCollectionRows(): Flow<List<DebtCollectionRow>>

    @Query("""
        SELECT
            c.*,
            COALESCE(salesByClient.totalSold, 0.0) AS totalSold,
            COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
            c.debtAmount AS balance
        FROM clients AS c
        LEFT JOIN (
            SELECT clientIdFk, SUM(totalAmount) AS totalSold
            FROM sales
            GROUP BY clientIdFk
        ) AS salesByClient ON salesByClient.clientIdFk = c.clientId
        LEFT JOIN (
            SELECT clientIdFk, SUM(amount) AS totalPaid
            FROM payments
            GROUP BY clientIdFk
        ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
        WHERE
            (:includeZeroDebt = 1 OR c.debtAmount > 0.0)
            AND c.debtAmount >= :minimumDebt
            AND (
                :searchQuery = ''
                OR c.name LIKE '%' || :searchQuery || '%'
                OR c.phone LIKE '%' || :searchQuery || '%'
            )
        ORDER BY
            CASE WHEN :sortMode = 'Menor deuda' THEN c.debtAmount END ASC,
            CASE WHEN :sortMode = 'Nombre' THEN c.name END COLLATE NOCASE ASC,
            CASE WHEN :sortMode = 'Mayor deuda' THEN c.debtAmount END DESC,
            c.name COLLATE NOCASE ASC
    """)
    fun getDebtCollectionRowsPagedSource(
        searchQuery: String,
        includeZeroDebt: Int,
        minimumDebt: Double,
        sortMode: String
    ): PagingSource<Int, DebtCollectionRow>

    @Query("""
        SELECT
            COUNT(*) AS clientCount,
            COALESCE(SUM(balance), 0.0) AS totalPending,
            COALESCE(SUM(totalPaid), 0.0) AS totalPaid
        FROM (
            SELECT
                c.clientId,
                COALESCE(paymentsByClient.totalPaid, 0.0) AS totalPaid,
                c.debtAmount AS balance
            FROM clients AS c
            LEFT JOIN (
                SELECT clientIdFk, SUM(amount) AS totalPaid
                FROM payments
                GROUP BY clientIdFk
            ) AS paymentsByClient ON paymentsByClient.clientIdFk = c.clientId
            WHERE
                (:includeZeroDebt = 1 OR c.debtAmount > 0.0)
                AND c.debtAmount >= :minimumDebt
                AND (
                    :searchQuery = ''
                    OR c.name LIKE '%' || :searchQuery || '%'
                    OR c.phone LIKE '%' || :searchQuery || '%'
                )
        )
    """)
    fun getDebtCollectionSummary(
        searchQuery: String,
        includeZeroDebt: Int,
        minimumDebt: Double
    ): Flow<DebtCollectionSummary>

    @Query("SELECT SUM(debtAmount) FROM clients")
    fun getTotalDebt(): Flow<Double?>

    @Query("""
        SELECT
            c.clientId,
            c.name AS clientName,
            COALESCE(SUM(s.totalAmount), 0.0) AS totalPurchased,
            COUNT(s.saleId) AS saleCount
        FROM clients AS c
        JOIN sales AS s ON c.clientId = s.clientIdFk
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY c.clientId
        ORDER BY totalPurchased DESC
        LIMIT :limit
    """)
    fun getClientPurchaseReports(startDate: Long, endDate: Long, limit: Int): Flow<List<ClientPurchaseReportRow>>
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>



    @Query("SELECT * FROM payments")
    fun getAllPaymentsSimple(): Flow<List<Payment>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM payments
        WHERE paymentDate BETWEEN :startDate AND :endDate
    """)
    fun getPaymentsTotalForRange(startDate: Long, endDate: Long): Flow<Double>
}

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: Pet)

    @Update
    suspend fun update(pet: Pet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<Pet>)

    @androidx.room.Transaction
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>>

    @Query("SELECT * FROM pets")
    fun getAllPetsSimple(): Flow<List<Pet>>
}

@Dao
interface TreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: Treatment)
    @Update
    suspend fun update(treatment: Treatment)

    @Delete
    suspend fun delete(treatment: Treatment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treatments: List<Treatment>)

    @Query("SELECT * FROM treatments WHERE petIdFk = :petId ORDER BY treatmentDate DESC")
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate IS NOT NULL AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    fun getUpcomingTreatments(): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate BETWEEN :startDate AND :endDate AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    suspend fun getUpcomingTreatmentsForRange(startDate: Long, endDate: Long): List<Treatment>

    @Query("UPDATE treatments SET isNextTreatmentCompleted = 1 WHERE treatmentId = :treatmentId")
    suspend fun markAsCompleted(treatmentId: String)

    @Query("SELECT * FROM treatments")
    fun getAllTreatmentsSimple(): Flow<List<Treatment>>
}

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseProductCrossRef(crossRef: PurchaseProductCrossRef)

    @Query("SELECT * FROM purchases WHERE isCredit = 1 AND isPaid = 0 AND dueDate IS NOT NULL AND dueDate <= :dateLimit ORDER BY dueDate ASC")
    fun getUnpaidPurchasesWithUpcomingDueDate(dateLimit: Long): Flow<List<Purchase>>

    @Query("UPDATE purchases SET isPaid = 1 WHERE purchaseId = :purchaseId")
    suspend fun markPurchaseAsPaid(purchaseId: String)
}

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: Appointment)

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @androidx.room.Transaction
    @Query("SELECT * FROM appointments WHERE appointmentDate >= :startDate AND appointmentDate < :endDate ORDER BY appointmentDate ASC")
    fun getAppointmentsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<Appointment>)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT * FROM appointments")
    fun getAllAppointmentsSimple(): Flow<List<Appointment>>
}

@Dao
interface AppointmentLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppointmentLog)

    @Query("SELECT * FROM appointment_logs WHERE originalAppointmentDate >= :startDate AND originalAppointmentDate < :endDate ORDER BY originalAppointmentDate DESC")
    fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentLog>>
}

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>)

    @Query("SELECT * FROM stock_movements WHERE productIdFk = :productId ORDER BY movementDate DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY movementDate DESC")
    fun getAllStockMovementsSimple(): Flow<List<StockMovement>>
}

@Dao
interface SearchDao {
    @Query("""
        SELECT id, type, title, subtitle
        FROM (
            SELECT
                clientId AS id,
                'client' AS type,
                'Cliente: ' || name AS title,
                COALESCE(phone, 'Sin telefono') AS subtitle,
                0 AS priority,
                name AS sortName
            FROM clients
            WHERE :query != ''
                AND (
                    name LIKE '%' || :query || '%'
                    OR phone LIKE '%' || :query || '%'
                )

            UNION ALL

            SELECT
                p.petId AS id,
                'pet' AS type,
                'Mascota: ' || p.name AS title,
                'Dueno: ' || c.name AS subtitle,
                1 AS priority,
                p.name AS sortName
            FROM pets AS p
            INNER JOIN clients AS c ON c.clientId = p.ownerIdFk
            WHERE :query != ''
                AND (
                    p.name LIKE '%' || :query || '%'
                    OR c.name LIKE '%' || :query || '%'
                )

            UNION ALL

            SELECT
                productId AS id,
                'product' AS type,
                CASE WHEN isService = 1 THEN 'Servicio: ' ELSE 'Producto: ' END || name AS title,
                COALESCE(category, sellingMethod) AS subtitle,
                2 AS priority,
                name AS sortName
            FROM products
            WHERE :query != ''
                AND (
                    name LIKE '%' || :query || '%'
                    OR category LIKE '%' || :query || '%'
                )
        )
        ORDER BY priority ASC, sortName COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun searchGlobal(query: String, limit: Int = 12): Flow<List<GlobalSearchRow>>
}
