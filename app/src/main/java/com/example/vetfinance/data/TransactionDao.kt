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

// --- CLASES DE RELACIÓN ---

/**
 * Representa una cita con todos sus detalles, incluyendo la información de la mascota y el cliente.
 * Room utiliza esta clase para unir las tablas de citas, mascotas y clientes.
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
 * Representa el resultado de una consulta de agregación para encontrar los productos más vendidos.
 * @property name El nombre del producto.
 * @property totalSold La cantidad total vendida de ese producto.
 */
data class TopSellingProduct(val name: String, val totalSold: Int)


// --- DAOs (Data Access Objects) ---

/**
 * DAO para la entidad [Transaction].
 * Actualmente no se utiliza activamente, pero está disponible para futuras funcionalidades de contabilidad.
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<Transaction>
}

/**
 * DAO para la entidad [Product]. Gestiona el acceso a los productos y servicios.
 */
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): Product?

    /** Proporciona una fuente de datos paginada para el inventario, con soporte para filtrado. */
    @Query("SELECT * FROM products WHERE (:filterType = 'Todos') OR (:filterType = 'Productos' AND isService = 0) OR (:filterType = 'Servicios' AND isService = 1) ORDER BY name ASC")
    fun getProductsPagedSource(filterType: String): PagingSource<Int, Product>

    /** Calcula el valor total del inventario sumando (precio * stock) para todos los productos físicos. */
    @Query("SELECT SUM(price * stock) FROM products WHERE isService = 0")
    fun getTotalInventoryValue(): Flow<Double?>

    //**Funcion de borrar producto
    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsPaged(limit: Int, offset: Int): List<Product>
}

/**
 * DAO para las entidades [Sale] y [SaleProductCrossRef]. Gestiona las ventas.
 */
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


    /** Obtiene todas las ventas con su lista de productos asociados. La anotación @Transaction asegura la atomicidad. */
    @androidx.room.Transaction
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithProducts(): Flow<List<SaleWithProducts>>

    @Query("SELECT * FROM sales ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesPaged(limit: Int, offset: Int): List<Sale>

    @Query("SELECT * FROM sales_products_cross_ref LIMIT :limit OFFSET :offset")
    suspend fun getSaleProductCrossRefsPaged(limit: Int, offset: Int): List<SaleProductCrossRef>

    /**
     * Calcula los productos más vendidos dentro de un rango de fechas.
     */
    @Query("""
        SELECT p.name, SUM(sp.quantity) as totalSold
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.id
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>
}

/**
 * DAO para la entidad [Client]. Gestiona el acceso a los datos de los clientes.
 */
@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    /** Actualiza el monto de la deuda para un cliente específico. */
    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getClientsPaged(limit: Int, offset: Int): List<Client>

    /** Proporciona una fuente de datos paginada para clientes con deuda, con soporte para búsqueda por nombre. */
    @Query("SELECT * FROM clients WHERE debtAmount > 0 AND name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun getDebtClientsPagedSource(searchQuery: String): PagingSource<Int, Client>

    /** Calcula la suma total de la deuda de todos los clientes. */
    @Query("SELECT SUM(debtAmount) FROM clients")
    fun getTotalDebt(): Flow<Double?>
}

/**
 * DAO para la entidad [Payment]. Gestiona el acceso a los datos de los pagos.
 */
@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    /** Obtiene todos los pagos de un cliente específico, ordenados por fecha descendente. */
    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaymentsPaged(limit: Int, offset: Int): List<Payment>
}

/**
 * DAO para la entidad [Pet]. Gestiona el acceso a los datos de las mascotas.
 */
@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: Pet)

    @Update
    suspend fun update(pet: Pet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<Pet>)

    /** Obtiene todas las mascotas con la información de su dueño. La anotación @Transaction asegura la atomicidad. */
    @androidx.room.Transaction
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>>

    @Query("SELECT * FROM pets ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getPetsPaged(limit: Int, offset: Int): List<Pet>
}

/**
 * DAO para la entidad [Treatment]. Gestiona el acceso a los datos de los tratamientos.
 */
@Dao
interface TreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: Treatment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treatments: List<Treatment>)

    /** Obtiene el historial de tratamientos para una mascota específica. */
    @Query("SELECT * FROM treatments WHERE petIdFk = :petId ORDER BY treatmentDate DESC")
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>>

    /** Obtiene los tratamientos futuros que aún no han sido marcados como completados. */
    @Query("SELECT * FROM treatments WHERE nextTreatmentDate IS NOT NULL AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    fun getUpcomingTreatments(): Flow<List<Treatment>>

    /** Marca un tratamiento específico como completado. */
    @Query("UPDATE treatments SET isNextTreatmentCompleted = 1 WHERE treatmentId = :treatmentId")
    suspend fun markAsCompleted(treatmentId: String)

    @Query("SELECT * FROM treatments ORDER BY treatmentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getTreatmentsPaged(limit: Int, offset: Int): List<Treatment>
}

/**
 * DAO para la entidad [Appointment]. Gestiona el acceso a los datos de las citas.
 */
@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: Appointment)

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    /** Obtiene todas las citas para un rango de fechas con los detalles de mascota y cliente. */
    @androidx.room.Transaction
    @Query("SELECT * FROM appointments WHERE appointmentDate >= :startDate AND appointmentDate < :endDate ORDER BY appointmentDate ASC")
    fun getAppointmentsForDateRange(startDate: Long, endDate: Long): Flow<List<AppointmentWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<Appointment>)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT * FROM appointments ORDER BY appointmentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getAppointmentsPaged(limit: Int, offset: Int): List<Appointment>
}
