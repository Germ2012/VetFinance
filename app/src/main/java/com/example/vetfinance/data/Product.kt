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
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var price: Double,
    var cost: Double,
    var stock: Double,
    val isService: Boolean,
    var sellingMethod: String = SELLING_METHOD_BY_UNIT
)