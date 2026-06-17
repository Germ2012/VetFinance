package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

const val SELLING_METHOD_BY_UNIT = "Por Unidad"
const val SELLING_METHOD_BY_WEIGHT_OR_AMOUNT = "Por Peso/Monto"
const val SELLING_METHOD_DOSE_ONLY = "Solo Dosis"

@Entity(
    tableName = "products",
    indices = [
        Index("name"),
        Index("category"),
        Index("supplierIdFk")
    ]
)
@Immutable
data class Product(
    @PrimaryKey
    val productId: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val cost: Double,
    val stock: Double,
    val isService: Boolean,
    val sellingMethod: String = SELLING_METHOD_BY_UNIT,
    val lowStockThreshold: Double? = null,
    val isContainer: Boolean = false,
    val containedProductId: String? = null,
    val containerSize: Double? = null,
    val supplierIdFk: String? = null,
    val category: String? = null,
    val unitMeasure: String? = null
)
