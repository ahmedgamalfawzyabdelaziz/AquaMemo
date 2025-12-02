package com.ahmedgamal.aquamemo.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ahmedgamal.aquamemo.billing.BillingManager
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.SettingsRepository
import com.ahmedgamal.aquamemo.data.TdsRepository
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.data.model.TdsReading
import com.ahmedgamal.aquamemo.ui.getCandleNameForWorker
import com.ahmedgamal.aquamemo.utils.LanguageManager
import com.ahmedgamal.aquamemo.worker.NotificationWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.work.ExistingWorkPolicy

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val filterRepository: FilterRepository,
    private val settingsRepository: SettingsRepository,
    val billingManager: BillingManager,
    private val tdsRepository: TdsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val selectedCurrency = settingsRepository.selectedCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "USD")

    fun updateSelectedCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedCurrency(currency)
        }
    }

    val allFilters: Flow<List<Filter>> = filterRepository.getAllFilters()
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val reminderTimeFlow: Flow<String> = settingsRepository.reminderTimeFlow
        .flowOn(Dispatchers.IO)

    val candlePrices: Flow<List<CandlePrice>> = filterRepository.getAllCandlePrices()

    init {
        viewModelScope.launch {
            filterRepository.initializeDefaultPrices()
        }
    }

    // ✅ هذه هي الدالة التي كانت مفقودة وتسببت في الأخطاء
    fun getFiltersByType(filterType: String): Flow<List<Filter>> {
        return filterRepository.getFiltersByType(filterType)
            .flowOn(Dispatchers.IO)
    }

    fun getFilterChanges(filterType: String, candleNumber: Int): Flow<List<FilterChangeHistory>> {
        return filterRepository.getFilterChanges(filterType, candleNumber)
    }

    suspend fun addFilterChange(change: FilterChangeHistory) {
        filterRepository.insertFilterChange(change)
    }

    fun saveFilter(filter: Filter) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                filterRepository.insertFilter(filter)
                val change = FilterChangeHistory(
                    filterType = filter.filterType,
                    candleNumber = filter.candleNumber,
                    changeDate = filter.lastChangedDate,
                    notes = getInitialChangeNote(context, filter.candleNumber)
                )
                filterRepository.insertFilterChange(change)
                scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                updateWidgetData()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving filter: ${e.message}")
            }
        }
    }

    fun updateFilter(filter: Filter) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldFilter = filterRepository.getSingleFilter(filter.filterType, filter.candleNumber)
                if (oldFilter != null && oldFilter.lastChangedDate != filter.lastChangedDate) {
                    val change = FilterChangeHistory(
                        filterType = filter.filterType,
                        candleNumber = filter.candleNumber,
                        changeDate = filter.lastChangedDate,
                        notes = getChangeNote(context, filter.candleNumber, oldFilter.lastChangedDate, filter.lastChangedDate)
                    )
                    filterRepository.insertFilterChange(change)
                }
                filterRepository.updateFilter(filter)
                WorkManager.getInstance(context).cancelAllWorkByTag("candle_${filter.candleNumber}")
                scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                updateWidgetData()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating filter: ${e.message}")
            }
        }
    }

    private fun getChangeNote(context: Context, candleNumber: Int, oldDate: Long, newDate: Long): String {
        val candleName = getCandleNameForWorker(candleNumber, context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return if (isArabicLanguage(context)) {
            "تم تغيير $candleName من ${dateFormat.format(Date(oldDate))} إلى ${dateFormat.format(Date(newDate))}"
        } else {
            "Changed $candleName from ${dateFormat.format(Date(oldDate))} to ${dateFormat.format(Date(newDate))}"
        }
    }

    fun updateCandlePrice(candleNumber: Int, price: Double) {
        viewModelScope.launch {
            filterRepository.updateCandlePrice(candleNumber, price)
        }
    }

    private fun getInitialChangeNote(context: Context, candleNumber: Int): String {
        val candleName = getCandleNameForWorker(candleNumber, context)
        return if (isArabicLanguage(context)) {
            "الإضافة الأولى لـ $candleName"
        } else {
            "Initial setup for $candleName"
        }
    }

    private fun isArabicLanguage(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val language = sharedPref.getString("language", "en") ?: "en"
        return language == "ar"
    }

    private fun scheduleNotification(candleNumber: Int, lastChangedDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminderTime = reminderTimeFlow.first()
                val intervalMonths = settingsRepository.getIntervalForCandle(candleNumber).first()
                if (intervalMonths <= 0) return@launch

                val nextChangeDate = Calendar.getInstance().apply {
                    timeInMillis = lastChangedDateMillis
                    add(Calendar.MONTH, intervalMonths)
                }.timeInMillis

                val reminderTimeParts = reminderTime.split(":")
                val reminderHour = reminderTimeParts[0].toIntOrNull() ?: 9
                val reminderMinute = reminderTimeParts[1].toIntOrNull() ?: 0

                val notificationTime = Calendar.getInstance().apply {
                    timeInMillis = nextChangeDate
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }.timeInMillis

                val delayMillis = notificationTime - System.currentTimeMillis()
                if (delayMillis > 0) {
                    val candleName = getCandleNameForWorker(candleNumber, context)
                    val inputData = Data.Builder()
                        .putInt("candleNumber", candleNumber)
                        .putString("candleName", candleName)
                        .build()
                    val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag("candle_$candleNumber")
                        .build()
                    WorkManager.getInstance(context).enqueue(notificationWorkRequest)
                } else {
                    showImmediateNotification(candleNumber)
                }
            } catch (e: Exception) {
                Log.e("Notification", "Error scheduling: ${e.message}")
            }
        }
    }

    private fun showImmediateNotification(candleNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val candleName = getCandleNameForWorker(candleNumber, context)
                val inputData = Data.Builder()
                    .putInt("candleNumber", candleNumber)
                    .putString("candleName", candleName)
                    .build()
                val immediateWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(inputData)
                    .build()
                WorkManager.getInstance(context).enqueue(immediateWorkRequest)
            } catch (e: Exception) {
                Log.e("Notification", "Error immediate: ${e.message}")
            }
        }
    }

    fun deleteAllAndResetState() {
        viewModelScope.launch(Dispatchers.IO) {
            filterRepository.deleteAllFilters()
            tdsRepository.deleteAllReadings()
            WorkManager.getInstance(context).cancelAllWork()
            updateWidgetData()
        }
    }

    // --- Backup & Restore Logic ---

    suspend fun getBackupData(): String {
        return withContext(Dispatchers.IO) {
            val filters = allFilters.first()
            val candlePrices = candlePrices.first()
            val settings = getSettingsData()
            val tdsReadings = tdsRepository.allReadings.first()

            val backupData = BackupData(
                filters = filters,
                candlePrices = candlePrices,
                settings = settings,
                tdsReadings = tdsReadings,
                backupDate = System.currentTimeMillis(),
                version = 3
            )
            val gson = Gson()
            gson.toJson(backupData)
        }
    }

    private suspend fun getSettingsData(): SettingsData {
        return withContext(Dispatchers.IO) {
            val remindersEnabled = settingsRepository.remindersEnabledFlow.first()
            val reminderTime = settingsRepository.reminderTimeFlow.first()
            val fontSize = settingsRepository.fontSizeFlow.first()
            val selectedCurrency = settingsRepository.selectedCurrencyFlow.first()
            val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val language = sharedPref.getString("language", "en") ?: "en"

            val intervals = mutableMapOf<String, Int>()
            for (i in 1..7) {
                val interval = settingsRepository.getIntervalForCandle(i).first()
                intervals[i.toString()] = interval
            }

            SettingsData(
                remindersEnabled = remindersEnabled,
                reminderTime = reminderTime,
                fontSize = fontSize,
                selectedCurrency = selectedCurrency,
                language = language,
                customIntervals = intervals
            )
        }
    }

    private val _restoreCompleted = MutableStateFlow(false)
    val restoreCompleted: StateFlow<Boolean> = _restoreCompleted

    fun restoreDataFromStream(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val gson = Gson()
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                if (backupData.filters.isNotEmpty()) {
                    filterRepository.deleteAllFilters()
                    backupData.filters.forEach { filter ->
                        filterRepository.insertFilter(filter)
                        scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                    }
                }

                if (backupData.candlePrices.isNotEmpty()) {
                    backupData.candlePrices.forEach { price ->
                        filterRepository.updateCandlePrice(price.candleNumber, price.price)
                    }
                }

                tdsRepository.deleteAllReadings()
                if (backupData.tdsReadings.isNotEmpty()) {
                    backupData.tdsReadings.forEach { reading ->
                        tdsRepository.insertReading(reading.value, reading.date, reading.notes)
                    }
                }

                restoreSettings(backupData.settings)
                updateWidgetData()

                withContext(Dispatchers.Main) {
                    _restoreCompleted.value = true
                }

            } catch (e: Exception) {
                Log.e("Restore", "Error restoring data: ${e.message}")
            }
        }
    }

    @SuppressLint("UseKtx")
    private suspend fun restoreSettings(settings: SettingsData) {
        settingsRepository.setRemindersEnabled(settings.remindersEnabled)
        settingsRepository.setReminderTime(settings.reminderTime)
        settingsRepository.setFontSize(settings.fontSize)
        settingsRepository.setSelectedCurrency(settings.selectedCurrency)

        if (settings.customIntervals.isNotEmpty()) {
            settings.customIntervals.forEach { (candleId, interval) ->
                settingsRepository.setIntervalForCandle(candleId.toInt(), interval)
            }
        }

        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPref.edit { putString("language", settings.language) }
        LanguageManager.setLanguage(context, settings.language)
    }

    fun clearAllChangeHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allFilters = allFilters.first()
                allFilters.forEach { filter ->
                    filterRepository.deleteFilterChanges(filter.filterType, filter.candleNumber)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error clearing history: ${e.message}")
            }
        }
    }

    fun rescheduleAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filters = filterRepository.getAllFilters().first()
                filters.forEach { filter ->
                    WorkManager.getInstance(context).cancelAllWorkByTag("candle_${filter.candleNumber}")
                    scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error rescheduling: ${e.message}")
            }
        }
    }

    // ✅ تحديث الويدجت باستخدام enqueueUniqueWork لحل مشكلة Binder Transaction
    private fun updateWidgetData() {
        Log.d("MainViewModel", "Requesting Widget Update...")
        val workRequest = OneTimeWorkRequestBuilder<com.ahmedgamal.aquamemo.widget.WidgetUpdateWorker>().build()
        WorkManager.getInstance(app.applicationContext).enqueueUniqueWork(
            "widget_update_unique_work",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    data class BackupData(
        val filters: List<Filter>,
        val candlePrices: List<CandlePrice>,
        val settings: SettingsData,
        val tdsReadings: List<TdsReading> = emptyList(),
        val backupDate: Long,
        val version: Int
    )

    data class SettingsData(
        val remindersEnabled: Boolean,
        val reminderTime: String,
        val fontSize: String,
        val language: String,
        val selectedCurrency: String,
        val customIntervals: Map<String, Int> = emptyMap()
    )
}