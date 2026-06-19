package com.example.vetfinance.vpc.data

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

data class DashboardStats(
    val clients: Int,
    val products: Int,
    val lowStock: Int,
    val sales: Int,
    val salesTotal: Double,
    val debtTotal: Double
)

data class ProductSummary(
    val productId: String,
    val name: String,
    val price: Double,
    val cost: Double,
    val stock: Double,
    val isService: Boolean,
    val category: String?
) {
    override fun toString(): String = name
}

data class ClientSummary(
    val clientId: String,
    val name: String,
    val phone: String?,
    val debtAmount: Double
) {
    override fun toString(): String = name
}

class VpcDatabase private constructor(
    val path: Path,
    private val connection: Connection
) : AutoCloseable {
    private val lock = Any()

    init {
        initialize()
    }

    private fun initialize() = synchronized(lock) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA busy_timeout = 5000")
            BackupSchema.tables.forEach { statement.execute(it.createTableSql()) }
            BackupSchema.tables.flatMap { it.indices }.forEach { statement.execute(it) }
        }
    }

    fun dashboardStats(): DashboardStats = synchronized(lock) {
        DashboardStats(
            clients = scalarInt("SELECT COUNT(*) FROM clients"),
            products = scalarInt("SELECT COUNT(*) FROM products"),
            lowStock = scalarInt(lowStockCountSql),
            sales = scalarInt("SELECT COUNT(*) FROM sales"),
            salesTotal = scalarDouble("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales"),
            debtTotal = scalarDouble("SELECT COALESCE(SUM(debtAmount), 0.0) FROM clients")
        )
    }

    fun searchProducts(query: String, limit: Int = 300): List<ProductSummary> = synchronized(lock) {
        val sql = """
            SELECT productId, name, price, cost, stock, isService, category
            FROM products
            WHERE ? = ''
                OR name LIKE ? COLLATE NOCASE
                OR COALESCE(category, '') LIKE ? COLLATE NOCASE
            ORDER BY name COLLATE NOCASE ASC
            LIMIT ?
        """.trimIndent()
        val like = "%${query.trim()}%"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, query.trim())
            statement.setString(2, like)
            statement.setString(3, like)
            statement.setInt(4, limit)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            ProductSummary(
                                productId = result.getString("productId"),
                                name = result.getString("name"),
                                price = result.getDouble("price"),
                                cost = result.getDouble("cost"),
                                stock = result.getDouble("stock"),
                                isService = result.getInt("isService") != 0,
                                category = result.getString("category")
                            )
                        )
                    }
                }
            }
        }
    }

    fun searchClients(query: String, limit: Int = 300): List<ClientSummary> = synchronized(lock) {
        val sql = """
            SELECT clientId, name, phone, debtAmount
            FROM clients
            WHERE ? = ''
                OR name LIKE ? COLLATE NOCASE
                OR COALESCE(phone, '') LIKE ? COLLATE NOCASE
            ORDER BY name COLLATE NOCASE ASC
            LIMIT ?
        """.trimIndent()
        val like = "%${query.trim()}%"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, query.trim())
            statement.setString(2, like)
            statement.setString(3, like)
            statement.setInt(4, limit)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            ClientSummary(
                                clientId = result.getString("clientId"),
                                name = result.getString("name"),
                                phone = result.getString("phone"),
                                debtAmount = result.getDouble("debtAmount")
                            )
                        )
                    }
                }
            }
        }
    }

    fun recentSales(limit: Int = 300): List<Array<Any?>> = synchronized(lock) {
        val sql = """
            SELECT s.saleId, s.date, COALESCE(c.name, 'Venta directa') AS clientName, s.totalAmount,
                COUNT(sp.crossRefId) AS itemCount
            FROM sales AS s
            LEFT JOIN clients AS c ON c.clientId = s.clientIdFk
            LEFT JOIN sales_products_cross_ref AS sp ON sp.saleId = s.saleId
            GROUP BY s.saleId, s.date, c.name, s.totalAmount
            ORDER BY s.date DESC
            LIMIT ?
        """.trimIndent()
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            arrayOf(
                                result.getString("saleId"),
                                result.getLong("date"),
                                result.getString("clientName"),
                                result.getDouble("totalAmount"),
                                result.getInt("itemCount")
                            )
                        )
                    }
                }
            }
        }
    }

    fun lowStockProducts(limit: Int = 300): List<ProductSummary> = synchronized(lock) {
        val sql = """
            SELECT productId, name, price, cost, stock, isService, category
            FROM products AS p
            WHERE ${lowStockWhereClause("p")}
            ORDER BY p.name COLLATE NOCASE ASC
            LIMIT ?
        """.trimIndent()
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            ProductSummary(
                                productId = result.getString("productId"),
                                name = result.getString("name"),
                                price = result.getDouble("price"),
                                cost = result.getDouble("cost"),
                                stock = result.getDouble("stock"),
                                isService = result.getInt("isService") != 0,
                                category = result.getString("category")
                            )
                        )
                    }
                }
            }
        }
    }

    fun insertClient(name: String, phone: String?, address: String?) = synchronized(lock) {
        val spec = BackupSchema.clients
        upsertRows(
            spec,
            listOf(
                mapOf(
                    "clientId" to UUID.randomUUID().toString(),
                    "name" to name,
                    "phone" to phone.orEmpty(),
                    "address" to address.orEmpty(),
                    "debtAmount" to "0.0"
                )
            )
        )
    }

    fun insertProduct(
        name: String,
        price: Double,
        cost: Double,
        stock: Double,
        isService: Boolean,
        category: String?
    ) = synchronized(lock) {
        val spec = BackupSchema.products
        upsertRows(
            spec,
            listOf(
                mapOf(
                    "productId" to UUID.randomUUID().toString(),
                    "name" to name,
                    "price" to price.toString(),
                    "cost" to cost.toString(),
                    "stock" to stock.toString(),
                    "isService" to isService.toString(),
                    "sellingMethod" to BackupSchema.byUnit,
                    "isContainer" to "false",
                    "category" to category.orEmpty()
                )
            )
        )
    }

    fun createCashSale(clientId: String?, productId: String, quantity: Double): String = synchronized(lock) {
        transaction {
            val product = getProductForSale(productId)
            if (!product.isService) {
                val updated = connection.prepareStatement(
                    "UPDATE products SET stock = stock - ? WHERE productId = ? AND stock >= ?"
                ).use { statement ->
                    statement.setDouble(1, quantity)
                    statement.setString(2, productId)
                    statement.setDouble(3, quantity)
                    statement.executeUpdate()
                }
                if (updated == 0) error("Stock insuficiente para ${product.name}.")
            }

            val saleId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val total = product.price * quantity
            upsertRows(
                BackupSchema.sales,
                listOf(
                    mapOf(
                        "saleId" to saleId,
                        "date" to now.toString(),
                        "totalAmount" to total.toString(),
                        "clientIdFk" to clientId.orEmpty()
                    )
                )
            )
            upsertRows(
                BackupSchema.saleProductCrossRefs,
                listOf(
                    mapOf(
                        "crossRefId" to UUID.randomUUID().toString(),
                        "saleId" to saleId,
                        "productId" to productId,
                        "quantitySold" to quantity.toString(),
                        "priceAtTimeOfSale" to product.price.toString()
                    )
                )
            )
            upsertRows(
                BackupSchema.transactions,
                listOf(
                    mapOf(
                        "transactionId" to UUID.randomUUID().toString(),
                        "saleIdFk" to saleId,
                        "date" to now.toString(),
                        "type" to "Ingreso",
                        "amount" to total.toString(),
                        "description" to "Venta registrada desde PC"
                    )
                )
            )
            if (!product.isService) {
                val stockAfter = scalarDouble("SELECT stock FROM products WHERE productId = ?", productId)
                upsertRows(
                    BackupSchema.stockMovements,
                    listOf(
                        mapOf(
                            "movementId" to UUID.randomUUID().toString(),
                            "productIdFk" to productId,
                            "productNameSnapshot" to product.name,
                            "movementDate" to now.toString(),
                            "movementType" to "SALE",
                            "quantityChange" to (-quantity).toString(),
                            "stockAfter" to stockAfter.toString(),
                            "note" to "Venta desde PC",
                            "unitCost" to product.cost.toString()
                        )
                    )
                )
            }
            saleId
        }
    }

    fun upsertRows(spec: TableSpec, rows: List<Map<String, String?>>): Int = synchronized(lock) {
        if (rows.isEmpty()) return@synchronized 0
        val placeholders = spec.columnNames.joinToString(", ") { "?" }
        val columns = spec.columnNames.joinToString(", ")
        val conflictTarget = spec.primaryKey.joinToString(", ")
        val updateColumns = spec.columnNames.filterNot { it in spec.primaryKey }
        val updateSet = updateColumns.joinToString(", ") { "$it = excluded.$it" }
        val sql = "INSERT INTO ${spec.tableName} ($columns) VALUES ($placeholders) ON CONFLICT($conflictTarget) DO UPDATE SET $updateSet"

        connection.prepareStatement(sql).use { statement ->
            rows.forEach { row ->
                bindRow(statement, spec, row)
                statement.addBatch()
            }
            statement.executeBatch().sum()
        }
    }

    fun exportRows(spec: TableSpec): List<Map<String, Any?>> = synchronized(lock) {
        val sql = "SELECT ${spec.columnNames.joinToString(", ")} FROM ${spec.tableName} ORDER BY ${spec.primaryKey.joinToString(", ")}"
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { result ->
                buildList {
                    while (result.next()) {
                        add(spec.columnNames.associateWith { result.getObject(it) })
                    }
                }
            }
        }
    }

    fun rowExists(tableName: String, columnName: String, id: String): Boolean = synchronized(lock) {
        connection.prepareStatement("SELECT 1 FROM $tableName WHERE $columnName = ? LIMIT 1").use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { it.next() }
        }
    }

    fun tableCount(tableName: String): Int = synchronized(lock) {
        scalarInt("SELECT COUNT(*) FROM $tableName")
    }

    fun <T> transaction(block: () -> T): T = synchronized(lock) {
        val oldAutoCommit = connection.autoCommit
        try {
            connection.autoCommit = false
            connection.createStatement().use { it.execute("PRAGMA defer_foreign_keys = ON") }
            val result = block()
            connection.commit()
            result
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = oldAutoCommit
        }
    }

    private fun bindRow(statement: PreparedStatement, spec: TableSpec, row: Map<String, String?>) {
        spec.columns.forEachIndexed { index, column ->
            val decoded = column.decode(row[column.name])
            when (decoded) {
                null -> statement.setObject(index + 1, null)
                is Boolean -> statement.setInt(index + 1, if (decoded) 1 else 0)
                is Double -> statement.setDouble(index + 1, decoded)
                is Long -> statement.setLong(index + 1, decoded)
                else -> statement.setString(index + 1, decoded.toString())
            }
        }
    }

    private fun getProductForSale(productId: String): ProductSummary {
        connection.prepareStatement(
            "SELECT productId, name, price, cost, stock, isService, category FROM products WHERE productId = ? LIMIT 1"
        ).use { statement ->
            statement.setString(1, productId)
            statement.executeQuery().use { result ->
                if (!result.next()) error("El producto seleccionado no existe.")
                return ProductSummary(
                    productId = result.getString("productId"),
                    name = result.getString("name"),
                    price = result.getDouble("price"),
                    cost = result.getDouble("cost"),
                    stock = result.getDouble("stock"),
                    isService = result.getInt("isService") != 0,
                    category = result.getString("category")
                )
            }
        }
    }

    private fun scalarInt(sql: String, vararg params: Any?): Int {
        connection.prepareStatement(sql).use { statement ->
            bindParams(statement, params)
            statement.executeQuery().use { result -> return if (result.next()) result.getInt(1) else 0 }
        }
    }

    private fun scalarDouble(sql: String, vararg params: Any?): Double {
        connection.prepareStatement(sql).use { statement ->
            bindParams(statement, params)
            statement.executeQuery().use { result -> return if (result.next()) result.getDouble(1) else 0.0 }
        }
    }

    private fun bindParams(statement: PreparedStatement, params: Array<out Any?>) {
        params.forEachIndexed { index, param ->
            when (param) {
                null -> statement.setObject(index + 1, null)
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                else -> statement.setObject(index + 1, param)
            }
        }
    }

    override fun close() {
        connection.close()
    }

    companion object {
        fun default(): VpcDatabase {
            val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                ?: Path.of(System.getProperty("user.home")).toString()
            val dir = Path.of(appData, "VetFinancePC")
            Files.createDirectories(dir)
            return open(dir.resolve("vetfinance_pc.db"))
        }

        fun open(path: Path): VpcDatabase {
            Class.forName("org.sqlite.JDBC")
            Files.createDirectories(path.toAbsolutePath().parent)
            val url = "jdbc:sqlite:${path.toAbsolutePath()}"
            return VpcDatabase(path.toAbsolutePath(), DriverManager.getConnection(url))
        }

        private val lowStockCountSql = "SELECT COUNT(*) FROM products AS p WHERE ${lowStockWhereClause("p")}"

        private fun lowStockWhereClause(alias: String): String {
            return """
                $alias.isService = 0
                AND $alias.isContainer = 0
                AND $alias.sellingMethod != '${BackupSchema.doseOnly}'
                AND COALESCE($alias.lowStockThreshold, CASE WHEN $alias.sellingMethod = '${BackupSchema.byUnit}' THEN 4.0 ELSE 0.0 END) > 0.0
                AND (
                    $alias.stock + COALESCE((
                        SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                        FROM products AS c
                        WHERE c.isContainer = 1
                            AND c.containedProductId = $alias.productId
                            AND c.stock > 0.0
                            AND COALESCE(c.containerSize, 0.0) > 0.0
                    ), 0.0)
                ) < COALESCE($alias.lowStockThreshold, CASE WHEN $alias.sellingMethod = '${BackupSchema.byUnit}' THEN 4.0 ELSE 0.0 END)
            """.trimIndent()
        }
    }
}
