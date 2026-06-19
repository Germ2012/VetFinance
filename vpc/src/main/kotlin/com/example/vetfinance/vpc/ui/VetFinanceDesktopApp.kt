package com.example.vetfinance.vpc.ui

import com.example.vetfinance.vpc.backup.BackupService
import com.example.vetfinance.vpc.data.ClientSummary
import com.example.vetfinance.vpc.data.ProductSummary
import com.example.vetfinance.vpc.data.VpcDatabase
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingWorker
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

class VetFinanceDesktopApp(private val database: VpcDatabase) : JFrame("VetFinance PC") {
    private val backupService = BackupService(database)
    private val locale = Locale.Builder().setLanguage("es").setRegion("PY").build()
    private val money = NumberFormat.getCurrencyInstance(locale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)

    private val dashboardPanel = JPanel(GridBagLayout())
    private val inventorySearch = JTextField()
    private val clientSearch = JTextField()
    private val inventoryModel = readOnlyTableModel("ID", "Nombre", "Precio", "Costo", "Stock", "Tipo", "Categoria")
    private val clientsModel = readOnlyTableModel("ID", "Nombre", "Telefono", "Deuda")
    private val salesModel = readOnlyTableModel("ID", "Fecha", "Cliente", "Total", "Items")
    private val lowStockModel = readOnlyTableModel("ID", "Nombre", "Stock", "Categoria")
    private val backupStatus = JLabel("Base local: ${database.path}")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1120, 720)
        setLocationRelativeTo(null)

        contentPane = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(14, 14, 14, 14)
            add(header(), BorderLayout.NORTH)
            add(tabs(), BorderLayout.CENTER)
        }

        refreshAll()
    }

    private fun header(): JPanel {
        val title = JLabel("VetFinance PC").apply {
            font = font.deriveFont(Font.BOLD, 24f)
        }
        val subtitle = JLabel("Version de escritorio ligera para volumen alto y backups compartidos")
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 0, 12, 0)
            add(title, BorderLayout.WEST)
            add(subtitle, BorderLayout.SOUTH)
        }
    }

    private fun tabs(): JTabbedPane = JTabbedPane().apply {
        addTab("Inicio", dashboardTab())
        addTab("Inventario", inventoryTab())
        addTab("Clientes", clientsTab())
        addTab("Ventas", salesTab())
        addTab("Backups", backupTab())
    }

    private fun dashboardTab(): JPanel = JPanel(BorderLayout(10, 10)).apply {
        add(dashboardPanel, BorderLayout.NORTH)
        add(
            JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("Stock bajo")
                add(JScrollPane(JTable(lowStockModel)), BorderLayout.CENTER)
            },
            BorderLayout.CENTER
        )
    }

    private fun inventoryTab(): JPanel = JPanel(BorderLayout(10, 10)).apply {
        val table = JTable(inventoryModel).apply {
            autoCreateRowSorter = true
            rowHeight = 28
        }
        inventorySearch.onChange { refreshInventory() }
        add(
            toolbar(
                JLabel("Buscar producto o servicio:"),
                inventorySearch,
                JButton("Nuevo producto").apply { addActionListener { showProductDialog() } },
                JButton("Actualizar").apply { addActionListener { refreshAll() } }
            ),
            BorderLayout.NORTH
        )
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    private fun clientsTab(): JPanel = JPanel(BorderLayout(10, 10)).apply {
        val table = JTable(clientsModel).apply {
            autoCreateRowSorter = true
            rowHeight = 28
        }
        clientSearch.onChange { refreshClients() }
        add(
            toolbar(
                JLabel("Buscar cliente:"),
                clientSearch,
                JButton("Nuevo cliente").apply { addActionListener { showClientDialog() } },
                JButton("Actualizar").apply { addActionListener { refreshAll() } }
            ),
            BorderLayout.NORTH
        )
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    private fun salesTab(): JPanel = JPanel(BorderLayout(10, 10)).apply {
        val table = JTable(salesModel).apply {
            autoCreateRowSorter = true
            rowHeight = 28
        }
        add(
            toolbar(
                JButton("Registrar venta simple").apply { addActionListener { showSaleDialog() } },
                JButton("Actualizar").apply { addActionListener { refreshAll() } }
            ),
            BorderLayout.NORTH
        )
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    private fun backupTab(): JPanel = JPanel(BorderLayout(10, 10)).apply {
        val importButton = JButton("Importar ZIP y unir")
        val exportButton = JButton("Exportar ZIP compatible")

        importButton.addActionListener { importBackup() }
        exportButton.addActionListener { exportBackup() }

        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 10, 10)).apply {
                add(importButton)
                add(exportButton)
                add(JButton("Actualizar pantalla").apply { addActionListener { refreshAll() } })
            },
            BorderLayout.NORTH
        )
        add(
            JLabel(
                "<html><b>Sincronizacion por backup:</b><br>" +
                    "1. Exporta ZIP desde Android o PC.<br>" +
                    "2. Importalo en el otro equipo con 'Importar ZIP y unir'.<br>" +
                    "3. La importacion no borra registros locales ausentes en el ZIP.</html>"
            ).apply {
                border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
                verticalAlignment = SwingConstants.TOP
            },
            BorderLayout.CENTER
        )
        add(backupStatus, BorderLayout.SOUTH)
    }

    private fun refreshAll() {
        refreshDashboard()
        refreshInventory()
        refreshClients()
        refreshSales()
    }

    private fun refreshDashboard() {
        val stats = database.dashboardStats()
        dashboardPanel.removeAll()

        listOf(
            "Clientes" to stats.clients.toString(),
            "Items inventario" to stats.products.toString(),
            "Stock bajo" to stats.lowStock.toString(),
            "Ventas" to stats.sales.toString(),
            "Ingresos" to money.format(stats.salesTotal),
            "Deudas clientes" to money.format(stats.debtTotal)
        ).forEachIndexed { index, (label, value) ->
            val constraints = GridBagConstraints().apply {
                gridx = index
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(0, if (index == 0) 0 else 8, 8, 0)
            }
            dashboardPanel.add(metricCard(label, value), constraints)
        }

        val lowStock = database.lowStockProducts()
        lowStockModel.setRows(lowStock.map { arrayOf(it.productId, it.name, it.stock, it.category.orEmpty()) })

        dashboardPanel.revalidate()
        dashboardPanel.repaint()
    }

    private fun refreshInventory() {
        val rows = database.searchProducts(inventorySearch.text)
            .map {
                arrayOf<Any?>(
                    it.productId,
                    it.name,
                    money.format(it.price),
                    money.format(it.cost),
                    it.stock,
                    if (it.isService) "Servicio" else "Producto",
                    it.category.orEmpty()
                )
            }
        inventoryModel.setRows(rows)
    }

    private fun refreshClients() {
        val rows = database.searchClients(clientSearch.text)
            .map { arrayOf<Any?>(it.clientId, it.name, it.phone.orEmpty(), money.format(it.debtAmount)) }
        clientsModel.setRows(rows)
    }

    private fun refreshSales() {
        val rows = database.recentSales().map {
            arrayOf<Any?>(
                it[0],
                dateFormat.format(Date(it[1] as Long)),
                it[2],
                money.format(it[3] as Double),
                it[4]
            )
        }
        salesModel.setRows(rows)
    }

    private fun showClientDialog() {
        val name = JTextField()
        val phone = JTextField()
        val address = JTextField()
        val accepted = formDialog(
            title = "Nuevo cliente",
            fields = listOf("Nombre" to name, "Telefono" to phone, "Direccion" to address)
        )
        if (!accepted) return
        runCatching {
            require(name.text.isNotBlank()) { "El nombre es obligatorio." }
            database.insertClient(name.text.trim(), phone.text.trim().ifBlank { null }, address.text.trim().ifBlank { null })
        }.onSuccess {
            refreshAll()
        }.onFailure {
            showError(it.message ?: "No se pudo guardar el cliente.")
        }
    }

    private fun showProductDialog() {
        val name = JTextField()
        val price = JTextField("0")
        val cost = JTextField("0")
        val stock = JTextField("0")
        val category = JTextField()
        val isService = JCheckBox("Es servicio")

        val accepted = formDialog(
            title = "Nuevo producto o servicio",
            fields = listOf(
                "Nombre" to name,
                "Precio" to price,
                "Costo" to cost,
                "Stock" to stock,
                "Categoria" to category,
                "" to isService
            )
        )
        if (!accepted) return
        runCatching {
            require(name.text.isNotBlank()) { "El nombre es obligatorio." }
            database.insertProduct(
                name = name.text.trim(),
                price = price.text.toDoubleOrZero(),
                cost = cost.text.toDoubleOrZero(),
                stock = stock.text.toDoubleOrZero(),
                isService = isService.isSelected,
                category = category.text.trim().ifBlank { null }
            )
        }.onSuccess {
            refreshAll()
        }.onFailure {
            showError(it.message ?: "No se pudo guardar el producto.")
        }
    }

    private fun showSaleDialog() {
        val productSearch = JTextField()
        val productCombo = JComboBox<ProductSummary>()
        val clientCombo = JComboBox<ClientChoice>()
        val quantity = JTextField("1")

        fun refreshProductOptions() {
            productCombo.removeAllItems()
            database.searchProducts(productSearch.text, 50).forEach(productCombo::addItem)
        }

        fun refreshClientOptions() {
            clientCombo.removeAllItems()
            clientCombo.addItem(ClientChoice(null, "Venta directa"))
            database.searchClients("", 200).forEach { client -> clientCombo.addItem(ClientChoice(client.clientId, client.name)) }
        }

        refreshProductOptions()
        refreshClientOptions()
        productSearch.onChange { refreshProductOptions() }

        val accepted = formDialog(
            title = "Registrar venta simple",
            fields = listOf(
                "Buscar producto" to productSearch,
                "Producto" to productCombo,
                "Cliente" to clientCombo,
                "Cantidad" to quantity
            )
        )
        if (!accepted) return

        runCatching {
            val product = productCombo.selectedItem as? ProductSummary ?: error("Selecciona un producto.")
            val client = clientCombo.selectedItem as? ClientChoice
            val qty = quantity.text.toDoubleOrZero()
            require(qty > 0.0) { "La cantidad debe ser mayor a cero." }
            database.createCashSale(client?.clientId, product.productId, qty)
        }.onSuccess {
            refreshAll()
        }.onFailure {
            showError(it.message ?: "No se pudo registrar la venta.")
        }
    }

    private fun importBackup() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Importar backup ZIP"
            fileFilter = FileNameExtensionFilter("Backups ZIP", "zip")
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return

        runBackground(
            message = "Importando backup...",
            block = { backupService.importZip(chooser.selectedFile) },
            done = { result ->
                result.onSuccess { report ->
                    backupStatus.text = "Importacion completada: ${report.importedRows} filas en ${report.files} archivos."
                    refreshAll()
                }.onFailure {
                    showError(it.message ?: "No se pudo importar el backup.")
                }
            }
        )
    }

    private fun exportBackup() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Exportar backup ZIP"
            fileFilter = FileNameExtensionFilter("Backups ZIP", "zip")
            selectedFile = File("vetfinance-pc-backup-${System.currentTimeMillis()}.zip")
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return

        val target = chooser.selectedFile.ensureZipExtension()
        runBackground(
            message = "Exportando backup...",
            block = { backupService.exportToZip(target) },
            done = { result ->
                result.onSuccess { report ->
                    backupStatus.text = "Backup creado: ${target.absolutePath} (${report.exportedRows} filas)."
                }.onFailure {
                    showError(it.message ?: "No se pudo exportar el backup.")
                }
            }
        )
    }

    private fun <T> runBackground(message: String, block: () -> T, done: (Result<T>) -> Unit) {
        backupStatus.text = message
        object : SwingWorker<T, Unit>() {
            override fun doInBackground(): T = block()

            override fun done() {
                val result = runCatching { get() }
                done(result)
            }
        }.execute()
    }

    private fun formDialog(title: String, fields: List<Pair<String, java.awt.Component>>): Boolean {
        val dialog = JDialog(this, title, true)
        var accepted = false

        val form = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(14, 14, 14, 14)
        }
        fields.forEachIndexed { index, (label, field) ->
            form.add(
                JLabel(label),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = index
                    anchor = GridBagConstraints.WEST
                    insets = Insets(4, 4, 4, 10)
                }
            )
            form.add(
                field.apply { preferredSize = Dimension(280, preferredSize.height.coerceAtLeast(30)) },
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = index
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(4, 4, 4, 4)
                }
            )
        }

        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(JButton("Cancelar").apply { addActionListener { dialog.dispose() } })
            add(JButton("Guardar").apply {
                addActionListener {
                    accepted = true
                    dialog.dispose()
                }
            })
        }

        dialog.contentPane = JPanel(BorderLayout()).apply {
            add(form, BorderLayout.CENTER)
            add(buttons, BorderLayout.SOUTH)
        }
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
        return accepted
    }

    private fun metricCard(label: String, value: String): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(java.awt.Color(220, 224, 230)),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        )
        add(JLabel(label), BorderLayout.NORTH)
        add(
            JLabel(value).apply {
                font = font.deriveFont(Font.BOLD, 22f)
            },
            BorderLayout.CENTER
        )
    }

    private fun toolbar(vararg components: java.awt.Component): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
        components.forEach { add(it) }
    }

    private fun readOnlyTableModel(vararg headers: String): DefaultTableModel {
        return object : DefaultTableModel(headers, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
    }

    private fun DefaultTableModel.setRows(rows: List<Array<Any?>>) {
        rowCount = 0
        rows.forEach { addRow(it) }
    }

    private fun JTextField.onChange(action: () -> Unit) {
        document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = action()
                override fun removeUpdate(event: DocumentEvent) = action()
                override fun changedUpdate(event: DocumentEvent) = action()
            }
        )
    }

    private fun String.toDoubleOrZero(): Double {
        val normalized = trim()
        return if (normalized.contains(",")) {
            normalized.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        } else {
            normalized.toDoubleOrNull() ?: 0.0
        }
    }

    private fun File.ensureZipExtension(): File {
        return if (extension.equals("zip", ignoreCase = true)) this else File("$absolutePath.zip")
    }

    private fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "VetFinance PC", JOptionPane.ERROR_MESSAGE)
    }

    private data class ClientChoice(val clientId: String?, val label: String) {
        override fun toString(): String = label
    }
}
