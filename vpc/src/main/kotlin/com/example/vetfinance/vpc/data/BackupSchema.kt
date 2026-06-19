package com.example.vetfinance.vpc.data

enum class SqlKind(val sqlName: String) {
    TEXT("TEXT"),
    REAL("REAL"),
    INTEGER("INTEGER")
}

data class ColumnSpec(
    val name: String,
    val kind: SqlKind,
    val nullable: Boolean = false,
    val defaultSql: String? = null,
    val defaultImport: String? = null,
    val isBoolean: Boolean = false
) {
    fun ddl(): String {
        val builder = StringBuilder()
        builder.append(name).append(' ').append(kind.sqlName)
        if (!nullable) builder.append(" NOT NULL")
        if (defaultSql != null) builder.append(" DEFAULT ").append(defaultSql)
        return builder.toString()
    }

    fun decode(raw: String?): Any? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: defaultImport
        if (value == null) {
            if (!nullable) error("La columna obligatoria '$name' esta vacia.")
            return null
        }
        return when {
            isBoolean -> value.equals("true", ignoreCase = true) || value == "1"
            kind == SqlKind.REAL -> value.toDouble()
            kind == SqlKind.INTEGER -> value.toLong()
            else -> value
        }
    }

    fun encode(value: Any?): String {
        if (value == null) return ""
        return if (isBoolean) {
            val boolValue = when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                else -> value.toString().equals("true", ignoreCase = true) || value.toString() == "1"
            }
            boolValue.toString()
        } else {
            value.toString()
        }
    }
}

data class TableSpec(
    val csvName: String,
    val tableName: String,
    val primaryKey: List<String>,
    val columns: List<ColumnSpec>,
    val foreignKeys: List<String> = emptyList(),
    val indices: List<String> = emptyList()
) {
    val columnNames: List<String> = columns.map { it.name }

    fun createTableSql(): String {
        val definitions = buildList {
            addAll(columns.map { it.ddl() })
            add("PRIMARY KEY (${primaryKey.joinToString(", ")})")
            addAll(foreignKeys)
        }
        return "CREATE TABLE IF NOT EXISTS $tableName (${definitions.joinToString(", ")})"
    }
}

data class ReferenceCheck(
    val childCsvName: String,
    val childColumn: String,
    val parentCsvName: String,
    val parentColumn: String
)

object BackupSchema {
    const val byUnit = "Por Unidad"
    const val doseOnly = "Solo Dosis"

    private fun text(name: String, nullable: Boolean = false, default: String? = null) =
        ColumnSpec(name, SqlKind.TEXT, nullable, default?.let { "'$it'" }, default)

    private fun real(name: String, nullable: Boolean = false, default: String? = null) =
        ColumnSpec(name, SqlKind.REAL, nullable, default, default)

    private fun integer(name: String, nullable: Boolean = false, default: String? = null) =
        ColumnSpec(name, SqlKind.INTEGER, nullable, default, default)

    private fun bool(name: String, default: Boolean = false) =
        ColumnSpec(name, SqlKind.INTEGER, nullable = false, defaultSql = if (default) "1" else "0", defaultImport = default.toString(), isBoolean = true)

    val clients = TableSpec(
        csvName = "clients.csv",
        tableName = "clients",
        primaryKey = listOf("clientId"),
        columns = listOf(
            text("clientId"),
            text("name"),
            text("phone", nullable = true),
            text("address", nullable = true),
            real("debtAmount", default = "0.0")
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_clients_name ON clients(name)",
            "CREATE INDEX IF NOT EXISTS index_clients_phone ON clients(phone)"
        )
    )

    val suppliers = TableSpec(
        csvName = "suppliers.csv",
        tableName = "suppliers",
        primaryKey = listOf("supplierId"),
        columns = listOf(
            text("supplierId"),
            text("name"),
            text("contactPerson", nullable = true),
            text("phone", nullable = true),
            text("email", nullable = true)
        ),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_suppliers_name ON suppliers(name)")
    )

    val products = TableSpec(
        csvName = "products.csv",
        tableName = "products",
        primaryKey = listOf("productId"),
        columns = listOf(
            text("productId"),
            text("name"),
            real("price", default = "0.0"),
            real("cost", default = "0.0"),
            real("stock", default = "0.0"),
            bool("isService"),
            text("sellingMethod", default = byUnit),
            real("lowStockThreshold", nullable = true),
            bool("isContainer"),
            text("containedProductId", nullable = true),
            real("containerSize", nullable = true),
            text("supplierIdFk", nullable = true),
            text("category", nullable = true),
            text("unitMeasure", nullable = true)
        ),
        foreignKeys = listOf(
            "FOREIGN KEY(supplierIdFk) REFERENCES suppliers(supplierId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED",
            "FOREIGN KEY(containedProductId) REFERENCES products(productId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_products_name ON products(name)",
            "CREATE INDEX IF NOT EXISTS index_products_category ON products(category)",
            "CREATE INDEX IF NOT EXISTS index_products_supplierIdFk ON products(supplierIdFk)",
            "CREATE INDEX IF NOT EXISTS index_products_containedProductId ON products(containedProductId)"
        )
    )

    val pets = TableSpec(
        csvName = "pets.csv",
        tableName = "pets",
        primaryKey = listOf("petId"),
        columns = listOf(
            text("petId"),
            text("name"),
            text("ownerIdFk"),
            integer("birthDate", nullable = true),
            text("breed", nullable = true),
            text("allergies", nullable = true),
            text("observations", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(ownerIdFk) REFERENCES clients(clientId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_pets_ownerIdFk ON pets(ownerIdFk)")
    )

    val treatments = TableSpec(
        csvName = "treatments.csv",
        tableName = "treatments",
        primaryKey = listOf("treatmentId"),
        columns = listOf(
            text("treatmentId"),
            text("petIdFk"),
            text("serviceId", nullable = true),
            integer("treatmentDate"),
            text("description", nullable = true),
            integer("nextTreatmentDate", nullable = true),
            bool("isNextTreatmentCompleted"),
            text("symptoms", nullable = true),
            text("diagnosis", nullable = true),
            text("treatmentPlan", nullable = true),
            real("weight", nullable = true),
            text("temperature", nullable = true)
        ),
        foreignKeys = listOf(
            "FOREIGN KEY(petIdFk) REFERENCES pets(petId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED",
            "FOREIGN KEY(serviceId) REFERENCES products(productId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_treatments_petIdFk ON treatments(petIdFk)",
            "CREATE INDEX IF NOT EXISTS index_treatments_serviceId ON treatments(serviceId)",
            "CREATE INDEX IF NOT EXISTS index_treatments_nextTreatmentDate ON treatments(nextTreatmentDate)"
        )
    )

    val sales = TableSpec(
        csvName = "sales.csv",
        tableName = "sales",
        primaryKey = listOf("saleId"),
        columns = listOf(
            text("saleId"),
            integer("date"),
            real("totalAmount", default = "0.0"),
            text("clientIdFk", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(clientIdFk) REFERENCES clients(clientId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_sales_clientIdFk ON sales(clientIdFk)",
            "CREATE INDEX IF NOT EXISTS index_sales_date ON sales(date)"
        )
    )

    val transactions = TableSpec(
        csvName = "transactions.csv",
        tableName = "transactions",
        primaryKey = listOf("transactionId"),
        columns = listOf(
            text("transactionId"),
            text("saleIdFk", nullable = true),
            integer("date"),
            text("type"),
            real("amount", default = "0.0"),
            text("description", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(saleIdFk) REFERENCES sales(saleId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_transactions_saleIdFk ON transactions(saleIdFk)")
    )

    val payments = TableSpec(
        csvName = "payments.csv",
        tableName = "payments",
        primaryKey = listOf("paymentId"),
        columns = listOf(
            text("paymentId"),
            text("clientIdFk"),
            real("amount", default = "0.0"),
            integer("paymentDate")
        ),
        foreignKeys = listOf("FOREIGN KEY(clientIdFk) REFERENCES clients(clientId) ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_payments_clientIdFk ON payments(clientIdFk)",
            "CREATE INDEX IF NOT EXISTS index_payments_paymentDate ON payments(paymentDate)"
        )
    )

    val saleProductCrossRefs = TableSpec(
        csvName = "sale_product_cross_refs.csv",
        tableName = "sales_products_cross_ref",
        primaryKey = listOf("crossRefId"),
        columns = listOf(
            text("crossRefId"),
            text("saleId"),
            text("productId"),
            real("quantitySold", default = "0.0"),
            real("priceAtTimeOfSale", default = "0.0"),
            text("notes", nullable = true),
            real("overridePrice", nullable = true)
        ),
        foreignKeys = listOf(
            "FOREIGN KEY(saleId) REFERENCES sales(saleId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED",
            "FOREIGN KEY(productId) REFERENCES products(productId) ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED"
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_sales_products_cross_ref_saleId ON sales_products_cross_ref(saleId)",
            "CREATE INDEX IF NOT EXISTS index_sales_products_cross_ref_productId ON sales_products_cross_ref(productId)"
        )
    )

    val appointments = TableSpec(
        csvName = "appointments.csv",
        tableName = "appointments",
        primaryKey = listOf("appointmentId"),
        columns = listOf(
            text("appointmentId"),
            text("clientIdFk"),
            text("petIdFk"),
            integer("appointmentDate"),
            text("description", nullable = true),
            text("status", default = "PENDING")
        ),
        foreignKeys = listOf(
            "FOREIGN KEY(clientIdFk) REFERENCES clients(clientId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED",
            "FOREIGN KEY(petIdFk) REFERENCES pets(petId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED"
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_appointments_clientIdFk ON appointments(clientIdFk)",
            "CREATE INDEX IF NOT EXISTS index_appointments_petIdFk ON appointments(petIdFk)",
            "CREATE INDEX IF NOT EXISTS index_appointments_appointmentDate ON appointments(appointmentDate)"
        )
    )

    val clientDebtHistory = TableSpec(
        csvName = "client_debt_history.csv",
        tableName = "client_debt_history",
        primaryKey = listOf("historyId"),
        columns = listOf(
            text("historyId"),
            text("clientIdFk"),
            integer("eventDate"),
            text("eventType"),
            real("amountChange", default = "0.0"),
            real("balanceAfter", default = "0.0"),
            text("note", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(clientIdFk) REFERENCES clients(clientId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_client_debt_history_clientIdFk ON client_debt_history(clientIdFk)",
            "CREATE INDEX IF NOT EXISTS index_client_debt_history_eventDate ON client_debt_history(eventDate)"
        )
    )

    val supplierDebts = TableSpec(
        csvName = "supplier_debts.csv",
        tableName = "supplier_debts",
        primaryKey = listOf("debtId"),
        columns = listOf(
            text("debtId"),
            text("supplierIdFk", nullable = true),
            text("description"),
            real("amount", default = "0.0"),
            integer("dueDate"),
            integer("createdAt"),
            bool("isPaid"),
            integer("paidAt", nullable = true),
            text("note", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(supplierIdFk) REFERENCES suppliers(supplierId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_supplier_debts_supplierIdFk ON supplier_debts(supplierIdFk)")
    )

    val restockOrders = TableSpec(
        csvName = "restock_orders.csv",
        tableName = "restock_orders",
        primaryKey = listOf("orderId"),
        columns = listOf(
            text("orderId"),
            text("supplierIdFk"),
            integer("orderDate"),
            real("totalAmount", default = "0.0")
        ),
        foreignKeys = listOf("FOREIGN KEY(supplierIdFk) REFERENCES suppliers(supplierId) ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_restock_orders_supplierIdFk ON restock_orders(supplierIdFk)")
    )

    val restockOrderItems = TableSpec(
        csvName = "restock_order_items.csv",
        tableName = "restock_order_items",
        primaryKey = listOf("itemId"),
        columns = listOf(
            text("itemId"),
            text("orderIdFk"),
            text("productIdFk"),
            real("quantity", default = "0.0"),
            real("costPerUnit", default = "0.0")
        ),
        foreignKeys = listOf(
            "FOREIGN KEY(orderIdFk) REFERENCES restock_orders(orderId) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED",
            "FOREIGN KEY(productIdFk) REFERENCES products(productId) ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED"
        ),
        indices = listOf(
            "CREATE INDEX IF NOT EXISTS index_restock_order_items_orderIdFk ON restock_order_items(orderIdFk)",
            "CREATE INDEX IF NOT EXISTS index_restock_order_items_productIdFk ON restock_order_items(productIdFk)"
        )
    )

    val stockMovements = TableSpec(
        csvName = "stock_movements.csv",
        tableName = "stock_movements",
        primaryKey = listOf("movementId"),
        columns = listOf(
            text("movementId"),
            text("productIdFk", nullable = true),
            text("productNameSnapshot"),
            integer("movementDate"),
            text("movementType"),
            real("quantityChange", default = "0.0"),
            real("stockAfter", default = "0.0"),
            text("note", nullable = true),
            real("unitCost", nullable = true)
        ),
        foreignKeys = listOf("FOREIGN KEY(productIdFk) REFERENCES products(productId) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED"),
        indices = listOf("CREATE INDEX IF NOT EXISTS index_stock_movements_productIdFk ON stock_movements(productIdFk)")
    )

    val tables: List<TableSpec> = listOf(
        clients,
        suppliers,
        products,
        pets,
        treatments,
        sales,
        transactions,
        payments,
        saleProductCrossRefs,
        appointments,
        clientDebtHistory,
        supplierDebts,
        restockOrders,
        restockOrderItems,
        stockMovements
    )

    val byCsvName: Map<String, TableSpec> = tables.associateBy { it.csvName }
    val allowedCsvNames: Set<String> = byCsvName.keys

    val referenceChecks = listOf(
        ReferenceCheck("products.csv", "supplierIdFk", "suppliers.csv", "supplierId"),
        ReferenceCheck("products.csv", "containedProductId", "products.csv", "productId"),
        ReferenceCheck("pets.csv", "ownerIdFk", "clients.csv", "clientId"),
        ReferenceCheck("treatments.csv", "petIdFk", "pets.csv", "petId"),
        ReferenceCheck("treatments.csv", "serviceId", "products.csv", "productId"),
        ReferenceCheck("sales.csv", "clientIdFk", "clients.csv", "clientId"),
        ReferenceCheck("transactions.csv", "saleIdFk", "sales.csv", "saleId"),
        ReferenceCheck("payments.csv", "clientIdFk", "clients.csv", "clientId"),
        ReferenceCheck("sale_product_cross_refs.csv", "saleId", "sales.csv", "saleId"),
        ReferenceCheck("sale_product_cross_refs.csv", "productId", "products.csv", "productId"),
        ReferenceCheck("appointments.csv", "clientIdFk", "clients.csv", "clientId"),
        ReferenceCheck("appointments.csv", "petIdFk", "pets.csv", "petId"),
        ReferenceCheck("client_debt_history.csv", "clientIdFk", "clients.csv", "clientId"),
        ReferenceCheck("supplier_debts.csv", "supplierIdFk", "suppliers.csv", "supplierId"),
        ReferenceCheck("restock_orders.csv", "supplierIdFk", "suppliers.csv", "supplierId"),
        ReferenceCheck("restock_order_items.csv", "orderIdFk", "restock_orders.csv", "orderId"),
        ReferenceCheck("restock_order_items.csv", "productIdFk", "products.csv", "productId"),
        ReferenceCheck("stock_movements.csv", "productIdFk", "products.csv", "productId")
    )
}
