package com.example.vetfinance.data

import java.util.UUID

// AÑADIDO: Nueva data class para el carrito, más flexible
data class CartItem(
    // Usamos un ID único para cada item en el carrito, especialmente para las dosis
    val cartItemId: String = UUID.randomUUID().toString(),
    val product: Product,
    var quantity: Double,
    val notes: String? = null,
    val overridePrice: Double? = null
)
