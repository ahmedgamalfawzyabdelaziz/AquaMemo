package com.ahmedgamal.aquamemo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ahmedgamal.aquamemo.data.dao.FilterDao
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.NotificationHistory

@Database(
    entities = [Filter::class, FilterChangeHistory::class, CandlePrice::class, NotificationHistory::class],
    version = 5,
    exportSchema = false
)
abstract class AquaMemoDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
}