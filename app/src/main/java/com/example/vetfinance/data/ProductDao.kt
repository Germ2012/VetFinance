package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Upsert
    suspend fun upsertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isService = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getServices(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE productId = :productId")
    suspend fun getProductById(productId: String): Product?

    @Query("SELECT * FROM products WHERE productId = :productId")
    fun getProductByIdFlow(productId: String): Flow<Product?>

    @Query("""
        SELECT *
        FROM products AS p
        WHERE
            (
                :filterType = 'Todos'
                OR (:filterType = 'Productos' AND p.isService = 0 AND p.sellingMethod != 'Solo Dosis')
                OR (:filterType = 'Servicios' AND p.isService = 1)
                OR (:filterType = 'Dosis' AND p.isService = 0 AND p.sellingMethod = 'Solo Dosis')
                OR (:filterType = 'Bajo stock' AND p.isService = 0 AND p.isContainer = 0 AND p.sellingMethod != 'Solo Dosis'
                    AND COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
                    AND (
                        p.stock + COALESCE((
                            SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                            FROM products AS c
                            WHERE c.isContainer = 1
                                AND c.containedProductId = p.productId
                                AND c.stock > 0.0
                                AND COALESCE(c.containerSize, 0.0) > 0.0
                        ), 0.0)
                    ) < COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END))
            )
        ORDER BY p.name ASC
    """)
    fun getProductsPagedSource(filterType: String): PagingSource<Int, Product>

    @RawQuery(observedEntities = [Product::class])
    fun searchProductsPagedSource(query: SupportSQLiteQuery): PagingSource<Int, Product>

    @RawQuery(observedEntities = [Product::class])
    fun searchProductSuggestions(query: SupportSQLiteQuery): Flow<List<Product>>

    @Query("""
        SELECT *
        FROM products
        WHERE isService = 0
            AND isContainer = 0
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun getContainedProductCandidates(limit: Int): Flow<List<Product>>

    @RawQuery(observedEntities = [Product::class])
    fun searchContainedProductCandidates(query: SupportSQLiteQuery): Flow<List<Product>>

    @Query("SELECT SUM(cost * stock) FROM products WHERE isService = 0")
    fun getTotalInventoryValue(): Flow<Double?>

    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                    WHEN COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
                        AND (
                            p.stock + COALESCE((
                                SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                                FROM products AS c
                                WHERE c.isContainer = 1
                                    AND c.containedProductId = p.productId
                                    AND c.stock > 0.0
                                    AND COALESCE(c.containerSize, 0.0) > 0.0
                            ), 0.0)
                        ) < COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
                    THEN 1 ELSE 0
                END
            ), 0) AS lowStockCount,
            COALESCE(SUM(
                CASE
                    WHEN COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) <= 0.0
                        OR (
                            p.stock + COALESCE((
                                SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                                FROM products AS c
                                WHERE c.isContainer = 1
                                    AND c.containedProductId = p.productId
                                    AND c.stock > 0.0
                                    AND COALESCE(c.containerSize, 0.0) > 0.0
                            ), 0.0)
                        ) >= COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
                    THEN 1 ELSE 0
                END
            ), 0) AS optimalCount
        FROM products AS p
        WHERE p.isService = 0
            AND p.isContainer = 0
            AND p.sellingMethod != 'Solo Dosis'
    """)
    fun getStockHealthSummary(): Flow<StockHealthRow>

    @Query("""
        SELECT
            COUNT(*) AS totalCount,
            COALESCE(SUM(CASE WHEN isService = 0 THEN 1 ELSE 0 END), 0) AS productCount,
            COALESCE(SUM(CASE WHEN isService = 1 THEN 1 ELSE 0 END), 0) AS serviceCount,
            COALESCE(SUM(
                CASE
                    WHEN p.isService = 0
                        AND p.isContainer = 0
                        AND p.sellingMethod != 'Solo Dosis'
                        AND COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
                        AND (
                            p.stock + COALESCE((
                                SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                                FROM products AS c
                                WHERE c.isContainer = 1
                                    AND c.containedProductId = p.productId
                                    AND c.stock > 0.0
                                    AND COALESCE(c.containerSize, 0.0) > 0.0
                            ), 0.0)
                        ) < COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
                    THEN 1 ELSE 0
                END
            ), 0) AS lowStockCount
        FROM products AS p
    """)
    fun getInventoryCounts(): Flow<InventoryCountsRow>

    @Query("""
        SELECT *
        FROM products AS p
        WHERE p.isService = 0
            AND p.isContainer = 0
            AND p.sellingMethod != 'Solo Dosis'
            AND COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
            AND (
                p.stock + COALESCE((
                    SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                    FROM products AS c
                    WHERE c.isContainer = 1
                        AND c.containedProductId = p.productId
                        AND c.stock > 0.0
                        AND COALESCE(c.containerSize, 0.0) > 0.0
                ), 0.0)
            ) < COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
        ORDER BY p.name COLLATE NOCASE ASC
    """)
    fun getLowStockProductsByName(): Flow<List<Product>>

    @Query("""
        SELECT p.productId
        FROM products AS p
        WHERE p.isService = 0
            AND p.isContainer = 0
            AND p.sellingMethod != 'Solo Dosis'
            AND COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END) > 0.0
            AND (
                p.stock + COALESCE((
                    SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                    FROM products AS c
                    WHERE c.isContainer = 1
                        AND c.containedProductId = p.productId
                        AND c.stock > 0.0
                        AND COALESCE(c.containerSize, 0.0) > 0.0
                ), 0.0)
            ) < COALESCE(p.lowStockThreshold, CASE WHEN p.sellingMethod = 'Por Unidad' THEN 4.0 ELSE 0.0 END)
    """)
    fun getLowStockProductIds(): Flow<List<String>>

    @Query("""
        SELECT
            COUNT(*) AS productCount,
            COALESCE(SUM(
                CASE
                    WHEN COALESCE(p.lowStockThreshold, 0.0) > 0.0
                        AND (
                            p.stock + COALESCE((
                                SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                                FROM products AS c
                                WHERE c.isContainer = 1
                                    AND c.containedProductId = p.productId
                                    AND c.stock > 0.0
                                    AND COALESCE(c.containerSize, 0.0) > 0.0
                            ), 0.0)
                        ) < COALESCE(p.lowStockThreshold, 0.0)
                    THEN 1 ELSE 0
                END
            ), 0) AS lowStockCount,
            COALESCE(SUM(p.stock), 0.0) AS totalUnits
        FROM products AS p
        WHERE p.isService = 0
    """)
    fun getInventoryReportSummary(): Flow<InventoryReportSummaryRow>

    @Query("""
        SELECT *
        FROM products AS p
        WHERE p.isService = 0
        ORDER BY
            CASE
                WHEN COALESCE(p.lowStockThreshold, 0.0) > 0.0
                    AND (
                        p.stock + COALESCE((
                            SELECT SUM(c.stock * COALESCE(c.containerSize, 0.0))
                            FROM products AS c
                            WHERE c.isContainer = 1
                                AND c.containedProductId = p.productId
                                AND c.stock > 0.0
                                AND COALESCE(c.containerSize, 0.0) > 0.0
                        ), 0.0)
                    ) < COALESCE(p.lowStockThreshold, 0.0)
                THEN 0 ELSE 1
            END,
            p.name COLLATE NOCASE ASC
    """)
    fun getInventoryReportProductsPagedSource(): PagingSource<Int, Product>

    @Query("UPDATE products SET stock = stock + :quantity WHERE productId = :productId")
    suspend fun incrementStock(productId: String, quantity: Double): Int

    @Query("UPDATE products SET stock = stock - :quantity WHERE productId = :productId AND stock >= :quantity")
    suspend fun decrementStockIfEnough(productId: String, quantity: Double): Int

    @Query("UPDATE products SET stock = stock + :quantity, cost = :cost WHERE productId = :productId")
    suspend fun incrementStockAndSetCost(productId: String, quantity: Double, cost: Double): Int

    @Query("SELECT stock FROM products WHERE productId = :productId")
    suspend fun getStockById(productId: String): Double?

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT COUNT(*) FROM sales_products_cross_ref WHERE productId = :productId")
    suspend fun countSaleDetailsForProduct(productId: String): Int

    @Query("SELECT * FROM products WHERE isContainer = 1 AND containedProductId = :containedProductId LIMIT 1")
    suspend fun findContainerForProduct(containedProductId: String): Product?
}
