package com.example.vetfinance.viewmodel

import android.content.Context
import android.widget.Toast
import com.example.vetfinance.R
import com.example.vetfinance.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class VetRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val transactionDao: TransactionDao,
    private val clientDao: ClientDao,
    private val paymentDao: PaymentDao,
    private val petDao: PetDao,
    private val treatmentDao: TreatmentDao,
    private val appointmentDao: AppointmentDao,
    private val context: Context
) {
    // ... (El resto del código del repositorio permanece igual)
    // ...
    // Asegúrate de que el resto del código esté aquí,
    // esta corrección solo afecta la firma de la clase y el constructor.
    // ...
    suspend fun exportDatabaseToZip(outputStream: OutputStream) {
        withContext(Dispatchers.IO) {
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(outputStream))

            // Exportar cada tabla a un archivo CSV
            exportTableToCsv(zipOutputStream, "clients.csv", clientDao.getAllClientsForBackup())
            exportTableToCsv(zipOutputStream, "pets.csv", petDao.getAllPetsForBackup())
            exportTableToCsv(zipOutputStream, "products.csv", productDao.getAllProductsForBackup())
            exportTableToCsv(zipOutputStream, "sales.csv", saleDao.getAllSalesForBackup())
            exportTableToCsv(zipOutputStream, "sale_details.csv", saleDao.getAllSaleDetailsForBackup())
            exportTableToCsv(zipOutputStream, "payments.csv", paymentDao.getAllPaymentsForBackup())
            exportTableToCsv(zipOutputStream, "treatments.csv", treatmentDao.getAllTreatmentsForBackup())
            exportTableToCsv(zipOutputStream, "appointments.csv", appointmentDao.getAllAppointmentsForBackup())
            exportTableToCsv(zipOutputStream, "transactions.csv", transactionDao.getAllTransactionsForBackup())

            zipOutputStream.close()
        }
    }

    private inline fun <reified T> exportTableToCsv(zipOutputStream: ZipOutputStream, fileName: String, data: List<T>) {
        if (data.isEmpty()) return

        zipOutputStream.putNextEntry(ZipEntry(fileName))
        val writer = BufferedWriter(OutputStreamWriter(zipOutputStream))
        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(*getHeaders(T::class.java)))

        for (item in data) {
            csvPrinter.printRecord(*getValues(item))
        }

        csvPrinter.flush()
        writer.flush() // Solo flush, no cierres el writer aquí.
        zipOutputStream.closeEntry()
    }


    private fun getHeaders(clazz: Class<*>): Array<String> {
        return when (clazz) {
            Client::class.java -> arrayOf("id", "name", "phone", "address")
            Pet::class.java -> arrayOf("id", "name", "ownerId", "breed")
            Product::class.java -> arrayOf("id", "name", "price", "cost", "stock", "isService", "sellingMethod")
            Sale::class.java -> arrayOf("id", "date", "totalAmount", "clientId")
            SaleDetail::class.java -> arrayOf("id", "saleId", "productId", "quantity", "price", "isByFraction", "amount")
            Payment::class.java -> arrayOf("id", "clientId", "amount", "date")
            Treatment::class.java -> arrayOf("id", "petId", "date", "description", "nextAppointment", "serviceId", "symptoms", "diagnosis", "plan", "weight", "temperature")
            Appointment::class.java -> arrayOf("id", "clientId", "petId", "date", "description")
            Transaction::class.java -> arrayOf("id", "date", "amount", "description", "type")
            else -> emptyArray()
        }
    }


    private fun getValues(item: Any): Array<String?> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return when (item) {
            is Client -> arrayOf(item.id.toString(), item.name, item.phone, item.address)
            is Pet -> arrayOf(item.id.toString(), item.name, item.ownerId.toString(), item.breed)
            is Product -> arrayOf(item.id.toString(), item.name, item.price.toString(), item.cost.toString(), item.stock.toString(), item.isService.toString(), item.sellingMethod)
            is Sale -> arrayOf(item.id.toString(), dateFormat.format(item.date), item.totalAmount.toString(), item.clientId?.toString())
            is SaleDetail -> arrayOf(item.id.toString(), item.saleId.toString(), item.productId.toString(), item.quantity.toString(), item.price.toString(), item.isByFraction.toString(), item.amount?.toString())
            is Payment -> arrayOf(item.id.toString(), item.clientId.toString(), item.amount.toString(), dateFormat.format(item.date))
            is Treatment -> arrayOf(item.id.toString(), item.petId.toString(), dateFormat.format(item.date), item.description, item.nextAppointment?.let { dateFormat.format(it) }, item.serviceId?.toString(), item.symptoms, item.diagnosis, item.plan, item.weight?.toString(), item.temperature)
            is Appointment -> arrayOf(item.id.toString(), item.clientId.toString(), item.petId.toString(), dateFormat.format(item.date), item.description)
            is Transaction -> arrayOf(item.id.toString(), dateFormat.format(item.date), item.amount.toString(), item.description, item.type)
            else -> emptyArray()
        }
    }


    suspend fun importDatabaseFromZip(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
                var zipEntry: ZipEntry?

                val dataMap = mutableMapOf<String, List<Array<String>>>()

                while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                    val entryName = zipEntry!!.name
                    val reader = BufferedReader(InputStreamReader(zipInputStream))
                    val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())

                    val records = csvParser.records.map { record ->
                        Array(record.size()) { i -> record[i] }
                    }
                    dataMap[entryName] = records
                }
                zipInputStream.close()

                if (dataMap.isEmpty()) {
                    throw IOException(context.getString(R.string.import_error_zip_empty_or_invalid))
                }

                val validationErrors = validateBackup(dataMap)
                if (validationErrors.isNotEmpty()) {
                    throw BackupValidationException(validationErrors)
                }
                mergeDatabase(dataMap)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.import_success_merge_completed), Toast.LENGTH_LONG).show()
                }
            } catch (e: BackupValidationException) {
                val errorMessages = e.errorMessages.joinToString("\n")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${context.getString(R.string.import_error_validation_unknown)}\n$errorMessages", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${context.getString(R.string.import_error_unexpected)}: ${e.message ?: context.getString(R.string.import_error_no_details)}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateBackup(dataMap: Map<String, List<Array<String>>>): List<String> {
        val errors = mutableListOf<String>()
        val clientIds = dataMap["clients.csv"]?.map { it[0].toLong() }?.toSet() ?: emptySet()
        val petIds = dataMap["pets.csv"]?.map { it[0].toLong() }?.toSet() ?: emptySet()
        val saleIds = dataMap["sales.csv"]?.map { it[0].toLong() }?.toSet() ?: emptySet()
        val productIds = dataMap["products.csv"]?.map { it[0].toLong() }?.toSet() ?: emptySet()

        dataMap["pets.csv"]?.forEach { if (it[2].toLong() !in clientIds) errors.add(context.getString(R.string.backup_validation_error_pet_invalid_owner)) }
        dataMap["treatments.csv"]?.forEach { if (it[1].toLong() !in petIds) errors.add(context.getString(R.string.backup_validation_error_treatment_invalid_pet)) }
        dataMap["sales.csv"]?.forEach { if (it[3].toLongOrNull() != null && it[3].toLong() !in clientIds) errors.add(context.getString(R.string.backup_validation_error_sale_invalid_client)) }
        dataMap["payments.csv"]?.forEach { if (it[1].toLong() !in clientIds) errors.add(context.getString(R.string.backup_validation_error_payment_invalid_client)) }

        dataMap["sale_details.csv"]?.forEach {
            if (it[1].toLong() !in saleIds) errors.add(context.getString(R.string.backup_validation_error_sale_detail_invalid_sale_id))
            if (it[2].toLong() !in productIds) errors.add(context.getString(R.string.backup_validation_error_sale_detail_invalid_product_id))
        }

        dataMap["appointments.csv"]?.forEach {
            if (it[1].toLong() !in clientIds) errors.add(context.getString(R.string.backup_validation_error_appointment_invalid_client))
            if (it[2].toLong() !in petIds) errors.add(context.getString(R.string.backup_validation_error_appointment_invalid_pet))
        }
        return errors.distinct()
    }


    private suspend fun mergeDatabase(dataMap: Map<String, List<Array<String>>>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        dataMap["clients.csv"]?.let { records ->
            val clients = records.map {
                Client(id = it[0].toLong(), name = it[1], phone = it[2], address = if (it.size > 3) it[3] else null)
            }
            clientDao.insertAll(clients)
        }
        dataMap["pets.csv"]?.let { records ->
            val pets = records.map {
                Pet(id = it[0].toLong(), name = it[1], ownerId = it[2].toLong(), breed = if (it.size > 3) it[3] else null)
            }
            petDao.insertAll(pets)
        }
        dataMap["products.csv"]?.let { records ->
            val products = records.map {
                Product(id = it[0].toLong(), name = it[1], price = it[2].toDouble(), cost = it[3].toDouble(), stock = it[4].toInt(), isService = it[5].toBoolean(), sellingMethod = it[6])
            }
            productDao.insertAll(products)
        }
        dataMap["sales.csv"]?.let { records ->
            val sales = records.map {
                Sale(id = it[0].toLong(), date = dateFormat.parse(it[1])!!, totalAmount = it[2].toDouble(), clientId = it[3].toLongOrNull())
            }
            saleDao.insertAll(sales)
        }
        dataMap["sale_details.csv"]?.let { records ->
            val saleDetails = records.map {
                SaleDetail(id = it[0].toLong(), saleId = it[1].toLong(), productId = it[2].toLong(), quantity = it[3].toInt(), price = it[4].toDouble(), isByFraction = it[5].toBoolean(), amount = it[6].toDoubleOrNull())
            }
            saleDao.insertAllSaleDetails(saleDetails)
        }
        dataMap["payments.csv"]?.let { records ->
            val payments = records.map {
                Payment(id = it[0].toLong(), clientId = it[1].toLong(), amount = it[2].toDouble(), date = dateFormat.parse(it[3])!!)
            }
            paymentDao.insertAll(payments)
        }
        dataMap["treatments.csv"]?.let { records ->
            val treatments = records.map {
                Treatment(id = it[0].toLong(), petId = it[1].toLong(), date = dateFormat.parse(it[2])!!, description = it[3], nextAppointment = it[4]?.let { dateStr -> dateFormat.parse(dateStr) }, serviceId = it[5].toLongOrNull(),
                    symptoms = it[6], diagnosis = it[7], plan = it[8], weight = it[9].toDoubleOrNull(), temperature = it[10])
            }
            treatmentDao.insertAll(treatments)
        }
        dataMap["appointments.csv"]?.let { records ->
            val appointments = records.map {
                Appointment(id = it[0].toLong(), clientId = it[1].toLong(), petId = it[2].toLong(), date = dateFormat.parse(it[3])!!, description = it[4])
            }
            appointmentDao.insertAll(appointments)
        }
        dataMap["transactions.csv"]?.let { records ->
            val transactions = records.map {
                Transaction(id = it[0].toLong(), date = dateFormat.parse(it[1])!!, amount = it[2].toDouble(), description = it[3], type = it[4])
            }
            transactionDao.insertAll(transactions)
        }
    }
}