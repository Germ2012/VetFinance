package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- CLASES DE RELACIÓN (NUEVAS Y CORREGIDAS) ---

/**
 * Data class para obtener los detalles completos de una cita,
 * incluyendo la mascota y el cliente asociados.
 */
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

/**
 * Data class para el resultado de la consulta de los productos más vendidos.
 */
data class TopSellingProduct(val name: String, val totalSold: Int)


// --- DAOs ---

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<Transaction>
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsPaged(limit: Int, offset: Int): List<Product>

    @Query("SELECT * FROM products WHERE (:filterType = 'Todos') OR (:filterType = 'Productos' AND isService = 0) OR (:filterType = 'Servicios' AND isService = 1) ORDER BY name ASC")
    fun getProductsPagedSource(filterType: String): PagingSource<Int, Product>
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

    @androidx.room.Transaction // <-- CORREGIDO
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithProducts(): Flow<List<SaleWithProducts>>

    @Query("SELECT * FROM sales ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesPaged(limit: Int, offset: Int): List<Sale>

    @Query("SELECT * FROM sales_products_cross_ref LIMIT :limit OFFSET :offset")
    suspend fun getSaleProductCrossRefsPaged(limit: Int, offset: Int): List<SaleProductCrossRef>

    @Query("""
        SELECT P.name, SUM(SP.quantity) as totalSold
        FROM sales_products_cross_ref AS SP
        JOIN products AS P ON SP.productId = P.id
        GROUP BY P.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(limit: Int): Flow<List<TopSellingProduct>>
}

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getClientsPaged(limit: Int, offset: Int): List<Client>

    @Query("SELECT * FROM clients WHERE debtAmount > 0 ORDER BY name ASC")
    fun getDebtClientsPagedSource(): PagingSource<Int, Client>

    @Query("SELECT SUM(debtAmount) FROM clients")
    fun getTotalDebt(): Flow<Double>
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaymentsPaged(limit: Int, offset: Int): List<Payment>
}

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: Pet)

    @Update
    suspend fun update(pet: Pet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<Pet>)

    @androidx.room.Transaction // <-- CORREGIDO
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>>

    @Query("SELECT * FROM pets ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getPetsPaged(limit: Int, offset: Int): List<Pet>
}

@Dao
interface TreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: Treatment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treatments: List<Treatment>)

    @Query("SELECT * FROM treatments WHERE petIdFk = :petId ORDER BY treatmentDate DESC")
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate IS NOT NULL AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    fun getUpcomingTreatments(): Flow<List<Treatment>>

    @Query("UPDATE treatments SET isNextTreatmentCompleted = 1 WHERE treatmentId = :treatmentId")
    suspend fun markAsCompleted(treatmentId: String)

    @Query("SELECT * FROM treatments ORDER BY treatmentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getTreatmentsPaged(limit: Int, offset: Int): List<Treatment>
}

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: Appointment)

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @androidx.room.Transaction // <-- CORREGIDO
    @Query("SELECT * FROM appointments WHERE appointmentDate >= :startDate AND appointmentDate < :endDate ORDER BY appointmentDate ASC")
    fun getAppointmentsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<Appointment>)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT * FROM appointments ORDER BY appointmentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getAppointmentsPaged(limit: Int, offset: Int): List<Appointment>
}
