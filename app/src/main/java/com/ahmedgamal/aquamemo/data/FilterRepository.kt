package com.ahmedgamal.aquamemo.data

import android.util.Log
import com.ahmedgamal.aquamemo.data.dao.FilterDao
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FilterRepository @Inject constructor(
    private val filterDao: FilterDao
) {
    suspend fun insertFilter(filter: Filter) {
        filterDao.insert(filter)
    }

    suspend fun updateFilter(filter: Filter) {
        Log.d("FilterRepository", "جاري تحديث الفلتر: ${filter.filterType}-${filter.candleNumber}")
        filterDao.update(filter)
        Log.d("FilterRepository", "تم التحديث بنجاح")
    }

    fun getAllFilters(): Flow<List<Filter>> {
        return filterDao.getAllFilters()
    }

    fun getFiltersByType(filterType: String): Flow<List<Filter>> {
        return filterDao.getFiltersByType(filterType)
    }

    suspend fun getSingleFilter(filterType: String, candleNumber: Int): Filter? {
        return filterDao.getSingleFilter(filterType, candleNumber)
    }

    suspend fun deleteAllFilters() {
        filterDao.deleteAllFilters()
    }

    suspend fun insertFilterChange(change: FilterChangeHistory) {
        filterDao.insertFilterChange(change)
    }

    fun getFilterChanges(filterType: String, candleNumber: Int): Flow<List<FilterChangeHistory>> {
        return filterDao.getFilterChanges(filterType, candleNumber)
    }

    suspend fun deleteFilterChanges(filterType: String, candleNumber: Int) {
        filterDao.deleteFilterChanges(filterType, candleNumber)
    }

    fun getAllCandlePrices(): Flow<List<CandlePrice>> {
        return filterDao.getAllCandlePrices()
    }

    suspend fun updateCandlePrice(candleNumber: Int, price: Double) {
        filterDao.updateCandlePrice(candleNumber, price)
    }

    suspend fun initializeDefaultPrices() {
        val existingPrices = filterDao.getAllCandlePrices().first()
        if (existingPrices.isEmpty()) {
            CandlePrice.getDefaultPrices().forEach { price ->
                filterDao.insertCandlePrice(price)
            }
        }
    }

    suspend fun insertNotification(notification: NotificationHistory) {
        filterDao.insertNotification(notification)
    }

    fun getAllNotifications(): Flow<List<NotificationHistory>> {
        return filterDao.getAllNotifications()
    }

    suspend fun markAllNotificationsAsRead() {
        filterDao.markAllNotificationsAsRead()
    }

    suspend fun clearAllNotifications() {
        filterDao.clearAllNotifications()
    }
}