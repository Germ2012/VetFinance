package com.example.vetfinance.vpc

import com.example.vetfinance.vpc.backup.BackupService
import com.example.vetfinance.vpc.data.VpcDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupMergeTest {
    @Test
    fun importZipAddsRowsWithoutDeletingLocalRows() {
        val dir = Files.createTempDirectory("vetfinance-pc-test")
        val source = VpcDatabase.open(dir.resolve("source.db"))
        val target = VpcDatabase.open(dir.resolve("target.db"))
        val backup = dir.resolve("backup.zip").toFile()

        source.use { sourceDb ->
            sourceDb.insertClient("Cliente Android", "111", null)
            sourceDb.insertProduct("Vacuna", 50000.0, 25000.0, 10.0, false, "Farmacia")
            BackupService(sourceDb).exportToZip(backup)
        }

        target.use { targetDb ->
            targetDb.insertClient("Cliente PC", "222", null)
            assertEquals(1, targetDb.tableCount("clients"))

            BackupService(targetDb).importZip(backup)

            assertEquals(2, targetDb.tableCount("clients"))
            assertEquals(1, targetDb.tableCount("products"))
        }
    }
}
