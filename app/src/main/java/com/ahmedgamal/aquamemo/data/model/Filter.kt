// Filter.kt
package com.ahmedgamal.aquamemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filterType: String,
    val candleNumber: Int,
    val lastChangedDate: Long
)