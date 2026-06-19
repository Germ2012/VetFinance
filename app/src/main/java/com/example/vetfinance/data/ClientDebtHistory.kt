package com.example.vetfinance.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

const val CLIENT_DEBT_EVENT_INITIAL = "INITIAL"
const val CLIENT_DEBT_EVENT_PAYMENT = "PAYMENT"
const val CLIENT_DEBT_EVENT_ADJUSTMENT = "ADJUSTMENT"

@Entity(
    tableName = "client_debt_history",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientIdFk"), Index("eventDate")]
)
@Immutable
data class ClientDebtHistory(
    @PrimaryKey val historyId: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val eventDate: Long,
    val eventType: String,
    val amountChange: Double,
    val balanceAfter: Double,
    val note: String?
)
