package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.Date // Asegúrate de que java.util.Date es la que usas
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
    ]
)
data class Sale(
    @PrimaryKey
    val saleId: String = UUID.randomUUID().toString(),
    val date: Date,
    val totalAmount: Double,
    val clientIdFk: String?
)

@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"],
    foreignKeys = [
        ForeignKey(entity = Sale::class, parentColumns = ["saleId"], childColumns = ["saleId"]),
        ForeignKey(entity = Product::class, parentColumns = ["productId"], childColumns = ["productId"])
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
        parentColumn = "saleId",
        entity = Product::class,
        associateBy = Junction(value = SaleProductCrossRef::class, parentColumn = "saleId", entityColumn = "productId")
    )
    val products: List<Product>,
    @Relation(
        parentColumn = "saleId",
        entityColumn = "saleId" // Esto debería referenciar SaleProductCrossRef.saleId
    )
    val crossRefs: List<SaleProductCrossRef>
)