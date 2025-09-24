// ruta: app/src/main/java/com/example/vetfinance/data/Payment.kt

package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // Asegúrate de que este import exista
import androidx.room.PrimaryKey
import java.util.UUID
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
    // 👇 AÑADE ESTA LÍNEA PARA CREAR EL ÍNDICE Y RESOLVER LA ADVERTENCIA
    indices = [Index(value = ["clientIdFk"])]
)
data class Payment(
    @PrimaryKey
    val paymentId:  String = UUID.randomUUID().toString(),
    val clientIdFk: String, // Clave foránea para relacionarlo con el cliente
    val amountPaid: Double,
    val paymentDate: Long = System.currentTimeMillis()
)