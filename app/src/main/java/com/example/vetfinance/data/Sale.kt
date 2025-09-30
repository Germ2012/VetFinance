package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // AÑADIDO: Índice para la llave foránea
    indices = [Index("clientIdFk")]
)
data class Sale(
    @PrimaryKey
    val saleId: String = UUID.randomUUID().toString(),
    val date: Long,
    val totalAmount: Double,
    val clientIdFk: String?
)

@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"],
    foreignKeys = [
        ForeignKey(entity = Sale::class, parentColumns = ["saleId"], childColumns = ["saleId"]),
        ForeignKey(entity = Product::class, parentColumns = ["productId"], childColumns = ["productId"])
    ],
    // AÑADIDO: Índice para la columna de la relación
    indices = [Index("productId")]
)
data class SaleProductCrossRef(
    val saleId: String,
    val productId: String,
    val quantitySold: Double,
    val priceAtTimeOfSale: Double,
    val notes: String? = null,
    val overridePrice: Double? = null
)

data class SaleWithProducts(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "saleId",
        entityColumn = "productId",
        associateBy = Junction(SaleProductCrossRef::class)
    )
    val products: List<Product>,
    @Relation(
        parentColumn = "saleId",
        entityColumn = "saleId"
    )
    val crossRefs: List<SaleProductCrossRef>
)