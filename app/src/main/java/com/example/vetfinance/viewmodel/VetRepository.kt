package com.example.vetfinance.viewmodel

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.room.withTransaction
import com.example.vetfinance.data.AppDatabase
import com.example.vetfinance.data.BackupValidationException
import com.example.vetfinance.data.Client
import com.example.vetfinance.data.ClientDao
import com.example.vetfinance.data.Payment
import com.example.vetfinance.data.PaymentDao
import com.example.vetfinance.data.Pet
import com.example.vetfinance.data.PetDao
import com.example.vetfinance.data.PetWithOwner
import com.example.vetfinance.data.Product
import com.example.vetfinance.data.ProductDao
import com.example.vetfinance.data.Sale
import com.example.vetfinance.data.SaleDao
import com.example.vetfinance.data.SaleProductCrossRef
import com.example.vetfinance.data.SaleWithProducts
import com.example.vetfinance.data.Transaction
import com.example.vetfinance.data.TransactionDao
import com.example.vetfinance.data.Treatment
import com.example.vetfinance.data.TreatmentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private data class ParsedBackupData(
    val clients: List<Client>,
    val products: List<Product>,
    val pets: List<Pet>,
    val treatments: List<Treatment>,
    val sales: List<Sale>,
    val transactions: List<Transaction>,
    val payments: List<Payment>,
    val saleProductCrossRefs: List<SaleProductCrossRef>
)


@Singleton
class VetRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val transactionDao: TransactionDao,
    private val clientDao: ClientDao,
    private val paymentDao: PaymentDao,
    private val petDao: PetDao,
    private val treatmentDao: TreatmentDao
) {

    // --- Métodos de Lectura ---
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getAllSales(): Flow<List<SaleWithProducts>> = saleDao.getAllSalesWithProducts()
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>> = paymentDao.getPaymentsForClient(clientId)
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>> = petDao.getAllPetsWithOwners()
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>> = treatmentDao.getTreatmentsForPet(petId)
    fun getUpcomingTreatments(): Flow<List<Treatment>> = treatmentDao.getUpcomingTreatments()
    fun getAllTreatments(): Flow<List<Treatment>> = treatmentDao.getAllTreatments()
    suspend fun productExists(name: String): Boolean = productDao.productExists(name)

    // --- Métodos de Escritura ---
    suspend fun insertProduct(product: Product) { productDao.insert(product) }
    suspend fun updateProduct(product: Product) { productDao.update(product) }
    suspend fun insertTransaction(transaction: Transaction) { transactionDao.insert(transaction) }
    suspend fun deleteTransaction(transaction: Transaction) { transactionDao.delete(transaction) }
    suspend fun insertClient(client: Client) { clientDao.insert(client) }
    suspend fun updateClient(client: Client) { clientDao.update(client) }
    suspend fun insertPet(pet: Pet) = petDao.insert(pet)
    suspend fun insertTreatment(treatment: Treatment) = treatmentDao.insert(treatment)
    suspend fun markTreatmentAsCompleted(treatmentId: String) = treatmentDao.markAsCompleted(treatmentId)

    suspend fun makePayment(client: Client, amount: Double) {
        val payment = Payment(clientIdFk = client.clientId, amountPaid = amount)
        paymentDao.insert(payment)
        val newDebt = client.debtAmount - amount
        clientDao.updateDebt(client.clientId, if (newDebt < 0) 0.0 else newDebt)
    }

    suspend fun insertSale(sale: Sale, items: Map<Product, Int>) {
        db.withTransaction {
            saleDao.insertSale(sale)
            items.forEach { (product, quantity) ->
                val crossRef = SaleProductCrossRef(
                    saleId = sale.saleId,
                    productId = product.id,
                    quantity = quantity,
                    priceAtTimeOfSale = product.price
                )
                saleDao.insertSaleProductCrossRef(crossRef)
                if (!product.isService) {
                    val updatedStock = product.stock - quantity
                    updateProduct(product.copy(stock = updatedStock))
                }
            }
        }
    }

    // --- LÓGICA DE IMPORTACIÓN Y EXPORTACIÓN ---

    suspend fun exportarDatosCompletos(): Map<String, String> = withContext(Dispatchers.IO) {
        val csvMap = mutableMapOf<String, String>()

        // Exportar Clientes
        csvMap["clients.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("clientId", "name", "phone", "debtAmount").build()
            CSVPrinter(sw, format).use { printer ->
                clientDao.getAllClients().first().forEach {
                    printer.printRecord(it.clientId, it.name, it.phone ?: "", it.debtAmount)
                }
            }
            sw.toString()
        }
        // Exportar Productos
        csvMap["products.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("id", "name", "price", "stock", "isService").build()
            CSVPrinter(sw, format).use { printer ->
                productDao.getAllProducts().first().forEach {
                    printer.printRecord(it.id, it.name, it.price, it.stock, it.isService)
                }
            }
            sw.toString()
        }
        // Exportar Mascotas
        csvMap["pets.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("petId", "name", "ownerIdFk").build()
            CSVPrinter(sw, format).use { printer ->
                petDao.getAllPetsWithOwners().first().forEach {
                    printer.printRecord(it.pet.petId, it.pet.name, it.pet.ownerIdFk)
                }
            }
            sw.toString()
        }
        // Exportar Tratamientos
        csvMap["treatments.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("treatmentId", "petIdFk", "treatmentDate", "description", "nextTreatmentDate", "isNextTreatmentCompleted").build()
            CSVPrinter(sw, format).use { printer ->
                treatmentDao.getAllTreatments().first().forEach {
                    printer.printRecord(it.treatmentId, it.petIdFk, it.treatmentDate, it.description, it.nextTreatmentDate ?: "", it.isNextTreatmentCompleted)
                }
            }
            sw.toString()
        }
        // Exportar Ventas
        csvMap["sales.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("saleId", "clientIdFk", "date", "totalAmount").build()
            CSVPrinter(sw, format).use { printer ->
                saleDao.getAllSalesWithProducts().first().forEach {
                    printer.printRecord(it.sale.saleId, it.sale.clientIdFk, it.sale.date, it.sale.totalAmount)
                }
            }
            sw.toString()
        }
        // Exportar Transacciones
        csvMap["transactions.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("transactionId", "saleIdFk", "date", "type", "amount", "description").build()
            CSVPrinter(sw, format).use { printer ->
                transactionDao.getAllTransactions().first().forEach {
                    printer.printRecord(it.transactionId, it.saleIdFk ?: "", it.date, it.type, it.amount, it.description ?: "")
                }
            }
            sw.toString()
        }
        // Exportar Pagos
        csvMap["payments.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("paymentId", "clientIdFk", "paymentDate", "amountPaid").build()
            CSVPrinter(sw, format).use { printer ->
                paymentDao.getAllPayments().first().forEach {
                    printer.printRecord(it.paymentId, it.clientIdFk, it.paymentDate, it.amountPaid)
                }
            }
            sw.toString()
        }
        // Exportar Detalles de Venta (Tabla de Unión)
        csvMap["sale_product_cross_refs.csv"] = StringWriter().use { sw ->
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("saleId", "productId", "quantity", "priceAtTimeOfSale").build()
            CSVPrinter(sw, format).use { printer ->
                saleDao.getAllSaleProductCrossRefs().first().forEach {
                    printer.printRecord(it.saleId, it.productId, it.quantity, it.priceAtTimeOfSale)
                }
            }
            sw.toString()
        }

        return@withContext csvMap
    }

    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            val archivosDelZip = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    generateSequence { zis.nextEntry }.forEach { entry ->
                        archivosDelZip[entry.name] = zis.bufferedReader().readText()
                    }
                }
            }
            if (archivosDelZip.isEmpty()) return@withContext "Error: El archivo ZIP está vacío o no es válido."

            val backupData = validateAndParseBackupData(archivosDelZip)

            // 👇 Se llama a la nueva función de fusión
            performMergeImport(backupData)

            return@withContext "Fusión de datos completada con éxito."

        } catch (e: BackupValidationException) {
            e.printStackTrace()
            return@withContext e.message ?: "Error de validación de copia de seguridad desconocido."
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return@withContext "Error de formato: Uno de los archivos CSV contiene un número inválido."
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return@withContext "Error de formato: Revise los encabezados de los archivos CSV. ${e.message ?: "Sin detalles."}"
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
            return@withContext "Error de integridad: Los datos son inconsistentes. ${e.message ?: "Sin detalles."}"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error inesperado al importar: ${e.message ?: "Sin detalles."}"
        }
    }

    @Throws(BackupValidationException::class)
    private fun validateAndParseBackupData(archivos: Map<String, String>): ParsedBackupData {
        // Parseo de todas las entidades
        val clients = parseCSV(archivos["clients.csv"], ::parseClient)
        val products = parseCSV(archivos["products.csv"], ::parseProduct)
        val pets = parseCSV(archivos["pets.csv"], ::parsePet)
        val treatments = parseCSV(archivos["treatments.csv"], ::parseTreatment)
        val sales = parseCSV(archivos["sales.csv"], ::parseSale)
        val transactions = parseCSV(archivos["transactions.csv"], ::parseTransaction)
        val payments = parseCSV(archivos["payments.csv"], ::parsePayment)
        val saleProductCrossRefs = parseCSV(archivos["sale_product_cross_refs.csv"], ::parseSaleProductCrossRef)

        // Sets de IDs para validaciones rápidas
        val clientIds = clients.map { it.clientId }.toSet()
        val petIds = pets.map { it.petId }.toSet()
        val productIds = products.map { it.id }.toSet()
        val saleIds = sales.map { it.saleId }.toSet()

        // --- Validaciones de integridad (claves foráneas) ---

        pets.forEach { pet ->
            if (pet.ownerIdFk !in clientIds)
                throw BackupValidationException("Conflicto en 'pets.csv': La mascota '${pet.name}' (ID: ${pet.petId}) se refiere a un dueño con ID ${pet.ownerIdFk} que no existe.")
        }

        treatments.forEach { treatment ->
            if (treatment.petIdFk !in petIds)
                throw BackupValidationException("Conflicto en 'treatments.csv': El tratamiento (ID: ${treatment.treatmentId}) se refiere a una mascota con ID ${treatment.petIdFk} que no existe.")
        }

        sales.forEach { sale ->
            if (sale.clientIdFk !in clientIds)
                throw BackupValidationException("Conflicto en 'sales.csv': La venta (ID: ${sale.saleId}) se refiere a un cliente con ID ${sale.clientIdFk} que no existe.")
        }

        payments.forEach { payment ->
            if (payment.clientIdFk !in clientIds)
                throw BackupValidationException("Conflicto en 'payments.csv': El pago (ID: ${payment.paymentId}) se refiere a un cliente con ID ${payment.clientIdFk} que no existe.")
        }

        saleProductCrossRefs.forEach { crossRef ->
            if (crossRef.saleId !in saleIds)
                throw BackupValidationException("Conflicto en 'sale_product_cross_refs.csv': Se encontró un producto asociado a una venta con ID ${crossRef.saleId} que no existe.")
            if (crossRef.productId !in productIds)
                throw BackupValidationException("Conflicto en 'sale_product_cross_refs.csv': Se encontró una venta asociada a un producto con ID ${crossRef.productId} que no existe.")
        }

        return ParsedBackupData(clients, products, pets, treatments, sales, transactions, payments, saleProductCrossRefs)
    }

    // 👇 6. ESTA ES LA NUEVA FUNCIÓN DE FUSIÓN
    private suspend fun performMergeImport(data: ParsedBackupData) {
        db.withTransaction {
            // --- YA NO SE BORRAN LOS DATOS ---
            // El OnConflictStrategy.REPLACE se encargará de la lógica.

            // Inserción/Actualización de los datos en orden de dependencia
            if (data.clients.isNotEmpty()) clientDao.insertAll(data.clients)
            if (data.products.isNotEmpty()) productDao.insertAll(data.products)

            // Las entidades con claves foráneas se insertan después de sus "padres"
            if (data.pets.isNotEmpty()) petDao.insertAll(data.pets)
            if (data.sales.isNotEmpty()) saleDao.insertAllSales(data.sales)
            if (data.treatments.isNotEmpty()) treatmentDao.insertAll(data.treatments)

            // El resto de datos
            if (data.transactions.isNotEmpty()) transactionDao.insertAll(data.transactions)
            if (data.payments.isNotEmpty()) paymentDao.insertAll(data.payments)
            if (data.saleProductCrossRefs.isNotEmpty()) saleDao.insertAllSaleProductCrossRefs(data.saleProductCrossRefs)
        }
    }

    private inline fun <T> parseCSV(content: String?, parser: (org.apache.commons.csv.CSVRecord) -> T): List<T> {
        if (content.isNullOrBlank()) return emptyList()
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build()
        return CSVParser.parse(content, format).map(parser)
    }

    // --- Funciones de parseo ---
    private fun parseClient(record: org.apache.commons.csv.CSVRecord): Client = Client(
        clientId = record.get("clientId"),
        name = record.get("name"),
        phone = record.get("phone").ifEmpty { null },
        debtAmount = record.get("debtAmount").toDouble()
    )

    private fun parseProduct(record: org.apache.commons.csv.CSVRecord): Product = Product(
        id = record.get("id"),
        name = record.get("name"),
        price = record.get("price").toDouble(),
        stock = record.get("stock").toInt(),
        isService = record.get("isService").toBoolean()
    )

    private fun parsePet(record: org.apache.commons.csv.CSVRecord): Pet = Pet(
        petId = record.get("petId"),
        name = record.get("name"),
        ownerIdFk = record.get("ownerIdFk")
    )

    private fun parseTreatment(record: org.apache.commons.csv.CSVRecord): Treatment = Treatment(
        treatmentId = record.get("treatmentId"),
        petIdFk = record.get("petIdFk"),
        description = record.get("description"),
        treatmentDate = record.get("treatmentDate").toLong(),
        nextTreatmentDate = record.get("nextTreatmentDate").toLongOrNull(),
        isNextTreatmentCompleted = record.get("isNextTreatmentCompleted").toBoolean()
    )

    private fun parseSale(record: org.apache.commons.csv.CSVRecord): Sale = Sale(
        saleId = record.get("saleId"),
        clientIdFk = record.get("clientIdFk"),
        date = record.get("date").toLong(),
        totalAmount = record.get("totalAmount").toDouble()
    )

    private fun parseTransaction(record: org.apache.commons.csv.CSVRecord): Transaction =
        Transaction(
            transactionId = record.get("transactionId"),
            saleIdFk = record.get("saleIdFk").ifEmpty { null },
            date = record.get("date").toLong(),
            type = record.get("type"),
            amount = record.get("amount").toDouble(),
            description = record.get("description").ifEmpty { null }
        )

    private fun parsePayment(record: org.apache.commons.csv.CSVRecord): Payment = Payment(
        paymentId = record.get("paymentId"),
        clientIdFk = record.get("clientIdFk"),
        amountPaid = record.get("amountPaid").toDouble(),
        paymentDate = record.get("paymentDate").toLong()
    )

    private fun parseSaleProductCrossRef(record: org.apache.commons.csv.CSVRecord): SaleProductCrossRef =
        SaleProductCrossRef(
            saleId = record.get("saleId"),
            productId = record.get("productId"),
            quantity = record.get("quantity").toInt(),
            priceAtTimeOfSale = record.get("priceAtTimeOfSale").toDouble()
        )
}
