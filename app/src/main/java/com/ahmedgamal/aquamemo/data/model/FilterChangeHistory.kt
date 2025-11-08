// FilterChangeHistory.kt
package com.ahmedgamal.aquamemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_change_history")
data class FilterChangeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filterType: String,
    val candleNumber: Int,
    val changeDate: Long,
    val notes: String = ""
)