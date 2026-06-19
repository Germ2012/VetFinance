package com.example.vetfinance.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleProductCrossRef(crossRef: SaleProductCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    @Upsert
    suspend fun upsertAllSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleProductCrossRefs(crossRefs: List<SaleProductCrossRef>)

    @Upsert
    suspend fun upsertAllSaleProductCrossRefs(crossRefs: List<SaleProductCrossRef>)

    @Query("DELETE FROM sales WHERE saleId = :saleId")
    suspend fun deleteSaleById(saleId: String)

    @Query("DELETE FROM sales_products_cross_ref WHERE saleId = :saleId")
    suspend fun deleteSaleProductCrossRefs(saleId: String)

    @Query("SELECT * FROM sales_products_cross_ref WHERE saleId = :saleId")
    suspend fun getSaleDetailsBySaleId(saleId: String): List<SaleProductCrossRef>

    @Query("""
        SELECT
            s.saleId AS saleId,
            s.date AS date,
            s.totalAmount AS totalAmount,
            s.clientIdFk AS clientIdFk,
            c.name AS clientName,
            COUNT(sp.crossRefId) AS itemCount
        FROM sales AS s
        LEFT JOIN clients AS c ON c.clientId = s.clientIdFk
        LEFT JOIN sales_products_cross_ref AS sp ON sp.saleId = s.saleId
        WHERE (:startDate IS NULL OR s.date >= :startDate)
            AND (:endDate IS NULL OR s.date <= :endDate)
        GROUP BY s.saleId, s.date, s.totalAmount, s.clientIdFk, c.name
        ORDER BY s.date DESC
    """)
    fun getSalesPagedSource(startDate: Long?, endDate: Long?): PagingSource<Int, SaleListItem>

    @Query("""
        SELECT
            COUNT(*) AS salesCount,
            COALESCE(SUM(totalAmount), 0.0) AS salesTotal
        FROM sales
        WHERE (:startDate IS NULL OR date >= :startDate)
            AND (:endDate IS NULL OR date <= :endDate)
    """)
    fun getSalesListSummary(startDate: Long?, endDate: Long?): Flow<CashClosingSalesRow>

    @Query("""
        SELECT
            periodKey,
            MIN(date) AS sampleDate,
            MAX(date) AS lastDate
        FROM (
            SELECT
                date,
                CASE
                    WHEN :periodType = 'DAY' THEN strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')
                    WHEN :periodType = 'WEEK' THEN strftime('%Y-%W', date / 1000, 'unixepoch', 'localtime')
                    ELSE strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')
                END AS periodKey
            FROM sales
        )
        GROUP BY periodKey
        ORDER BY lastDate DESC
        LIMIT :limit
    """)
    fun getSalePeriods(periodType: String, limit: Int = 365): Flow<List<SalePeriodRow>>

    @Query("""
        SELECT
            sp.crossRefId AS crossRefId,
            sp.productId AS productId,
            p.name AS productName,
            sp.quantitySold AS quantitySold,
            sp.priceAtTimeOfSale AS priceAtTimeOfSale,
            sp.notes AS notes,
            sp.overridePrice AS overridePrice
        FROM sales_products_cross_ref AS sp
        LEFT JOIN products AS p ON p.productId = sp.productId
        WHERE sp.saleId = :saleId
        ORDER BY p.name COLLATE NOCASE ASC
    """)
    fun getSaleDetailLines(saleId: String): Flow<List<SaleDetailLine>>

    @Query("""
        SELECT
            p.name,
            SUM(sp.quantitySold) as totalSold,
            SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)) as totalRevenue
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """)
    fun getTopSellingProductsByQuantity(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>

    @Query("""
        SELECT
            p.name,
            SUM(sp.quantitySold) as totalSold,
            SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)) as totalRevenue
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.name
        ORDER BY totalRevenue DESC
        LIMIT :limit
    """)
    fun getTopSellingProductsByRevenue(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<TopSellingProduct>>

    @Query("""
        SELECT
            CAST((date - :startDate) / 86400000 AS INTEGER) AS dayIndex,
            COALESCE(SUM(totalAmount), 0.0) AS totalSales
        FROM sales
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY dayIndex
        ORDER BY dayIndex ASC
    """)
    fun getDailySalesTotals(startDate: Long, endDate: Long): Flow<List<DailySalesTotal>>

    @Query("""
        SELECT
            COUNT(*) AS salesCount,
            COALESCE(SUM(totalAmount), 0.0) AS salesTotal
        FROM sales
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun getSalesTotalsForRange(startDate: Long, endDate: Long): Flow<CashClosingSalesRow>

    @Query("""
        SELECT
            COALESCE((
                SELECT SUM(totalAmount)
                FROM sales
                WHERE date BETWEEN :startDate AND :endDate
            ), 0.0) AS salesTotal,
            COALESCE((
                SELECT SUM(
                    COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)
                    - (COALESCE(p.cost, 0.0) * sp.quantitySold)
                )
                FROM sales_products_cross_ref AS sp
                JOIN sales AS s ON sp.saleId = s.saleId
                LEFT JOIN products AS p ON sp.productId = p.productId
                WHERE s.date BETWEEN :startDate AND :endDate
            ), 0.0) AS grossProfit
    """)
    fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummaryRow>

    @Query("""
        SELECT
            CASE
                WHEN p.isService = 1 THEN 'Servicios'
                WHEN p.category IS NULL OR TRIM(p.category) = '' THEN 'Sin categoria'
                ELSE p.category
            END AS category,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)), 0.0) AS revenue,
            COALESCE(SUM(COALESCE(p.cost, 0.0) * sp.quantitySold), 0.0) AS cost,
            COALESCE(SUM(
                COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)
                - (COALESCE(p.cost, 0.0) * sp.quantitySold)
            ), 0.0) AS profit
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY profit DESC
        LIMIT :limit
    """)
    fun getCategoryProfitRows(startDate: Long, endDate: Long, limit: Int = 8): Flow<List<CategoryProfitRow>>

    @Query("""
        SELECT p.*
        FROM products AS p
        INNER JOIN (
            SELECT productId, SUM(quantitySold) AS totalSold
            FROM sales_products_cross_ref
            GROUP BY productId
        ) AS ranked ON ranked.productId = p.productId
        ORDER BY ranked.totalSold DESC, p.name ASC
        LIMIT :limit
    """)
    fun getFrequentSaleProducts(limit: Int = 6): Flow<List<Product>>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name,
            p.isService,
            COALESCE(SUM(sp.quantitySold), 0.0) AS totalSold,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold)), 0.0) AS revenue,
            COALESCE(SUM(COALESCE(p.cost, 0.0) * sp.quantitySold), 0.0) AS cost,
            COALESCE(SUM(COALESCE(sp.overridePrice, sp.priceAtTimeOfSale * sp.quantitySold) - (COALESCE(p.cost, 0.0) * sp.quantitySold)), 0.0) AS profit
        FROM sales_products_cross_ref AS sp
        JOIN sales AS s ON sp.saleId = s.saleId
        JOIN products AS p ON sp.productId = p.productId
        WHERE s.date BETWEEN :startDate AND :endDate
        GROUP BY p.productId, p.name, p.isService
        ORDER BY profit DESC
    """)
    fun getProductProfitReports(startDate: Long, endDate: Long): Flow<List<ProductProfitReportRow>>

    @Query("SELECT * FROM sales")
    fun getAllSalesSimple(): Flow<List<Sale>>

    @Query("SELECT * FROM sales_products_cross_ref")
    fun getAllSaleProductCrossRefsSimple(): Flow<List<SaleProductCrossRef>>
}
