package com.example.vetfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val supplierId: String = UUID.randomUUID().toString(),
    val name: String,
    val contactPerson: String?,
    val phone: String?,
    val email: String?
)