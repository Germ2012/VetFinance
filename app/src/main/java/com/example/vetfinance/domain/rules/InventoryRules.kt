package com.example.vetfinance.domain.rules

import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY

object InventoryRules {
    fun defaultThresholdFor(sellingMethod: String): Double {
        return if (sellingMethod == SELLING_METHOD_BY_UNIT) 4.0 else 0.0
    }

    fun isLowStockConsideringContainers(
        isService: Boolean,
        isContainer: Boolean,
        sellingMethod: String,
        stock: Double,
        lowStockThreshold: Double?,
        availableInClosedContainers: Double
    ): Boolean {
        if (isService || isContainer || sellingMethod == SELLING_METHOD_DOSE_ONLY) return false
        val threshold = lowStockThreshold ?: defaultThresholdFor(sellingMethod)
        if (threshold <= 0.0) return false
        return stock + availableInClosedContainers.coerceAtLeast(0.0) < threshold
    }

    fun validateContainerConfiguration(
        productId: String,
        isContainer: Boolean,
        containedProductId: String?,
        containerSize: Double?,
        containedProductExists: Boolean,
        containedProductIsService: Boolean,
        containedProductIsContainer: Boolean,
        existingContainerIdForContainedProduct: String?
    ): String? {
        if (!isContainer) return null

        val targetId = containedProductId?.takeIf { it.isNotBlank() }
            ?: return "Selecciona el producto contenido del contenedor."

        if (productId.isNotBlank() && targetId == productId) {
            return "Un producto contenedor no puede contenerse a si mismo."
        }

        if ((containerSize ?: 0.0) <= 0.0) {
            return "El tamano del contenedor debe ser mayor a cero."
        }

        if (!containedProductExists) {
            return "El producto contenido no existe."
        }

        if (containedProductIsService) {
            return "Un contenedor no puede apuntar a un servicio."
        }

        if (containedProductIsContainer) {
            return "Un contenedor no puede apuntar a otro contenedor."
        }

        if (existingContainerIdForContainedProduct != null && existingContainerIdForContainedProduct != productId) {
            return "Ya existe otro contenedor configurado para ese producto contenido."
        }

        return null
    }
}
