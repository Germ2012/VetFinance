package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa un pago o abono realizado por un cliente para reducir su deuda.
 *
 * @property paymentId El identificador único para el pago, generado automáticamente.
 * @property clientIdFk La clave foránea que vincula el pago con el [Client] que lo realizó.
 * @property amountPaid El monto de dinero que fue pagado.
 * @property paymentDate La fecha y hora en que se registró el pago, almacenada como milisegundos.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.CASCADE // Si se borra un cliente, se borran sus pagos.
        )
    ],
    indices = [Index(value = ["clientIdFk"])]
)
data class Payment(
    @PrimaryKey
    val paymentId: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val amountPaid: Double,
    val paymentDate: Long = System.currentTimeMillis()
)