package com.example.vetfinance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // Nuevo método para obtener transacciones en lotes
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    fun getTransactionsPaged(limit: Int, offset: Int): List<Transaction>
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Update
    suspend fun update(product: Product)

    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE LOWER(name) = LOWER(:name) LIMIT 1)")
    suspend fun productExists(name: String): Boolean

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Product?

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    // Nuevo método para obtener productos en lotes
    @Query("SELECT * FROM products ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsPaged(limit: Int, offset: Int): List<Product>
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleProductCrossRef(crossRef: SaleProductCrossRef)

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleProductCrossRefs(crossRefs: List<SaleProductCrossRef>)

    @androidx.room.Transaction
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithProducts(): Flow<List<SaleWithProducts>>

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    // 👇 CORRECCIÓN: Nombre de tabla corregido a 'sales_products_cross_ref' (plural)
    @Query("DELETE FROM sales_products_cross_ref")
    suspend fun deleteAllSaleProductCrossRefs()

    @Query("SELECT EXISTS(SELECT 1 FROM sales WHERE date = :date AND totalAmount = :total LIMIT 1)")
    suspend fun saleExists(date: Long, total: Double): Boolean

    // 👇 CORRECCIÓN: Nombre de tabla y tipo de retorno corregidos
    @Query("SELECT * FROM sales_products_cross_ref")
    fun getAllSaleProductCrossRefs(): Flow<List<SaleProductCrossRef>>

    // Nuevo método para obtener ventas en lotes
    @Query("SELECT * FROM sales ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesPaged(limit: Int, offset: Int): List<Sale>

    // Nuevo método para obtener relaciones de ventas en lotes
    @Query("SELECT * FROM sales_products_cross_ref LIMIT :limit OFFSET :offset")
    suspend fun getSaleProductCrossRefsPaged(limit: Int, offset: Int): List<SaleProductCrossRef>
}

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("UPDATE clients SET debtAmount = :newDebtAmount WHERE clientId = :clientId")
    suspend fun updateDebt(clientId: String, newDebtAmount: Double)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Query("SELECT * FROM clients WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Client?

    // Nuevo método para obtener clientes en lotes
    @Query("SELECT * FROM clients ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getClientsPaged(limit: Int, offset: Int): List<Client>
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    @Query("SELECT * FROM payments WHERE clientIdFk = :clientId ORDER BY paymentDate DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()

    @Query("SELECT * FROM payments")
    fun getAllPayments(): Flow<List<Payment>>

    // Nuevo método para obtener pagos en lotes
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaymentsPaged(limit: Int, offset: Int): List<Payment>
}

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: Pet)

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<Pet>)

    @androidx.room.Transaction
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>>

    @Query("DELETE FROM pets")
    suspend fun deleteAllPets()

    @Query("SELECT * FROM pets WHERE name = :name AND ownerIdFk = :ownerId LIMIT 1")
    suspend fun findByNameAndOwner(name: String, ownerId: String): Pet?

    // Nuevo método para obtener mascotas en lotes
    @Query("SELECT * FROM pets ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getPetsPaged(limit: Int, offset: Int): List<Pet>
}

@Dao
interface TreatmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: Treatment)

    @Query("SELECT * FROM treatments ORDER BY treatmentDate DESC")
    fun getAllTreatments(): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE petIdFk = :petId ORDER BY treatmentDate DESC")
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE nextTreatmentDate IS NOT NULL AND isNextTreatmentCompleted = 0 ORDER BY nextTreatmentDate ASC")
    fun getUpcomingTreatments(): Flow<List<Treatment>>

    @Query("UPDATE treatments SET isNextTreatmentCompleted = 1 WHERE treatmentId = :treatmentId")
    suspend fun markAsCompleted(treatmentId: String)

    @Query("DELETE FROM treatments")
    suspend fun deleteAllTreatments()

    // 👇 CORRECCIÓN: Se añadió la anotación para la fusión de datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treatments: List<Treatment>)

    // Nuevo método para obtener tratamientos en lotes
    @Query("SELECT * FROM treatments ORDER BY treatmentDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getTreatmentsPaged(limit: Int, offset: Int): List<Treatment>
}