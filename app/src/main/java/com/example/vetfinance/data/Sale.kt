package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("clientIdFk")]
)
data class Sale(
    @PrimaryKey
    val saleId: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis()
)

data class SaleWithProducts(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "saleId",
        entityColumn = "id",
        associateBy = Junction(
            SaleProductCrossRef::class,
            parentColumn = "saleId",
            entityColumn = "productId")
    )
    val products: List<Product>
)
// 👇 AQUÍ ESTÁ LA CORRECCIÓN 👇
@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"],
    indices = [Index(value = ["productId"])]
)
data class SaleProductCrossRef(
    val saleId: String,
    val productId: String, // Product ID
    val quantity: Int,
    val priceAtTimeOfSale: Double // Precio al momento de la venta
)
