package com.ahmedgamal.aquamemo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: Filter)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(filter: Filter)

    @Query("SELECT * FROM filters")
    fun getAllFilters(): Flow<List<Filter>>

    @Query("SELECT * FROM filters WHERE filterType = :filterType")
    fun getFiltersByType(filterType: String): Flow<List<Filter>>

    @Query("SELECT * FROM filters WHERE filterType = :filterType AND candleNumber = :candleNumber")
    suspend fun getSingleFilter(filterType: String, candleNumber: Int): Filter?

    @Query("DELETE FROM filters")
    suspend fun deleteAllFilters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilterChange(change: FilterChangeHistory)

    @Query("SELECT * FROM filter_change_history WHERE filterType = :filterType AND candleNumber = :candleNumber ORDER BY changeDate DESC")
    fun getFilterChanges(filterType: String, candleNumber: Int): Flow<List<FilterChangeHistory>>

    @Query("DELETE FROM filter_change_history WHERE filterType = :filterType AND candleNumber = :candleNumber")
    suspend fun deleteFilterChanges(filterType: String, candleNumber: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandlePrice(candlePrice: CandlePrice)

    @Query("SELECT * FROM candle_prices")
    fun getAllCandlePrices(): Flow<List<CandlePrice>>

    @Query("UPDATE candle_prices SET price = :price WHERE candleNumber = :candleNumber")
    suspend fun updateCandlePrice(candleNumber: Int, price: Double)

    @Query("UPDATE candle_prices SET currency = :currency")
    suspend fun updateAllCurrencies(currency: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationHistory)

    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationHistory>>

    @Query("UPDATE notification_history SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM notification_history")
    suspend fun clearAllNotifications()
}