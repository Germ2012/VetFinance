package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import java.util.UUID


@Immutable
data class CartItem(
    val cartItemId: String = UUID.randomUUID().toString(),
    val product: Product,
    val quantity: Double,
    val notes: String? = null,
    val overridePrice: Double? = null
)
