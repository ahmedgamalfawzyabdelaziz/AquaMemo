package com.ahmedgamal.aquamemo.data

import com.ahmedgamal.aquamemo.data.dao.TdsDao
import com.ahmedgamal.aquamemo.data.model.TdsReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TdsRepository @Inject constructor(
    private val tdsDao: TdsDao
) {
    // 1. جلب كل القراءات (للعرض في القائمة - الأحدث أولاً)
    val allReadings: Flow<List<TdsReading>> = tdsDao.getAllReadings()

    // 2. جلب القراءات للرسم البياني (الأقدم أولاً - حسب التاريخ)
    val chartReadings: Flow<List<TdsReading>> = tdsDao.getReadingsForChart()

    // 3. جلب آخر قراءة فقط (لعرض الملخص السريع)
    val lastReading: Flow<TdsReading?> = tdsDao.getLastReading()

    // 4. إضافة قراءة جديدة
    suspend fun insertReading(value: Int, date: Long, notes: String = "") {
        val reading = TdsReading(value = value, date = date, notes = notes)
        tdsDao.insertReading(reading)
    }

    // 5. حذف قراءة
    suspend fun deleteReading(reading: TdsReading) {
        tdsDao.deleteReading(reading)
    }

    suspend fun deleteAllReadings() {
        tdsDao.deleteAllReadings()
    }
}