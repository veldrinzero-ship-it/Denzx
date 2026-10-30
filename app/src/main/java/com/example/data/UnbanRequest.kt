package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unban_requests")
data class UnbanRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val countryCode: String,
    val ticketId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int, // 60 or 600
    val appealsSentCount: Int,
    val status: String, // "PENDING", "PROCESSING", "RESTORED", "FAILED"
    val officialResponse: String
)
