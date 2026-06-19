package com.example.vetfinance.vpc.backup

import com.example.vetfinance.vpc.data.BackupSchema
import com.example.vetfinance.vpc.data.TableSpec
import com.example.vetfinance.vpc.data.VpcDatabase
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupImportReport(
    val importedRows: Int,
    val files: Int
)

data class BackupExportReport(
    val exportedRows: Int,
    val files: Int
)

class BackupService(private val database: VpcDatabase) {
    fun exportToZip(file: File): BackupExportReport {
        var exportedRows = 0
        var exportedFiles = 0

        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            BackupSchema.tables.forEach { spec ->
                val rows = database.exportRows(spec)
                if (rows.isEmpty()) return@forEach

                val csv = rowsToCsv(spec, rows)
                zip.putNextEntry(ZipEntry(spec.csvName))
                zip.write(csv.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()

                exportedRows += rows.size
                exportedFiles++
            }
        }

        return BackupExportReport(exportedRows, exportedFiles)
    }

    fun importZip(file: File): BackupImportReport {
        val files = readZip(file)
        if (files.isEmpty()) error("El ZIP no contiene archivos compatibles.")

        val parsedRows = files.mapValues { (csvName, content) ->
            val spec = BackupSchema.byCsvName[csvName] ?: error("Archivo no permitido: $csvName")
            parseCsv(spec, content)
        }

        validateReferences(parsedRows)

        var importedRows = 0
        database.transaction {
            BackupSchema.tables.forEach { spec ->
                val rows = parsedRows[spec.csvName].orEmpty()
                if (rows.isNotEmpty()) {
                    importedRows += database.upsertRows(spec, rows)
                }
            }
        }

        return BackupImportReport(importedRows, parsedRows.size)
    }

    private fun readZip(file: File): Map<String, String> {
        val files = linkedMapOf<String, String>()
        var totalBytes = 0L

        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }

                val csvName = entry.name.substringAfterLast('/')
                if (csvName !in BackupSchema.allowedCsvNames) {
                    error("El ZIP contiene un archivo no reconocido: $csvName")
                }

                val bytes = readEntry(zip, csvName)
                totalBytes += bytes.size
                if (totalBytes > MAX_TOTAL_BYTES) error("El respaldo supera el tamano maximo permitido.")

                files[csvName] = bytes.toString(StandardCharsets.UTF_8)
                zip.closeEntry()
            }
        }

        return files
    }

    private fun readEntry(zip: ZipInputStream, csvName: String): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var bytesForEntry = 0

        while (true) {
            val read = zip.read(buffer)
            if (read == -1) break

            bytesForEntry += read
            if (bytesForEntry > MAX_FILE_BYTES) {
                error("El archivo $csvName es demasiado grande para importarse.")
            }
            output.write(buffer, 0, read)
        }

        return output.toByteArray()
    }

    private fun parseCsv(spec: TableSpec, content: String): List<Map<String, String?>> {
        if (content.isBlank()) return emptyList()

        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .build()

        return CSVParser.parse(content, format).use { parser ->
            val headers = parser.headerMap.keys
            spec.columns.forEach { column ->
                val hasLegacyFallback = spec.csvName == "sale_product_cross_refs.csv" && column.name == "crossRefId"
                if (column.name !in headers && !column.nullable && column.defaultImport == null && !hasLegacyFallback) {
                    error("Falta la columna obligatoria '${column.name}' en ${spec.csvName}.")
                }
            }
            parser.map { record ->
                val row = spec.columns.associate { column ->
                    val value = if (record.isMapped(column.name)) record.get(column.name) else null
                    column.name to value
                }.toMutableMap()
                if (spec.csvName == "sale_product_cross_refs.csv" && row["crossRefId"].isNullOrBlank()) {
                    row["crossRefId"] = "${row["saleId"].orEmpty()}_${row["productId"].orEmpty()}"
                }
                row
            }
        }
    }

    private fun validateReferences(rowsByCsv: Map<String, List<Map<String, String?>>>) {
        val importedIds = BackupSchema.tables.associate { spec ->
            val idColumn = spec.primaryKey.singleOrNull()
            spec.csvName to if (idColumn == null) {
                emptySet()
            } else {
                rowsByCsv[spec.csvName].orEmpty()
                    .mapNotNull { it[idColumn]?.trim()?.takeIf(String::isNotEmpty) }
                    .toSet()
            }
        }

        BackupSchema.referenceChecks.forEach { check ->
            val parentSpec = BackupSchema.byCsvName.getValue(check.parentCsvName)
            for (row in rowsByCsv[check.childCsvName].orEmpty()) {
                val id = row[check.childColumn]?.trim()?.takeIf(String::isNotEmpty) ?: continue
                val existsInImport = id in importedIds.getValue(check.parentCsvName)
                val existsInDatabase = database.rowExists(parentSpec.tableName, check.parentColumn, id)
                if (!existsInImport && !existsInDatabase) {
                    error("${check.childCsvName} apunta a un registro inexistente en ${check.parentCsvName}: $id")
                }
            }
        }
    }

    private fun rowsToCsv(spec: TableSpec, rows: List<Map<String, Any?>>): String {
        StringWriter().use { writer ->
            val csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(*spec.columnNames.toTypedArray())
                .build()
            CSVPrinter(writer, csvFormat).use { printer ->
                rows.forEach { row ->
                    printer.printRecord(
                        spec.columns.map { column ->
                            column.encode(row[column.name])
                        }
                    )
                }
            }
            return writer.toString()
        }
    }

    private companion object {
        const val MAX_FILE_BYTES = 50_000_000
        const val MAX_TOTAL_BYTES = 250_000_000
    }
}
