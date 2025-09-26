package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.vetfinance.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class interna para manejar los datos del backup durante la importación/exportación.
 */
private data class ParsedBackupData(
    val clients: List<Client>,
    val products: List<Product>,
    val pets: List<Pet>,
    val treatments: List<Treatment>,
    val sales: List<Sale>,
    val transactions: List<Transaction>,
    val payments: List<Payment>,
    val saleProductCrossRefs: List<SaleProductCrossRef>,
    val appointments: List<Appointment>
)

/**
 * Repositorio central que maneja todas las operaciones de datos de la aplicación.
 *
 * Actúa como una única fuente de verdad (Single Source of Truth) para la UI, abstrayendo los
 * orígenes de datos (en este caso, la base de datos de Room). Proporciona métodos limpios
 * para que el `VetViewModel` acceda y manipule los datos sin conocer los detalles de la
 * implementación de la base de datos.
 *
 * @property db Instancia de la base de datos Room de la aplicación.
 * @property productDao DAO para acceder a los datos de los productos.
 * @property saleDao DAO para acceder a los datos de las ventas.
 * @property clientDao DAO para acceder a los datos de los clientes.
 * @property paymentDao DAO para acceder a los datos de los pagos.
 * @property petDao DAO para acceder a los datos de las mascotas.
 * @property treatmentDao DAO para acceder a los datos de los tratamientos.
 * @property appointmentDao DAO para acceder a los datos de las citas.
 */
@Singleton
class VetRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val transactionDao: TransactionDao,
    private val clientDao: ClientDao,
    private val paymentDao: PaymentDao,
    private val petDao: PetDao,
    private val treatmentDao: TreatmentDao,
    private val appointmentDao: AppointmentDao
) {

    /** Tamaño del lote para operaciones de exportación para no sobrecargar la memoria. */
    private val batchSize = 500

    //** Funciones de borrado
    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }

    suspend fun deleteClient(client: Client) {
        clientDao.delete(client)
    }

    suspend fun deleteSale(saleWithProducts: SaleWithProducts) {
        val sale = saleWithProducts.sale
        db.withTransaction {
            val saleDetails = saleDao.getSaleDetailsBySaleId(sale.saleId)
            for (detail in saleDetails) {
                val product = productDao.getProductById(detail.productId)
                if (product != null && !product.isService) {
                    val newStock = product.stock + detail.quantity
                    productDao.update(product.copy(stock = newStock))
                }
            }
            saleDao.deleteSaleProductCrossRefs(sale.saleId)
            saleDao.deleteSale(sale)
        }
    }


    // --- OPERACIONES DE PAGINACIÓN ---

    /** Obtiene un flujo paginado de productos, opcionalmente filtrado por tipo. */
    fun getProductsPaginated(filterType: String): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { productDao.getProductsPagedSource(filterType) }
        ).flow
    }

    /** Obtiene un flujo paginado de clientes con deuda, filtrado por una consulta de búsqueda. */
    fun getDebtClientsPaginated(searchQuery: String): Flow<PagingData<Client>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { clientDao.getDebtClientsPagedSource(searchQuery) }
        ).flow
    }

    // --- OPERACIONES CON FECHAS (CITAS) ---

    /** Obtiene todas las citas con sus detalles para una fecha específica. */
    fun getAppointmentsForDate(date: LocalDate): Flow<List<AppointmentWithDetails>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return appointmentDao.getAppointmentsForDateRange(startOfDay, endOfDay)
    }

    // --- OPERACIONES DE REPORTES ---

    /** Obtiene los productos más vendidos, limitados por la cantidad especificada. */
    fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int): Flow<List<TopSellingProduct>> = saleDao.getTopSellingProducts(startDate, endDate, limit)

    /** Obtiene la suma total de la deuda de todos los clientes. */
    fun getTotalDebt(): Flow<Double?> = clientDao.getTotalDebt()

    /** Calcula el valor total del inventario (precio * stock) para productos físicos. */
    fun getTotalInventoryValue(): Flow<Double?> = productDao.getTotalInventoryValue()

    // --- OPERACIONES DE LECTURA (FLUJOS DE DATOS) ---

    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getAllSales(): Flow<List<SaleWithProducts>> = saleDao.getAllSalesWithProducts()
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>> = paymentDao.getPaymentsForClient(clientId)
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>> = petDao.getAllPetsWithOwners()
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>> = treatmentDao.getTreatmentsForPet(petId)
    fun getUpcomingTreatments(): Flow<List<Treatment>> = treatmentDao.getUpcomingTreatments()
    suspend fun getSaleDetailsBySaleId(saleId: String): List<SaleProductCrossRef> = saleDao.getSaleDetailsBySaleId(saleId)
    suspend fun getProductById(productId: String): Product? = productDao.getProductById(productId)

    // --- OPERACIONES DE ESCRITURA (SUSPEND) ---

    suspend fun insertProduct(product: Product) = productDao.insertAll(listOf(product))
    suspend fun updateProduct(product: Product) = productDao.update(product)
    suspend fun insertClient(client: Client) = clientDao.insertAll(listOf(client))
    suspend fun updateClient(client: Client) = clientDao.update(client)
    suspend fun insertPet(pet: Pet) = petDao.insert(pet)
    suspend fun updatePet(pet: Pet) = petDao.update(pet)
    suspend fun insertTreatment(treatment: Treatment) = treatmentDao.insert(treatment)
    suspend fun insertAppointment(appointment: Appointment) = appointmentDao.insert(appointment)
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.update(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.delete(appointment)

    /** Marca un tratamiento como completado y crea el siguiente si es necesario. */
    suspend fun markTreatmentAsCompleted(treatmentId: String) = treatmentDao.markAsCompleted(treatmentId)

    /**
     * Registra un pago para un cliente y actualiza su deuda total.
     * La operación se asegura de que la deuda nunca sea negativa.
     */
    suspend fun makePayment(client: Client, amount: Double) {
        val payment = Payment(clientIdFk = client.clientId, amountPaid = amount)
        paymentDao.insert(payment)
        val newDebt = client.debtAmount - amount
        clientDao.updateDebt(client.clientId, if (newDebt < 0) 0.0 else newDebt)
    }

    /**
     * Inserta una nueva venta y sus productos asociados en una única transacción.
     * También actualiza el stock de los productos vendidos.
     */
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
                // Solo se descuenta el stock si no es un servicio.
                if (!product.isService) {
                    val updatedStock = product.stock - quantity
                    updateProduct(product.copy(stock = updatedStock))
                }
            }
        }
    }

    // --- LÓGICA DE IMPORTACIÓN Y EXPORTACIÓN ---

    /**
     * Exporta todas las tablas de la base de datos a un mapa de [NombreDeArchivo, ContenidoCSV].
     * Se ejecuta en el despachador de IO para no bloquear el hilo principal.
     */
    suspend fun exportarDatosCompletos(): Map<String, String> = withContext(Dispatchers.IO) {
        val csvMap = mutableMapOf<String, String>()

        /**
         * Función genérica para exportar una tabla en lotes (batches) para evitar
         * problemas de memoria con bases de datos grandes.
         */
        suspend fun <T> exportBatch(
            daoMethod: suspend (limit: Int, offset: Int) -> List<T>,
            fileName: String,
            headers: Array<String>,
            recordMapper: (T, CSVPrinter) -> Unit
        ) {
            val sw = StringWriter()
            val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader(*headers).build()
            CSVPrinter(sw, format).use { printer ->
                var offset = 0
                var batch: List<T>
                do {
                    batch = daoMethod(batchSize, offset)
                    batch.forEach { recordMapper(it, printer) }
                    offset += batchSize
                } while (batch.isNotEmpty())
            }
            csvMap[fileName] = sw.toString()
        }

        // Se exporta cada tabla a su respectivo archivo CSV.
        exportBatch(clientDao::getClientsPaged, "clients.csv", arrayOf("clientId", "name", "phone", "debtAmount")) { it, p -> p.printRecord(it.clientId, it.name, it.phone ?: "", it.debtAmount) }
        exportBatch(productDao::getProductsPaged, "products.csv", arrayOf("id", "name", "price", "stock", "cost", "isService")) { it, p -> p.printRecord(it.id, it.name, it.price, it.stock, it.cost, it.isService) }
        exportBatch(petDao::getPetsPaged, "pets.csv", arrayOf("petId", "name", "ownerIdFk", "birthDate", "breed", "allergies")) { it, p -> p.printRecord(it.petId, it.name, it.ownerIdFk, it.birthDate ?: "", it.breed ?: "", it.allergies ?: "") }
        exportBatch(treatmentDao::getTreatmentsPaged, "treatments.csv", arrayOf("treatmentId", "petIdFk", "treatmentDate", "description", "weight", "temperature", "symptoms", "diagnosis", "treatmentPlan", "nextTreatmentDate", "isNextTreatmentCompleted")) { it, p -> p.printRecord(it.treatmentId, it.petIdFk, it.treatmentDate, it.description, it.weight ?: "", it.temperature ?: "", it.symptoms ?: "", it.diagnosis ?: "", it.treatmentPlan ?: "", it.nextTreatmentDate ?: "", it.isNextTreatmentCompleted) }
        exportBatch(saleDao::getSalesPaged, "sales.csv", arrayOf("saleId", "clientIdFk", "date", "totalAmount")) { it, p -> p.printRecord(it.saleId, it.clientIdFk, it.date, it.totalAmount) }
        exportBatch(transactionDao::getTransactionsPaged, "transactions.csv", arrayOf("transactionId", "saleIdFk", "date", "type", "amount", "description")) { it, p -> p.printRecord(it.transactionId, it.saleIdFk ?: "", it.date, it.type, it.amount, it.description ?: "") }
        exportBatch(paymentDao::getPaymentsPaged, "payments.csv", arrayOf("paymentId", "clientIdFk", "paymentDate", "amountPaid")) { it, p -> p.printRecord(it.paymentId, it.clientIdFk, it.paymentDate, it.amountPaid) }
        exportBatch(saleDao::getSaleProductCrossRefsPaged, "sale_product_cross_refs.csv", arrayOf("saleId", "productId", "quantity", "priceAtTimeOfSale")) { it, p -> p.printRecord(it.saleId, it.productId, it.quantity, it.priceAtTimeOfSale) }
        exportBatch(appointmentDao::getAppointmentsPaged, "appointments.csv", arrayOf("appointmentId", "clientIdFk", "petIdFk", "appointmentDate", "description", "isCompleted")) { it, p -> p.printRecord(it.appointmentId, it.clientIdFk, it.petIdFk, it.appointmentDate, it.description, it.isCompleted) }

        return@withContext csvMap
    }

    /**
     * Importa datos desde un archivo ZIP, valida su integridad y los fusiona con la base de datos local.
     * @param uri El URI del archivo ZIP seleccionado por el usuario.
     * @param context El contexto de la aplicación, necesario para acceder al ContentResolver.
     * @return Un mensaje de éxito o error.
     */
    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            // 1. Descomprimir el contenido del ZIP en memoria.
            val archivosDelZip = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    generateSequence { zis.nextEntry }.forEach { entry ->
                        archivosDelZip[entry.name] = zis.bufferedReader().readText()
                    }
                }
            }
            if (archivosDelZip.isEmpty()) return@withContext "Error: El archivo ZIP está vacío o no es válido."

            // 2. Validar y parsear los datos CSV.
            val backupData = validateAndParseBackupData(archivosDelZip)

            // 3. Insertar los datos validados en la base de datos en una única transacción.
            performMergeImport(backupData)

            return@withContext "Fusión de datos completada con éxito."
        } catch (e: BackupValidationException) {
            e.printStackTrace()
            return@withContext e.message ?: "Error de validación desconocido."
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error inesperado al importar: ${e.message ?: "Sin detalles."}"
        }
    }

    /**
     * Valida la integridad referencial de los datos del backup antes de importarlos.
     * Lanza una [BackupValidationException] si se encuentra una inconsistencia.
     */
    @Throws(BackupValidationException::class)
    private fun validateAndParseBackupData(archivos: Map<String, String>): ParsedBackupData {
        // Se parsean todas las entidades desde sus respectivos CSVs.
        val clients = parseCSV(archivos["clients.csv"], ::parseClient)
        val products = parseCSV(archivos["products.csv"], ::parseProduct)
        val pets = parseCSV(archivos["pets.csv"], ::parsePet)
        val treatments = parseCSV(archivos["treatments.csv"], ::parseTreatment)
        val sales = parseCSV(archivos["sales.csv"], ::parseSale)
        val transactions = parseCSV(archivos["transactions.csv"], ::parseTransaction)
        val payments = parseCSV(archivos["payments.csv"], ::parsePayment)
        val saleProductCrossRefs = parseCSV(archivos["sale_product_cross_refs.csv"], ::parseSaleProductCrossRef)
        val appointments = parseCSV(archivos["appointments.csv"], ::parseAppointment)

        // Se crean conjuntos de IDs para una validación eficiente.
        val clientIds = clients.map { it.clientId }.toSet()
        val petIds = pets.map { it.petId }.toSet()
        val productIds = products.map { it.id }.toSet()
        val saleIds = sales.map { it.saleId }.toSet()

        // Se comprueba que todas las claves foráneas apunten a entidades existentes.
        pets.forEach { if (it.ownerIdFk !in clientIds) throw BackupValidationException("Mascota con dueño inválido: ${it.name}") }
        treatments.forEach { if (it.petIdFk !in petIds) throw BackupValidationException("Tratamiento con mascota inválida: ${it.treatmentId}") }
        sales.forEach { if (it.clientIdFk !in clientIds) throw BackupValidationException("Venta con cliente inválido: ${it.saleId}") }
        payments.forEach { if (it.clientIdFk !in clientIds) throw BackupValidationException("Pago con cliente inválido: ${it.paymentId}") }
        saleProductCrossRefs.forEach {
            if (it.saleId !in saleIds) throw BackupValidationException("Detalle de venta con ID de venta inválido: ${it.saleId}")
            if (it.productId !in productIds) throw BackupValidationException("Detalle de venta con ID de producto inválido: ${it.productId}")
        }
        appointments.forEach {
            if (it.clientIdFk !in clientIds) throw BackupValidationException("Cita con cliente inválido: ${it.appointmentId}")
            if (it.petIdFk !in petIds) throw BackupValidationException("Cita con mascota inválida: ${it.appointmentId}")
        }

        return ParsedBackupData(clients, products, pets, treatments, sales, transactions, payments, saleProductCrossRefs, appointments)
    }

    /** Inserta todos los datos del backup en la base de datos dentro de una transacción. */
    private suspend fun performMergeImport(data: ParsedBackupData) {
        db.withTransaction {
            if (data.clients.isNotEmpty()) clientDao.insertAll(data.clients)
            if (data.products.isNotEmpty()) productDao.insertAll(data.products)
            if (data.pets.isNotEmpty()) petDao.insertAll(data.pets)
            if (data.sales.isNotEmpty()) saleDao.insertAllSales(data.sales)
            if (data.treatments.isNotEmpty()) treatmentDao.insertAll(data.treatments)
            if (data.transactions.isNotEmpty()) transactionDao.insertAll(data.transactions)
            if (data.payments.isNotEmpty()) paymentDao.insertAll(data.payments)
            if (data.saleProductCrossRefs.isNotEmpty()) saleDao.insertAllSaleProductCrossRefs(data.saleProductCrossRefs)
            if (data.appointments.isNotEmpty()) appointmentDao.insertAll(data.appointments)
        }
    }

    /** Función de utilidad para parsear un contenido de texto CSV a una lista de objetos. */
    private inline fun <T> parseCSV(content: String?, parser: (org.apache.commons.csv.CSVRecord) -> T): List<T> {
        if (content.isNullOrBlank()) return emptyList()
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build()
        return CSVParser.parse(content, format).map(parser)
    }

    // --- Funciones de Parseo de CSV ---
    private fun parseClient(r: org.apache.commons.csv.CSVRecord) = Client(r["clientId"], r["name"], r["phone"].ifEmpty { null }, r["debtAmount"].toDouble())
    private fun parseProduct(r: org.apache.commons.csv.CSVRecord) = Product(r["id"], r["name"], r["price"].toDouble(), r["stock"].toInt(), r["cost"].toDouble(), r["isService"].toBoolean())
    private fun parsePet(r: org.apache.commons.csv.CSVRecord) = Pet(r["petId"], r["name"], r["ownerIdFk"], r["birthDate"].toLongOrNull(), r["breed"].ifEmpty { null }, r["allergies"].ifEmpty { null })
    private fun parseTreatment(r: org.apache.commons.csv.CSVRecord) = Treatment(r["treatmentId"], r["petIdFk"], r["description"], r["treatmentDate"].toLong(), r["weight"].toDoubleOrNull(), r["temperature"].toDoubleOrNull(), r["symptoms"].ifEmpty { null }, r["diagnosis"].ifEmpty { null }, r["treatmentPlan"].ifEmpty { null }, r["nextTreatmentDate"].toLongOrNull(), r["isNextTreatmentCompleted"].toBoolean())
    private fun parseSale(r: org.apache.commons.csv.CSVRecord) = Sale(r["saleId"], r["clientIdFk"], r["totalAmount"].toDouble(), r["date"].toLong())
    private fun parseTransaction(r: org.apache.commons.csv.CSVRecord) = Transaction(r["transactionId"], r["saleIdFk"].ifEmpty { null }, r["date"].toLong(), r["type"], r["amount"].toDouble(), r["description"].ifEmpty { null })
    private fun parsePayment(r: org.apache.commons.csv.CSVRecord) = Payment(r["paymentId"], r["clientIdFk"], r["amountPaid"].toDouble(), r["paymentDate"].toLong())
    private fun parseSaleProductCrossRef(r: org.apache.commons.csv.CSVRecord) = SaleProductCrossRef(r["saleId"], r["productId"], r["quantity"].toInt(), r["priceAtTimeOfSale"].toDouble())
    private fun parseAppointment(r: org.apache.commons.csv.CSVRecord) = Appointment(r["appointmentId"], r["clientIdFk"], r["petIdFk"], r["appointmentDate"].toLong(), r["description"], r["isCompleted"].toBoolean())
}