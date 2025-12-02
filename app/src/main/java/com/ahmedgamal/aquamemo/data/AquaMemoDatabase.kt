package com.ahmedgamal.aquamemo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ahmedgamal.aquamemo.data.dao.FilterDao
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import com.ahmedgamal.aquamemo.data.dao.TdsDao
import com.ahmedgamal.aquamemo.data.model.TdsReading

@Database(
    entities = [Filter::class, FilterChangeHistory::class, CandlePrice::class, NotificationHistory::class, TdsReading::class],
    version = 6,
    exportSchema = false
)
abstract class AquaMemoDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
    abstract fun tdsDao(): TdsDao
}