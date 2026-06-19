package com.example.vetfinance.domain.rules

import com.example.vetfinance.data.SELLING_METHOD_BY_UNIT
import com.example.vetfinance.data.SELLING_METHOD_BY_WEIGHT_OR_AMOUNT
import com.example.vetfinance.data.SELLING_METHOD_DOSE_ONLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryRulesTest {

    @Test
    fun lowStockWithoutContainersUsesDefaultUnitThreshold() {
        val result = InventoryRules.isLowStockConsideringContainers(
            isService = false,
            isContainer = false,
            sellingMethod = SELLING_METHOD_BY_UNIT,
            stock = 3.0,
            lowStockThreshold = null,
            availableInClosedContainers = 0.0
        )

        assertTrue(result)
    }

    @Test
    fun containedProductIsNotLowStockWhenContainersCoverThreshold() {
        val result = InventoryRules.isLowStockConsideringContainers(
            isService = false,
            isContainer = false,
            sellingMethod = SELLING_METHOD_BY_UNIT,
            stock = 1.0,
            lowStockThreshold = 10.0,
            availableInClosedContainers = 12.0
        )

        assertFalse(result)
    }

    @Test
    fun doseAndServiceItemsAreNeverLowStockAlerts() {
        assertFalse(
            InventoryRules.isLowStockConsideringContainers(
                isService = true,
                isContainer = false,
                sellingMethod = SELLING_METHOD_BY_UNIT,
                stock = 0.0,
                lowStockThreshold = 10.0,
                availableInClosedContainers = 0.0
            )
        )
        assertFalse(
            InventoryRules.isLowStockConsideringContainers(
                isService = false,
                isContainer = false,
                sellingMethod = SELLING_METHOD_DOSE_ONLY,
                stock = 0.0,
                lowStockThreshold = 10.0,
                availableInClosedContainers = 0.0
            )
        )
    }

    @Test
    fun containerValidationRejectsSelfReference() {
        val error = InventoryRules.validateContainerConfiguration(
            productId = "product-1",
            isContainer = true,
            containedProductId = "product-1",
            containerSize = 10.0,
            containedProductExists = true,
            containedProductIsService = false,
            containedProductIsContainer = false,
            existingContainerIdForContainedProduct = null
        )

        assertEquals("Un producto contenedor no puede contenerse a si mismo.", error)
    }

    @Test
    fun containerValidationRejectsNestedContainer() {
        val error = InventoryRules.validateContainerConfiguration(
            productId = "container-1",
            isContainer = true,
            containedProductId = "container-2",
            containerSize = 10.0,
            containedProductExists = true,
            containedProductIsService = false,
            containedProductIsContainer = true,
            existingContainerIdForContainedProduct = null
        )

        assertEquals("Un contenedor no puede apuntar a otro contenedor.", error)
    }

    @Test
    fun containerValidationRejectsDuplicateContainer() {
        val error = InventoryRules.validateContainerConfiguration(
            productId = "container-1",
            isContainer = true,
            containedProductId = "product-1",
            containerSize = 10.0,
            containedProductExists = true,
            containedProductIsService = false,
            containedProductIsContainer = false,
            existingContainerIdForContainedProduct = "container-2"
        )

        assertEquals("Ya existe otro contenedor configurado para ese producto contenido.", error)
    }

    @Test
    fun validContainerConfigurationPasses() {
        val error = InventoryRules.validateContainerConfiguration(
            productId = "container-1",
            isContainer = true,
            containedProductId = "product-1",
            containerSize = 10.0,
            containedProductExists = true,
            containedProductIsService = false,
            containedProductIsContainer = false,
            existingContainerIdForContainedProduct = "container-1"
        )

        assertNull(error)
    }

    @Test
    fun weightedProductsWithoutExplicitThresholdDoNotAlert() {
        val result = InventoryRules.isLowStockConsideringContainers(
            isService = false,
            isContainer = false,
            sellingMethod = SELLING_METHOD_BY_WEIGHT_OR_AMOUNT,
            stock = 0.0,
            lowStockThreshold = null,
            availableInClosedContainers = 0.0
        )

        assertFalse(result)
    }
}
