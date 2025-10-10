package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

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
