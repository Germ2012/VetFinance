package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// --- CLASES DE RELACIÓN ---
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

data class TopSellingProduct(val name: String, val totalSold: Double)


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

    @Query("SELECT * FROM products WHERE (:filterType = 'Todos') OR (:filterType = 'Productos' AND isService = 0) OR (:filterType = 'Servicios' AND isService = 1) ORDER BY name ASC")
    fun getProductsPagedSource(filterType: String): PagingSource<Int, Product>

    @Query("SELECT SUM(price * stock) FROM products WHERE isService = 0")
    fun getTotalInventoryValue(): Flow<Double?>

    @Delete
    suspend fun delete(product: Product)
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
        SELECT p.name, SUM(sp.quantitySold) as totalSold
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>

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

    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("SELECT * FROM clients WHERE debtAmount > 0 AND name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun getDebtClientsPagedSource(searchQuery: String): PagingSource<Int, Client>

    @Query("SELECT SUM(debtAmount) FROM clients")
    fun getTotalDebt(): Flow<Double?>
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
@Entity(tableName = "appointment_logs")
data class AppointmentLog(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val originalAppointmentDate: Long,
    val clientName: String,
    val petName: String,
    val cancellationReason: String,
    val cancelledOnDate: Long = System.currentTimeMillis()
)

@Dao
interface AppointmentLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppointmentLog)

    @Query("SELECT * FROM appointment_logs WHERE originalAppointmentDate >= :startDate AND originalAppointmentDate < :endDate ORDER BY originalAppointmentDate DESC")
    fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentLog>>
}

