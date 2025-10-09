package com.example.vetfinance.data

import java.util.UUID


data class CartItem(
    val cartItemId: String = UUID.randomUUID().toString(),
    val product: Product,
    var quantity: Double,
    val notes: String? = null,
    val overridePrice: Double? = null
)
