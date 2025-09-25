package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa a un cliente en la base de datos.
 *
 * @property clientId El identificador único para el cliente, generado automáticamente.
 * @property name El nombre completo del cliente.
 * @property phone El número de teléfono del cliente (opcional).
 * @property debtAmount La cantidad de deuda pendiente que tiene el cliente. El valor por defecto es 0.0.
 */
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val clientId: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val debtAmount: Double = 0.0
)