package com.ahmedgamal.aquamemo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmedgamal.aquamemo.data.model.TdsReading
import kotlinx.coroutines.flow.Flow

@Dao
interface TdsDao {
    @Query("SELECT * FROM tds_readings ORDER BY date DESC")
    fun getAllReadings(): Flow<List<TdsReading>>

    @Query("SELECT * FROM tds_readings ORDER BY date ASC")
    fun getReadingsForChart(): Flow<List<TdsReading>> // للرسم البياني (تصاعدي)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: TdsReading)

    @Delete
    suspend fun deleteReading(reading: TdsReading)

    @Query("SELECT * FROM tds_readings ORDER BY date DESC LIMIT 1")
    fun getLastReading(): Flow<TdsReading?>

    @Query("DELETE FROM tds_readings")
    suspend fun deleteAllReadings()
}