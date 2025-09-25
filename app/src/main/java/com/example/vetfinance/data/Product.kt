package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa un item del inventario, que puede ser un producto físico o un servicio.
 *
 * @property id El identificador único para el producto/servicio, generado automáticamente.
 * @property name El nombre del producto o servicio.
 * @property price El precio de venta.
 * @property stock La cantidad disponible en inventario. Para servicios, este campo no se utiliza y puede tener un valor por defecto.
 * @property isService Un booleano que diferencia entre productos y servicios. `true` si es un servicio, `false` si es un producto físico.
 * @property cost El costo de adquisición del producto.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val stock: Int,
    val cost: Double, // Nuevo campo para el costo
    val isService: Boolean = false
)