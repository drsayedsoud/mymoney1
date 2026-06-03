package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // INCOME or EXPENSE
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
