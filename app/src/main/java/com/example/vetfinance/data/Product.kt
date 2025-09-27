package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Definir constantes para los métodos de venta
const val SELLING_METHOD_BY_UNIT = "Por Unidad"
const val SELLING_METHOD_BY_WEIGHT_OR_AMOUNT = "Por Peso/Monto"
const val SELLING_METHOD_DOSE_ONLY = "Solo Dosis"

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    var price: Double,
    var cost: Double,
    var stock: Int,
    val isService: Boolean,
    var sellingMethod: String = SELLING_METHOD_BY_UNIT
)