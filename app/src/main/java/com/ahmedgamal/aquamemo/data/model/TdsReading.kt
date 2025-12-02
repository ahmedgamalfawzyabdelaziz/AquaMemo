package com.ahmedgamal.aquamemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tds_readings")
data class TdsReading(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val value: Int, // قيمة الأملاح (TDS)
    val date: Long, // تاريخ القياس
    val notes: String = "" // ملاحظات اختيارية
)