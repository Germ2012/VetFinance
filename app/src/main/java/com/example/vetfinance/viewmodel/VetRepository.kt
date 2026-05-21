package com.example.vetfinance.viewmodel

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.vetfinance.R
import com.example.vetfinance.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

// Data class definition for backup
private data class ParsedBackupData(
    val clients: List<Client>,
    val products: List<Product>,
    val pets: List<Pet>,
    val treatments: List<Treatment>,
    val sales: List<Sale>,
    val transactions: List<Transaction>,
    val payments: List<Payment>,
    val saleProductCrossRefs: List<SaleProductCrossRef>,
    val appointments: List<Appointment>,
    val suppliers: List<Supplier>
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
    private val treatmentDao: TreatmentDao,
    private val appointmentDao: AppointmentDao,
    private val supplierDao: SupplierDao,
    private val purchaseDao: PurchaseDao,
    private val restockDao: RestockDao,
    private val appointmentLogDao: AppointmentLogDao,
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val MAX_BACKUP_FILE_BYTES = 2_000_000
        const val MAX_BACKUP_TOTAL_BYTES = 10_000_000
        val ALLOWED_BACKUP_FILES = setOf(
            "clients.csv",
            "suppliers.csv",
            "products.csv",
            "pets.csv",
            "treatments.csv",
            "sales.csv",
            "transactions.csv",
            "payments.csv",
            "sale_product_cross_refs.csv",
            "appointments.csv"
        )
    }

    suspend fun getRestockHistoryForDateRange(startDate: Long, endDate: Long): List<RestockHistoryItem> {
        return restockDao.getRestockHistoryForDateRange(startDate, endDate)
    }

    suspend fun deleteProduct(product: Product) {
        db.withTransaction {
            val salesUsingProduct = productDao.countSaleDetailsForProduct(product.productId)
            if (salesUsingProduct > 0) {
                throw IllegalStateException(
                    "No se puede eliminar ${product.name} porque ya forma parte del historial de ventas. " +
                        "Podés dejarlo con stock 0 o editarlo, pero eliminarlo borraría trazabilidad."
                )
            }
            productDao.delete(product)
        }
    }

    suspend fun performInventoryTransfer(containerId: String, containedId: String, amountToTransfer: Double) {
        require(amountToTransfer > 0.0) { "La cantidad a transferir debe ser mayor a cero." }

        db.withTransaction {
            val containerProduct = productDao.getProductById(containerId)
                ?: throw IllegalStateException("Producto contenedor no encontrado.")
            val containedProduct = productDao.getProductById(containedId)
                ?: throw IllegalStateException("Producto contenido no encontrado.")

            if (containerProduct.stock < 1.0) {
                throw IllegalStateException("No hay stock disponible del contenedor ${containerProduct.name}.")
            }

            productDao.update(containerProduct.copy(stock = containerProduct.stock - 1.0))
            productDao.update(containedProduct.copy(stock = containedProduct.stock + amountToTransfer))
        }
    }

    suspend fun deleteClient(client: Client) {
        db.withTransaction {
            val currentClient = clientDao.getClientById(client.clientId) ?: return@withTransaction
            val paymentsCount = clientDao.countPaymentsForClient(client.clientId)
            if (currentClient.debtAmount > 0.0 || paymentsCount > 0) {
                throw IllegalStateException(
                    "No se puede eliminar ${currentClient.name} porque tiene deuda o pagos registrados. " +
                        "Esto protege el historial financiero."
                )
            }
            clientDao.delete(currentClient)
        }
    }

    suspend fun deleteSale(saleWithProducts: SaleWithProducts) {
        val sale = saleWithProducts.sale
        db.withTransaction {
            val saleDetails = saleDao.getSaleDetailsBySaleId(sale.saleId)
            for (detail in saleDetails) {
                val product = productDao.getProductById(detail.productId)
                if (product != null && !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY) {
                    val newStock = product.stock + detail.quantitySold
                    productDao.update(product.copy(stock = newStock))
                }
            }
            saleDao.deleteSaleProductCrossRefs(sale.saleId)
            saleDao.deleteSale(sale)
        }
    }

    // --- Pagination Methods ---
    fun getProductsPaginated(filterType: String): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { productDao.getProductsPagedSource(filterType) }
        ).flow
    }

    fun getDebtClientsPaginated(searchQuery: String): Flow<PagingData<Client>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { clientDao.getDebtClientsPagedSource(searchQuery) }
        ).flow
    }

    // --- Query Methods (Flows and suspend) ---
    fun getAppointmentsForDate(date: LocalDate): Flow<List<AppointmentWithDetails>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return appointmentDao.getAppointmentsForDateRange(startOfDay, endOfDay)
    }

    fun getAppointmentsForDate(startDate: Long, endDate: Long): Flow<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForDateRange(startDate, endDate)
    }

    fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int): Flow<List<TopSellingProduct>> = saleDao.getTopSellingProducts(startDate, endDate, limit)
    fun getTotalDebt(): Flow<Double?> = clientDao.getTotalDebt()
    fun getTotalInventoryValue(): Flow<Double?> = productDao.getTotalInventoryValue()
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getAllSales(): Flow<List<SaleWithProducts>> = saleDao.getAllSalesWithProducts()
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>> = paymentDao.getPaymentsForClient(clientId)
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>> = petDao.getAllPetsWithOwners()
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>> = treatmentDao.getTreatmentsForPet(petId)
    fun getUpcomingTreatments(): Flow<List<Treatment>> = treatmentDao.getUpcomingTreatments()
    suspend fun getUpcomingTreatmentsForRange(startDate: Long, endDate: Long): List<Treatment> = treatmentDao.getUpcomingTreatmentsForRange(startDate, endDate)
    suspend fun getSaleDetailsBySaleId(saleId: String): List<SaleProductCrossRef> = saleDao.getSaleDetailsBySaleId(saleId)
    suspend fun getProductById(productId: String): Product? = productDao.getProductById(productId)

    // --- Insertion and Update Methods (CRUD) ---
    suspend fun insertOrUpdateProduct(product: Product) {
        val existingProduct = productDao.getProductById(product.productId)
        if (existingProduct == null) {
            val productToInsert = product.copy(productId = UUID.randomUUID().toString())
            productDao.insertProduct(productToInsert)
        } else {
            productDao.update(product)
        }
    }

    suspend fun insertClient(client: Client) = clientDao.insertAll(listOf(client))
    suspend fun updateClient(client: Client) = clientDao.update(client)

    suspend fun insertSupplier(supplier: Supplier) = supplierDao.insert(supplier)
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.update(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.delete(supplier)

    suspend fun insertPet(pet: Pet) = petDao.insert(pet)
    suspend fun updatePet(pet: Pet) = petDao.update(pet)

    suspend fun insertTreatment(treatment: Treatment) = treatmentDao.insert(treatment)
    suspend fun updateTreatment(treatment: Treatment) = treatmentDao.update(treatment)
    suspend fun deleteTreatment(treatment: Treatment) = treatmentDao.delete(treatment)

    suspend fun insertAppointment(appointment: Appointment) = appointmentDao.insert(appointment)
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.update(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.delete(appointment)

    suspend fun markTreatmentAsCompleted(treatmentId: String) = treatmentDao.markAsCompleted(treatmentId)

    suspend fun makePayment(client: Client, amount: Double) {
        if (amount <= 0) return

        db.withTransaction {
            val currentClient = clientDao.getClientById(client.clientId) ?: return@withTransaction
            val actualPaymentAmount = min(amount, currentClient.debtAmount)
            if (actualPaymentAmount <= 0) return@withTransaction

            val payment = Payment(
                clientIdFk = currentClient.clientId,
                amount = actualPaymentAmount,
                paymentDate = System.currentTimeMillis()
            )
            paymentDao.insert(payment)

            val newDebt = currentClient.debtAmount - actualPaymentAmount
            clientDao.updateDebt(currentClient.clientId, if (newDebt < 0.01) 0.0 else newDebt)
        }
    }

    suspend fun insertSale(sale: Sale, items: List<CartItem>) {
        require(items.isNotEmpty()) { "La venta debe tener al menos un producto o servicio." }
        require(items.all { it.quantity > 0.0 }) { "Todas las cantidades de la venta deben ser mayores a cero." }

        db.withTransaction {
            saleDao.insertSale(sale)

            items.forEach { cartItem ->
                val productFromCart = cartItem.product
                var productForSale = productDao.getProductById(productFromCart.productId)
                    ?: throw IllegalStateException("Producto no encontrado: ${productFromCart.name}")

                if (shouldDiscountStock(productForSale)) {
                    productForSale = ensureStockAvailable(productForSale, cartItem.quantity)
                    productDao.update(productForSale.copy(stock = productForSale.stock - cartItem.quantity))
                }

                val crossRef = SaleProductCrossRef(
                    saleId = sale.saleId,
                    productId = productForSale.productId,
                    quantitySold = cartItem.quantity,
                    priceAtTimeOfSale = productForSale.price,
                    notes = cartItem.notes,
                    overridePrice = cartItem.overridePrice
                )
                saleDao.insertSaleProductCrossRef(crossRef)
            }
        }
    }

    private fun shouldDiscountStock(product: Product): Boolean {
        return !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY
    }

    private suspend fun ensureStockAvailable(product: Product, requiredQuantity: Double): Product {
        var currentProduct = productDao.getProductById(product.productId)
            ?: throw IllegalStateException("Producto no encontrado: ${product.name}")

        if (currentProduct.stock >= requiredQuantity) return currentProduct

        val container = productDao.findContainerForProduct(currentProduct.productId)
        val containerSize = container?.containerSize ?: 0.0
        val fullContainersAvailable = floor(container?.stock ?: 0.0).toInt()

        if (container != null && containerSize > 0.0 && fullContainersAvailable > 0) {
            val missingQuantity = requiredQuantity - currentProduct.stock
            val containersNeeded = ceil(missingQuantity / containerSize).toInt()
            val containersToOpen = min(containersNeeded, fullContainersAvailable)

            if (containersToOpen > 0) {
                productDao.update(container.copy(stock = container.stock - containersToOpen))
                currentProduct = currentProduct.copy(stock = currentProduct.stock + (containersToOpen * containerSize))
                productDao.update(currentProduct)
            }
        }

        if (currentProduct.stock < requiredQuantity) {
            throw IllegalStateException(
                "Stock insuficiente para ${currentProduct.name}. Disponible: ${currentProduct.stock}, solicitado: $requiredQuantity"
            )
        }

        return currentProduct
    }

    suspend fun performRestock(order: RestockOrder, items: List<RestockOrderItem>) {
        require(items.isNotEmpty()) { "La reposición debe tener al menos un producto." }
        require(items.all { it.quantity > 0.0 && it.costPerUnit >= 0.0 }) {
            "Las cantidades deben ser mayores a cero y los costos no pueden ser negativos."
        }

        db.withTransaction {
            restockDao.insertOrder(order)
            restockDao.insertOrderItems(items)

            items.forEach { item ->
                val product = productDao.getProductById(item.productIdFk)
                    ?: throw IllegalStateException("Producto no encontrado en reposición.")
                if (!product.isService) {
                    val updatedStock = product.stock + item.quantity
                    val updatedProduct = product.copy(
                        stock = updatedStock,
                        cost = item.costPerUnit
                    )
                    productDao.update(updatedProduct)
                }
            }
        }
    }

    // --- Export and Import Logic ---
    private fun <T> listToCsvString(data: List<T>, headers: Array<String>, recordToStringArray: (T) -> Array<String>): String {
        StringWriter().use { writer ->
            val csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader(*headers).build()
            CSVPrinter(writer, csvFormat).use { csvPrinter ->
                data.forEach { record ->
                    csvPrinter.printRecord(*recordToStringArray(record))
                }
            }
            return writer.toString()
        }
    }

    suspend fun exportarDatosCompletos(): Map<String, String> = withContext(Dispatchers.IO) {
        val csvMap = mutableMapOf<String, String>()

        // Clients, Suppliers, Products, Pets, Treatments, Sales, etc.
        val clientHeaders = arrayOf("clientId", "name", "phone", "address", "debtAmount")
        val clients = clientDao.getAllClients().first()
        if (clients.isNotEmpty()) {
            csvMap["clients.csv"] = listToCsvString(clients, clientHeaders) { client ->
                arrayOf(client.clientId, client.name, client.phone ?: "", client.address ?: "", client.debtAmount.toString())
            }
        }

        val supplierHeaders = arrayOf("supplierId", "name", "contactPerson", "phone", "email")
        val suppliers = supplierDao.getAllSuppliers().first()
        if (suppliers.isNotEmpty()) {
            csvMap["suppliers.csv"] = listToCsvString(suppliers, supplierHeaders) { supplier ->
                arrayOf(supplier.supplierId, supplier.name, supplier.contactPerson ?: "", supplier.phone ?: "", supplier.email ?: "")
            }
        }

        val productHeaders = arrayOf("productId", "name", "price", "cost", "stock", "isService", "sellingMethod", "lowStockThreshold", "isContainer", "containedProductId", "containerSize", "supplierIdFk")
        val products = productDao.getAllProducts().first()
        if (products.isNotEmpty()) {
            csvMap["products.csv"] = listToCsvString(products, productHeaders) { product ->
                arrayOf(product.productId, product.name, product.price.toString(), product.cost.toString(), product.stock.toString(), product.isService.toString(), product.sellingMethod, product.lowStockThreshold?.toString() ?: "", product.isContainer.toString(), product.containedProductId ?: "", product.containerSize?.toString() ?: "", product.supplierIdFk ?: "")
            }
        }

        val petHeaders = arrayOf("petId", "name", "ownerIdFk", "birthDate", "breed", "allergies")
        val pets = petDao.getAllPetsSimple().first()
        if (pets.isNotEmpty()){
            csvMap["pets.csv"] = listToCsvString(pets, petHeaders) { pet ->
                arrayOf(pet.petId, pet.name, pet.ownerIdFk, pet.birthDate?.toString() ?: "", pet.breed ?: "", pet.allergies ?: "")
            }
        }

        val treatmentHeaders = arrayOf("treatmentId", "petIdFk", "serviceId", "treatmentDate", "description", "nextTreatmentDate", "isNextTreatmentCompleted", "symptoms", "diagnosis", "treatmentPlan", "weight", "temperature")
        val treatments = treatmentDao.getAllTreatmentsSimple().first()
        if (treatments.isNotEmpty()){
            csvMap["treatments.csv"] = listToCsvString(treatments, treatmentHeaders) { t ->
                arrayOf(t.treatmentId, t.petIdFk, t.serviceId ?: "", t.treatmentDate.toString(), t.description ?: "", t.nextTreatmentDate?.toString() ?: "", t.isNextTreatmentCompleted.toString(), t.symptoms ?: "", t.diagnosis ?: "", t.treatmentPlan ?: "", t.weight?.toString() ?: "", t.temperature ?: "")
            }
        }

        val saleHeaders = arrayOf("saleId", "date", "totalAmount", "clientIdFk")
        val sales = saleDao.getAllSalesSimple().first()
        if (sales.isNotEmpty()){
            csvMap["sales.csv"] = listToCsvString(sales, saleHeaders) { sale ->
                arrayOf(sale.saleId, sale.date.toString(), sale.totalAmount.toString(), sale.clientIdFk ?: "")
            }
        }

        val transactionHeaders = arrayOf("transactionId", "saleIdFk", "date", "type", "amount", "description")
        val transactions = transactionDao.getAllTransactions().first()
        if (transactions.isNotEmpty()){
            csvMap["transactions.csv"] = listToCsvString(transactions, transactionHeaders) { t ->
                arrayOf(t.transactionId, t.saleIdFk ?: "", t.date.toString(), t.type, t.amount.toString(), t.description ?: "")
            }
        }

        val paymentHeaders = arrayOf("paymentId", "clientIdFk", "amount", "paymentDate")
        val payments = paymentDao.getAllPaymentsSimple().first()
        if (payments.isNotEmpty()){
            csvMap["payments.csv"] = listToCsvString(payments, paymentHeaders) { p ->
                arrayOf(p.paymentId, p.clientIdFk, p.amount.toString(), p.paymentDate.toString())
            }
        }

        val saleProductCrossRefHeaders = arrayOf("crossRefId", "saleId", "productId", "quantitySold", "priceAtTimeOfSale", "notes", "overridePrice")
        val saleProductCrossRefs = saleDao.getAllSaleProductCrossRefsSimple().first()
        if (saleProductCrossRefs.isNotEmpty()){
            csvMap["sale_product_cross_refs.csv"] = listToCsvString(saleProductCrossRefs, saleProductCrossRefHeaders) { cr ->
                arrayOf(cr.crossRefId, cr.saleId, cr.productId, cr.quantitySold.toString(), cr.priceAtTimeOfSale.toString(), cr.notes ?: "", cr.overridePrice?.toString() ?: "")
            }
        }

        val appointmentHeaders = arrayOf("appointmentId", "clientIdFk", "petIdFk", "appointmentDate", "description")
        val appointments = appointmentDao.getAllAppointmentsSimple().first()
        if (appointments.isNotEmpty()){
            csvMap["appointments.csv"] = listToCsvString(appointments, appointmentHeaders) { a ->
                arrayOf(a.appointmentId, a.clientIdFk, a.petIdFk, a.appointmentDate.toString(), a.description ?: "")
            }
        }

        return@withContext csvMap
    }

    suspend fun importarDatosDesdeZIP(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            val archivosDelZip = mutableMapOf<String, String>()
            var totalBytesRead = 0L

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        if (entry.isDirectory) {
                            zis.closeEntry()
                            continue
                        }

                        val fileName = entry.name.substringAfterLast('/')
                        if (fileName !in ALLOWED_BACKUP_FILES) {
                            throw BackupValidationException("El respaldo contiene un archivo no permitido: $fileName")
                        }

                        val entryBytes = readZipEntryBytesLimited(zis, fileName)
                        totalBytesRead += entryBytes.size
                        if (totalBytesRead > MAX_BACKUP_TOTAL_BYTES) {
                            throw BackupValidationException("El respaldo es demasiado grande para importarse de forma segura.")
                        }

                        archivosDelZip[fileName] = entryBytes.toString(Charsets.UTF_8)
                        zis.closeEntry()
                    }
                }
            }
            if (archivosDelZip.isEmpty()) return@withContext context.getString(R.string.import_error_zip_empty_or_invalid)

            val backupData = validateAndParseBackupData(archivosDelZip)
            performMergeImport(backupData)

            return@withContext context.getString(R.string.import_success_merge_completed)
        } catch (e: BackupValidationException) {
            e.printStackTrace()
            return@withContext e.message ?: context.getString(R.string.import_error_validation_unknown)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext context.getString(R.string.import_error_unexpected, e.message ?: context.getString(R.string.import_error_no_details))
        }
    }

    private fun readZipEntryBytesLimited(zis: ZipInputStream, fileName: String): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var bytesForEntry = 0

        while (true) {
            val read = zis.read(buffer)
            if (read == -1) break

            bytesForEntry += read
            if (bytesForEntry > MAX_BACKUP_FILE_BYTES) {
                throw BackupValidationException("El archivo $fileName supera el tamaño máximo permitido.")
            }

            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    @Throws(BackupValidationException::class)
    private fun validateAndParseBackupData(archivos: Map<String, String>): ParsedBackupData {
        // Parsing
        val clients = parseCSV(archivos["clients.csv"], ::parseClient)
        val products = parseCSV(archivos["products.csv"], ::parseProduct)
        val pets = parseCSV(archivos["pets.csv"], ::parsePet)
        val treatments = parseCSV(archivos["treatments.csv"], ::parseTreatment)
        val sales = parseCSV(archivos["sales.csv"], ::parseSale)
        val transactions = parseCSV(archivos["transactions.csv"], ::parseTransaction)
        val payments = parseCSV(archivos["payments.csv"], ::parsePayment)
        val saleProductCrossRefs = parseCSV(archivos["sale_product_cross_refs.csv"], ::parseSaleProductCrossRef)
        val appointments = parseCSV(archivos["appointments.csv"], ::parseAppointment)
        val suppliers = parseCSV(archivos["suppliers.csv"], ::parseSupplier)

        // Validation
        val clientIds = clients.map { it.clientId }.toSet()
        val petIds = pets.map { it.petId }.toSet()
        val productIds = products.map { it.productId }.toSet()
        val saleIds = sales.map { it.saleId }.toSet()

        pets.forEach { if (it.ownerIdFk !in clientIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_pet_invalid_owner, it.name)) }
        treatments.forEach { if (it.petIdFk !in petIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_treatment_invalid_pet, it.treatmentId)) }
        sales.forEach { if (it.clientIdFk != null && it.clientIdFk !in clientIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_sale_invalid_client, it.saleId)) }
        payments.forEach { if (it.clientIdFk !in clientIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_payment_invalid_client, it.paymentId)) }
        saleProductCrossRefs.forEach {
            if (it.saleId !in saleIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_sale_detail_invalid_sale_id, it.saleId))
            if (it.productId !in productIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_sale_detail_invalid_product_id, it.productId))
        }
        appointments.forEach {
            if (it.clientIdFk !in clientIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_appointment_invalid_client, it.appointmentId))
            if (it.petIdFk !in petIds) throw BackupValidationException(context.getString(R.string.backup_validation_error_appointment_invalid_pet, it.appointmentId))
        }
        if (suppliers.isNotEmpty()) {
            val supplierIds = suppliers.map { it.supplierId }.toSet()
            products.forEach { product ->
                if (product.supplierIdFk != null && product.supplierIdFk !in supplierIds) {
                    throw BackupValidationException(context.getString(R.string.backup_validation_error_product_invalid_supplier, product.name))
                }
            }
        }

        return ParsedBackupData(clients, products, pets, treatments, sales, transactions, payments, saleProductCrossRefs, appointments, suppliers)
    }

    private suspend fun performMergeImport(data: ParsedBackupData) {
        db.withTransaction {
            if (data.clients.isNotEmpty()) clientDao.insertAll(data.clients)
            if (data.suppliers.isNotEmpty()) supplierDao.insertAll(data.suppliers)
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

    // --- CSV Parsers ---
    private inline fun <T> parseCSV(content: String?, parser: (CSVRecord) -> T): List<T> {
        if (content.isNullOrBlank()) return emptyList()
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .build()
        return CSVParser.parse(content, format).map { record ->
            try {
                parser(record)
            } catch (e: BackupValidationException) {
                throw e
            } catch (e: Exception) {
                throw BackupValidationException("No se pudo leer una fila del respaldo: ${e.message ?: "sin detalle"}")
            }
        }
    }

    private fun CSVRecord.required(column: String): String {
        if (!isMapped(column)) throw BackupValidationException("Falta la columna obligatoria '$column' en un CSV del respaldo.")
        return get(column).ifBlank { throw BackupValidationException("La columna obligatoria '$column' tiene un valor vacío.") }
    }

    private fun CSVRecord.optional(column: String): String? {
        return if (isMapped(column)) get(column).ifBlank { null } else null
    }

    private fun parseClient(r: CSVRecord) = Client(
        clientId = r.required("clientId"),
        name = r.required("name"),
        phone = r.optional("phone"),
        address = r.optional("address"),
        debtAmount = r.required("debtAmount").toDouble()
    )

    private fun parseProduct(r: CSVRecord) = Product(
        productId = r.required("productId"),
        name = r.required("name"),
        price = r.required("price").toDouble(),
        cost = r.required("cost").toDouble(),
        stock = r.required("stock").toDouble(),
        isService = r.required("isService").toBoolean(),
        sellingMethod = r.optional("sellingMethod") ?: SELLING_METHOD_BY_UNIT,
        lowStockThreshold = r.optional("lowStockThreshold")?.toDoubleOrNull(),
        isContainer = r.optional("isContainer")?.toBoolean() ?: false,
        containedProductId = r.optional("containedProductId"),
        containerSize = r.optional("containerSize")?.toDoubleOrNull(),
        supplierIdFk = r.optional("supplierIdFk")
    )

    private fun parsePet(r: CSVRecord) = Pet(
        petId = r.required("petId"),
        name = r.required("name"),
        ownerIdFk = r.required("ownerIdFk"),
        birthDate = r.optional("birthDate")?.toLongOrNull(),
        breed = r.optional("breed"),
        allergies = r.optional("allergies")
    )

    private fun parseTreatment(r: CSVRecord) = Treatment(
        treatmentId = r.required("treatmentId"),
        petIdFk = r.required("petIdFk"),
        serviceId = r.optional("serviceId"),
        treatmentDate = r.required("treatmentDate").toLong(),
        description = r.optional("description"),
        nextTreatmentDate = r.optional("nextTreatmentDate")?.toLongOrNull(),
        isNextTreatmentCompleted = r.optional("isNextTreatmentCompleted")?.toBoolean() ?: false,
        symptoms = r.optional("symptoms"),
        diagnosis = r.optional("diagnosis"),
        treatmentPlan = r.optional("treatmentPlan"),
        weight = r.optional("weight")?.toDoubleOrNull(),
        temperature = r.optional("temperature")
    )

    private fun parseSale(r: CSVRecord) = Sale(
        saleId = r.required("saleId"),
        date = r.required("date").toLong(),
        totalAmount = r.required("totalAmount").toDouble(),
        clientIdFk = r.optional("clientIdFk")
    )

    private fun parseTransaction(r: CSVRecord) = Transaction(
        transactionId = r.required("transactionId"),
        saleIdFk = r.optional("saleIdFk"),
        date = r.required("date").toLong(),
        type = r.required("type"),
        amount = r.required("amount").toDouble(),
        description = r.optional("description")
    )

    private fun parsePayment(r: CSVRecord) = Payment(
        paymentId = r.required("paymentId"),
        clientIdFk = r.required("clientIdFk"),
        amount = r.required("amount").toDouble(),
        paymentDate = r.required("paymentDate").toLong()
    )

    private fun parseSaleProductCrossRef(r: CSVRecord) = SaleProductCrossRef(
        crossRefId = r.optional("crossRefId") ?: UUID.randomUUID().toString(),
        saleId = r.required("saleId"),
        productId = r.required("productId"),
        quantitySold = r.required("quantitySold").toDouble(),
        priceAtTimeOfSale = r.required("priceAtTimeOfSale").toDouble(),
        notes = r.optional("notes"),
        overridePrice = r.optional("overridePrice")?.toDoubleOrNull()
    )

    private fun parseAppointment(r: CSVRecord) = Appointment(
        appointmentId = r.required("appointmentId"),
        clientIdFk = r.required("clientIdFk"),
        petIdFk = r.required("petIdFk"),
        appointmentDate = r.required("appointmentDate").toLong(),
        description = r.optional("description")
    )

    private fun parseSupplier(r: CSVRecord) = Supplier(
        supplierId = r.required("supplierId"),
        name = r.required("name"),
        contactPerson = r.optional("contactPerson"),
        phone = r.optional("phone"),
        email = r.optional("email")
    )
}