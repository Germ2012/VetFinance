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
import java.io.StringWriter
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import com.example.vetfinance.data.AppointmentLogDao // <-- Añadir importación
import com.example.vetfinance.data.AppointmentLog

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
    private val supplierDao: SupplierDao, // Added SupplierDao
    private val restockDao: RestockDao,   // Added RestockDao
    @ApplicationContext private val context: Context
) {

    suspend fun getRestockHistoryForDateRange(startDate: Long, endDate: Long): List<RestockHistoryItem> {
        return restockDao.getRestockHistoryForDateRange(startDate, endDate)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }
    // Reemplaza el método performInventoryTransfer con la lógica correcta.
    suspend fun performInventoryTransfer(containerId: String, containedId: String, amountToTransfer: Double) {
        db.withTransaction {
            val containerProduct = productDao.getProductById(containerId)
            val containedProduct = productDao.getProductById(containedId)

            if (containerProduct != null && containedProduct != null) {
                if (containerProduct.stock >= 1) {
                    // Restar 1 del contenedor
                    val updatedContainer = containerProduct.copy(stock = containerProduct.stock - 1)
                    productDao.update(updatedContainer)

                    // Sumar la cantidad definida en 'containerSize' al producto a granel
                    val updatedContained = containedProduct.copy(stock = containedProduct.stock + amountToTransfer)
                    productDao.update(updatedContained)
                }
            }
        }
    }
    suspend fun deleteClient(client: Client) {
        clientDao.delete(client)
    }

    // --- FUNCIÓN ACTUALIZADA ---
    suspend fun deleteSale(saleWithProducts: SaleWithProducts) {
        val sale = saleWithProducts.sale
        db.withTransaction {
            val saleDetails = saleDao.getSaleDetailsBySaleId(sale.saleId)
            for (detail in saleDetails) {
                val product = productDao.getProductById(detail.productId)
                // Esta condición ahora cubre ambos tipos de venta (Unidad y Peso/Monto)
                if (product != null && !product.isService && product.sellingMethod != SELLING_METHOD_DOSE_ONLY) {
                    val newStock = product.stock + detail.quantitySold
                    productDao.update(product.copy(stock = newStock))
                }
            }
            saleDao.deleteSaleProductCrossRefs(sale.saleId)
            saleDao.deleteSale(sale)
        }
    }

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

    fun getAppointmentsForDate(date: LocalDate): Flow<List<AppointmentWithDetails>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return appointmentDao.getAppointmentsForDateRange(startOfDay, endOfDay)
    }

    // Exposes the DAO query for a date range
    fun getAppointmentsForDate(startDate: Long, endDate: Long): Flow<List<AppointmentWithDetails>> {
        return appointmentDao.getAppointmentsForDateRange(startDate, endDate)
    }

    fun getTopSellingProducts(startDate: Long, endDate: Long, limit: Int): Flow<List<TopSellingProduct>> = saleDao.getTopSellingProducts(startDate, endDate, limit)
    fun getTotalDebt(): Flow<Double?> = clientDao.getTotalDebt()
    fun getTotalInventoryValue(): Flow<Double?> = productDao.getTotalInventoryValue()

    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getAllSales(): Flow<List<SaleWithProducts>> = saleDao.getAllSalesWithProducts()
    fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers() // Added supplier function
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>> = paymentDao.getPaymentsForClient(clientId)
    fun getAllPetsWithOwners(): Flow<List<PetWithOwner>> = petDao.getAllPetsWithOwners()
    fun getTreatmentsForPet(petId: String): Flow<List<Treatment>> = treatmentDao.getTreatmentsForPet(petId)
    fun getUpcomingTreatments(): Flow<List<Treatment>> = treatmentDao.getUpcomingTreatments()
    suspend fun getSaleDetailsBySaleId(saleId: String): List<SaleProductCrossRef> = saleDao.getSaleDetailsBySaleId(saleId)
    suspend fun getProductById(productId: String): Product? = productDao.getProductById(productId)

    /**
 * Inserta un producto si es nuevo o lo actualiza si ya existe.
 * Esta función es la ÚNICA responsable de asignar un nuevo ID a los productos nuevos.
 */
suspend fun insertOrUpdateProduct(product: Product) {
    // Intenta encontrar un producto existente con el ID proporcionado.
    val existingProduct = productDao.getProductById(product.productId)

    if (existingProduct == null) {
        // --- LÓGICA PARA PRODUCTOS NUEVOS ---
        // Si no se encuentra, es un producto 100% nuevo.
        // Se crea una copia y se le asigna un ID único y aleatorio AHORA.
        val productToInsert = product.copy(productId = UUID.randomUUID().toString())
        productDao.insertProduct(productToInsert) // Llama a la función del DAO
    } else {
        // --- LÓGICA PARA ACTUALIZACIONES ---
        // Si se encuentra, es una actualización. Se guarda con sus nuevos datos.
        productDao.update(product)
    }
}
    suspend fun insertClient(client: Client) = clientDao.insertAll(listOf(client))
    suspend fun updateClient(client: Client) = clientDao.update(client)
    suspend fun insertSupplier(supplier: Supplier) = supplierDao.insert(supplier) // Added supplier function
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.update(supplier) // Added supplier function
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.delete(supplier) // Added supplier function
    suspend fun insertPet(pet: Pet) = petDao.insert(pet)
    suspend fun updatePet(pet: Pet) = petDao.update(pet)
    suspend fun insertTreatment(treatment: Treatment) = treatmentDao.insert(treatment)

    // --- INICIO CÓDIGO A AÑADIR ---
    suspend fun updateTreatment(treatment: Treatment) = treatmentDao.update(treatment)
    suspend fun deleteTreatment(treatment: Treatment) = treatmentDao.delete(treatment)
    // --- FIN CÓDIGO A AÑADIR ---
    
    suspend fun insertAppointment(appointment: Appointment) = appointmentDao.insert(appointment)
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.update(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.delete(appointment)

    suspend fun markTreatmentAsCompleted(treatmentId: String) = treatmentDao.markAsCompleted(treatmentId)

    suspend fun makePayment(client: Client, amount: Double) {
        if (amount <= 0) return
        val actualPaymentAmount = min(amount, client.debtAmount)
        if (actualPaymentAmount <= 0) return

        val payment = Payment(clientIdFk = client.clientId, amount = actualPaymentAmount, paymentDate = System.currentTimeMillis())
        paymentDao.insert(payment)

        val newDebt = client.debtAmount - actualPaymentAmount
        clientDao.updateDebt(client.clientId, if (newDebt < 0.01) 0.0 else newDebt)
    }

    // --- FUNCIÓN ACTUALIZADA ---
    suspend fun insertSale(sale: Sale, items: List<CartItem>) {
        db.withTransaction {
            saleDao.insertSale(sale)
            items.forEach { cartItem ->
                val product = cartItem.product
                val crossRef = SaleProductCrossRef(
                    saleId = sale.saleId,
                    productId = cartItem.product.productId,
                    quantitySold = cartItem.quantity,
                    priceAtTimeOfSale = cartItem.product.price,
                    notes = cartItem.notes,
                    overridePrice = cartItem.overridePrice
                )
                saleDao.insertSaleProductCrossRef(crossRef)

                if (product.sellingMethod != SELLING_METHOD_DOSE_ONLY && !product.isService) {
                    // Si el producto que se vende es a granel y no hay stock suficiente, abre un contenedor.
                    if (product.stock < cartItem.quantity) {
                        // Nota: Esta consulta puede ser ineficiente. Considerar un método DAO más directo.
                        val container = productDao.getAllProducts().first()
                            .find { it.isContainer && it.containedProductId == product.productId }

                        if (container != null) {
                            performInventoryTransfer(container.productId, product.productId, container.containerSize ?: 0.0)
                        }
                    }

                    // Ahora, con el stock potencialmente actualizado, descuenta la cantidad vendida.
                    // Es crucial volver a obtener el producto para tener el stock más reciente.
                    val currentProduct = productDao.getProductById(product.productId)
                    if (currentProduct != null) {
                        val updatedStock = currentProduct.stock - cartItem.quantity
                        productDao.update(currentProduct.copy(stock = updatedStock))
                    }
                }
            }
        }
    }

    suspend fun performRestock(order: RestockOrder, items: List<RestockOrderItem>) {
        db.withTransaction {
            restockDao.insertOrder(order)
            restockDao.insertOrderItems(items)

            items.forEach { item ->
                val product = productDao.getProductById(item.productIdFk)
                // Only update stock for non-service products
                if (product != null && !product.isService) {
                    val updatedStock = product.stock + item.quantity
                    val updatedProduct = product.copy(
                        stock = updatedStock,
                        cost = item.costPerUnit // Update product cost to the new cost per unit
                    )
                    productDao.update(updatedProduct)
                }
            }
        }
    }

    private fun <T> listToCsvString(data: List<T>, headers: Array<String>, recordToStringArray: (T) -> Array<String>): String {
        StringWriter().use { writer ->
            CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(*headers)).use { csvPrinter ->
                data.forEach { record ->
                    csvPrinter.printRecord(*recordToStringArray(record))
                }
            }
            return writer.toString()
        }
    }

    suspend fun exportarDatosCompletos(): Map<String, String> = withContext(Dispatchers.IO) {
        val csvMap = mutableMapOf<String, String>()

        // Clients
        val clientHeaders = arrayOf("clientId", "name", "phone", "address", "debtAmount")
        val clients = clientDao.getAllClients().first()
        if (clients.isNotEmpty()) {
            csvMap["clients.csv"] = listToCsvString(clients, clientHeaders) { client ->
                arrayOf(client.clientId, client.name, client.phone ?: "", client.address ?: "", client.debtAmount.toString())
            }
        }

        // Products
        val
productHeaders = arrayOf("productId", "name", "price", "cost", "stock", "isService", "sellingMethod", "lowStockThreshold", "supplierIdFk") // Added supplierIdFk
        val products = productDao.getAllProducts().first()
        if (products.isNotEmpty()) {
            csvMap["products.csv"] = listToCsvString(products, productHeaders) { product ->
                arrayOf(product.productId, product.name, product.price.toString(), product.cost.toString(), product.stock.toString(), product.isService.toString(), product.sellingMethod, product.lowStockThreshold?.toString() ?: "", product.supplierIdFk ?: "")
            }
        }

        // Pets
        val petHeaders = arrayOf("petId", "name", "ownerIdFk", "birthDate", "breed", "allergies")
        val pets = petDao.getAllPetsSimple().first()
        if (pets.isNotEmpty()){
            csvMap["pets.csv"] = listToCsvString(pets, petHeaders) { pet ->
                arrayOf(pet.petId, pet.name, pet.ownerIdFk, pet.birthDate?.toString() ?: "", pet.breed ?: "", pet.allergies ?: "")
            }
        }

        // Treatments
        val treatmentHeaders = arrayOf("treatmentId", "petIdFk", "serviceId", "treatmentDate", "description", "nextTreatmentDate", "isNextTreatmentCompleted", "symptoms", "diagnosis", "treatmentPlan", "weight", "temperature")
        val treatments = treatmentDao.getAllTreatmentsSimple().first()
        if (treatments.isNotEmpty()){
            csvMap["treatments.csv"] = listToCsvString(treatments, treatmentHeaders) { t ->
                arrayOf(t.treatmentId, t.petIdFk, t.serviceId ?: "", t.treatmentDate.toString(), t.description ?: "", t.nextTreatmentDate?.toString() ?: "", t.isNextTreatmentCompleted.toString(), t.symptoms ?: "", t.diagnosis ?: "", t.treatmentPlan ?: "", t.weight?.toString() ?: "", t.temperature ?: "")
            }
        }

        // Sales
        val saleHeaders = arrayOf("saleId", "date", "totalAmount", "clientIdFk")
        val sales = saleDao.getAllSalesSimple().first()
        if (sales.isNotEmpty()){
            csvMap["sales.csv"] = listToCsvString(sales, saleHeaders) { sale ->
                arrayOf(sale.saleId, sale.date.toString(), sale.totalAmount.toString(), sale.clientIdFk ?: "")
            }
        }

        // Transactions
        val transactionHeaders = arrayOf("transactionId", "saleIdFk", "date", "type", "amount", "description")
        val transactions = transactionDao.getAllTransactions().first()
        if (transactions.isNotEmpty()){
            csvMap["transactions.csv"] = listToCsvString(transactions, transactionHeaders) { t ->
                arrayOf(t.transactionId, t.saleIdFk ?: "", t.date.toString(), t.type, t.amount.toString(), t.description ?: "")
            }
        }

        // Payments
        val paymentHeaders = arrayOf("paymentId", "clientIdFk", "amount", "paymentDate")
        val payments = paymentDao.getAllPaymentsSimple().first()
        if (payments.isNotEmpty()){
            csvMap["payments.csv"] = listToCsvString(payments, paymentHeaders) { p ->
                arrayOf(p.paymentId, p.clientIdFk, p.amount.toString(), p.paymentDate.toString())
            }
        }

        // SaleProductCrossRefs
        val saleProductCrossRefHeaders = arrayOf("saleId", "productId", "quantitySold", "priceAtTimeOfSale", "notes", "overridePrice")
        val saleProductCrossRefs = saleDao.getAllSaleProductCrossRefsSimple().first()
        if (saleProductCrossRefs.isNotEmpty()){
            csvMap["sale_product_cross_refs.csv"] = listToCsvString(saleProductCrossRefs, saleProductCrossRefHeaders) { cr ->
                arrayOf(cr.saleId, cr.productId, cr.quantitySold.toString(), cr.priceAtTimeOfSale.toString(), cr.notes ?: "", cr.overridePrice?.toString() ?: "")
            }
        }

        // Appointments
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
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    generateSequence { zis.nextEntry }.forEach { entry ->
                        archivosDelZip[entry.name] = zis.bufferedReader().readText()
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

    @Throws(BackupValidationException::class)
    private fun validateAndParseBackupData(archivos: Map<String, String>): ParsedBackupData {
        val clients = parseCSV(archivos["clients.csv"], ::parseClient)
        val products = parseCSV(archivos["products.csv"], ::parseProduct)
        val pets = parseCSV(archivos["pets.csv"], ::parsePet)
        val treatments = parseCSV(archivos["treatments.csv"], ::parseTreatment)
        val sales = parseCSV(archivos["sales.csv"], ::parseSale)
        val transactions = parseCSV(archivos["transactions.csv"], ::parseTransaction)
        val payments = parseCSV(archivos["payments.csv"], ::parsePayment)
        val saleProductCrossRefs = parseCSV(archivos["sale_product_cross_refs.csv"], ::parseSaleProductCrossRef)
        val appointments = parseCSV(archivos["appointments.csv"], ::parseAppointment)

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

        return ParsedBackupData(clients, products, pets, treatments, sales, transactions, payments, saleProductCrossRefs, appointments)
    }

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

    private inline fun <T> parseCSV(content: String?, parser: (org.apache.commons.csv.CSVRecord) -> T): List<T> {
        if (content.isNullOrBlank()) return emptyList()
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build()
        return CSVParser.parse(content, format).map(parser)
    }

    private fun parseClient(r: org.apache.commons.csv.CSVRecord) = Client(r["clientId"], r["name"], r["phone"].ifEmpty { null }, r.get("address")?.ifEmpty { null }, r["debtAmount"].toDouble())
    private fun parseProduct(r: org.apache.commons.csv.CSVRecord) = Product(r["productId"], r["name"], r["price"].toDouble(), r["cost"].toDouble(), r["stock"].toDouble(), r["isService"].toBoolean(), r.get("sellingMethod") ?: SELLING_METHOD_BY_UNIT, r.get("lowStockThreshold")?.toDoubleOrNull(), supplierIdFk = r.get("supplierIdFk")?.ifEmpty { null }) // Added supplierIdFk
    private fun parsePet(r: org.apache.commons.csv.CSVRecord) = Pet(r["petId"], r["name"], r["ownerIdFk"], r["birthDate"].toLongOrNull(), r["breed"].ifEmpty { null }, r["allergies"].ifEmpty { null })
    private fun parseTreatment(r: org.apache.commons.csv.CSVRecord) = Treatment(r["treatmentId"], r["petIdFk"], r["serviceId"].ifEmpty { null }, r["treatmentDate"].toLong(), r["description"]?.ifEmpty { null }, r["nextTreatmentDate"].toLongOrNull(), r["isNextTreatmentCompleted"].toBoolean(), r["symptoms"].ifEmpty { null }, r["diagnosis"].ifEmpty { null }, r["treatmentPlan"].ifEmpty { null }, r["weight"].toDoubleOrNull(), r["temperature"]?.ifEmpty { null })
    private fun parseSale(r: org.apache.commons.csv.CSVRecord) = Sale(r["saleId"], r["date"].toLong(), r["totalAmount"].toDouble(), r["clientIdFk"]?.ifEmpty { null })
    private fun parseTransaction(r: org.apache.commons.csv.CSVRecord) = Transaction(r["transactionId"], r["saleIdFk"].ifEmpty { null }, r["date"].toLong(), r["type"], r["amount"].toDouble(), r["description"].ifEmpty { null })
    private fun parsePayment(r: org.apache.commons.csv.CSVRecord) = Payment(r["paymentId"], r["clientIdFk"], r["amount"].toDouble(), r["paymentDate"].toLong())
    private fun parseSaleProductCrossRef(r: org.apache.commons.csv.CSVRecord) = SaleProductCrossRef(
        saleId = r["saleId"],
        productId = r["productId"],
        quantitySold = r["quantitySold"].toDouble(),
        priceAtTimeOfSale = r["priceAtTimeOfSale"].toDouble(),
        notes = r.get("notes")?.ifEmpty { null },
        overridePrice = r.get("overridePrice")?.toDoubleOrNull()
    )
    private fun parseAppointment(r: org.apache.commons.csv.CSVRecord) = Appointment(r["appointmentId"], r["clientIdFk"], r["petIdFk"], r["appointmentDate"].toLong(), r["description"]?.ifEmpty { null })
}
