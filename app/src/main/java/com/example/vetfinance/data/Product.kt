package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

const val SELLING_METHOD_BY_UNIT = "Por Unidad"
const val SELLING_METHOD_BY_WEIGHT_OR_AMOUNT = "Por Peso/Monto"
const val SELLING_METHOD_DOSE_ONLY = "Solo Dosis"

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val productId: String = UUID.randomUUID().toString(),
    val name: String,
    var price: Double,
    var cost: Double,
    var stock: Double,
    val isService: Boolean,
    var sellingMethod: String = SELLING_METHOD_BY_UNIT,
    var lowStockThreshold: Double? = null,
    val isContainer: Boolean = false, // Es un producto contenedor (ej. la bolsa)
    val containedProductId: String? = null, // ID del producto a granel que contiene
    val containerSize: Double? = null, // Tamaño del contenedor (ej. 25 para 25kg)
    val supplierIdFk: String? = null
)