package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID


@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val stock: Int,
    val isService: Boolean = false // Para diferenciar entre productos y servicios
)