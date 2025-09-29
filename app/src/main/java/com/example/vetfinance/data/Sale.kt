package com.example.vetfinance.data

import androidx.room.*
import java.util.*
import com.example.vetfinance.data.Converters

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Sale(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val date: Date,
    val totalAmount: Double,
    val clientIdFk: String?
)

@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"],
    foreignKeys = [
        ForeignKey(entity = Sale::class, parentColumns = ["id"], childColumns = ["saleId"]),
        ForeignKey(entity = Product::class, parentColumns = ["id"], childColumns = ["productId"])
    ]
)
data class SaleProductCrossRef(
    val saleId: String,
    val productId: String,
    val quantity: Int,
    val priceAtTimeOfSale: Double,
    val isByFraction: Boolean,
    val amount: Double?
)

data class SaleWithProducts(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "id",
        entity = Product::class,
        associateBy = Junction(
            value = SaleProductCrossRef::class,
            parentColumn = "saleId",
            entityColumn = "productId"
        )
    )
    val products: List<Product>,
    @Relation(
parentColumn = "id",
entityColumn = "saleId"
)
val crossRefs: List<SaleProductCrossRef>

)