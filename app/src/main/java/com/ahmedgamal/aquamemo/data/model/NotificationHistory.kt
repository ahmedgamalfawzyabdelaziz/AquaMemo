package com.ahmedgamal.aquamemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
data class NotificationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // "LOCAL_CANDLE" أو "REMOTE_ADMIN"
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val iconType: String // "CANDLE" أو "INFO" أو "WARNING"
)