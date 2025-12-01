// MainViewModel.kt
package com.ahmedgamal.aquamemo.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.model.Filter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import com.ahmedgamal.aquamemo.worker.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.Gson
import java.io.InputStream
import java.util.Calendar
import com.ahmedgamal.aquamemo.data.SettingsRepository
import com.ahmedgamal.aquamemo.data.model.CandlePrice
import com.ahmedgamal.aquamemo.data.model.FilterChangeHistory
import com.ahmedgamal.aquamemo.ui.getCandleNameForWorker
import com.ahmedgamal.aquamemo.utils.LanguageManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.stateIn
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.app.Application
import androidx.work.ExistingWorkPolicy
import com.ahmedgamal.aquamemo.billing.BillingManager

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val filterRepository: FilterRepository,
    private val settingsRepository: SettingsRepository,
    val billingManager: BillingManager,
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
    private fun getChangeNote(
        context: Context,
        candleNumber: Int,
        oldDate: Long,
        newDate: Long
    ): String {
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
    fun getFiltersByType(filterType: String): Flow<List<Filter>> {
        return filterRepository.getFiltersByType(filterType)
            .flowOn(Dispatchers.IO)
    }
    // 🔽 **الدالة المحسنة لجدولة الإشعارات**
    private fun scheduleNotification(candleNumber: Int, lastChangedDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminderTime = reminderTimeFlow.first()
                val intervalMonths = settingsRepository.getIntervalForCandle(candleNumber).first()
                if (intervalMonths <= 0) {
                    Log.w("Notification", "تم إلغاء جدولة الشمعة $candleNumber لأن مدتها 0.")
                    return@launch // لا تقم بجدولة إشعار
                }
                // حساب موعد التغيير القادم
                val nextChangeDate = Calendar.getInstance().apply {
                    timeInMillis = lastChangedDateMillis
                    add(Calendar.MONTH, intervalMonths)
                }.timeInMillis
                // تحليل وقت التذكير
                val reminderTimeParts = reminderTime.split(":")
                val reminderHour = reminderTimeParts[0].toIntOrNull() ?: 9
                val reminderMinute = reminderTimeParts[1].toIntOrNull() ?: 0
                // تحديد وقت الإشعار الدقيق
                val notificationTime = Calendar.getInstance().apply {
                    timeInMillis = nextChangeDate
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    // إذا فات وقت الإشعار اليوم، نضيف يوم
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
                    Log.d("Notification", "تم جدولة إشعار للشمعة $candleNumber بعد ${delayMillis / (1000 * 60 * 60)} ساعة")
                } else {
                    // إذا فات الوقت، نعرض إشعار فوري
                    showImmediateNotification(candleNumber)
                }
            } catch (e: Exception) {
                Log.e("Notification", "خطأ في جدولة الإشعار: ${e.message}")
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

                Log.d("Notification", "تم إرسال إشعار فوري للشمعة $candleNumber")
            } catch (e: Exception) {
                Log.e("Notification", "خطأ في الإشعار الفوري: ${e.message}")
            }
        }
    }
    fun deleteAllAndResetState() {
        viewModelScope.launch(Dispatchers.IO) {
            filterRepository.deleteAllFilters()
            WorkManager.getInstance(context).cancelAllWork()
            updateWidgetData()
        }
    }
    suspend fun getBackupData(): String {
        return withContext(Dispatchers.IO) {
            val filters = allFilters.first()
            val candlePrices = candlePrices.first()
            val settings = getSettingsData()
            val backupData = BackupData(
                filters = filters,
                candlePrices = candlePrices,
                settings = settings,
                backupDate = System.currentTimeMillis(),
                version = 1
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
            SettingsData(
                remindersEnabled = remindersEnabled,
                reminderTime = reminderTime,
                fontSize = fontSize,
                selectedCurrency = selectedCurrency,
                language = language
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

                // ✅ استرجاع الفلاتر
                if (backupData.filters.isNotEmpty()) {
                    filterRepository.deleteAllFilters()
                    backupData.filters.forEach { filter ->
                        filterRepository.insertFilter(filter)
                        scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                    }
                }

                // ✅ استرجاع أسعار الشمعات
                if (backupData.candlePrices.isNotEmpty()) {
                    backupData.candlePrices.forEach { price ->
                        filterRepository.updateCandlePrice(price.candleNumber, price.price)
                    }
                }

                // ✅ استرجاع الإعدادات (بما فيها اللغة)
                restoreSettings(backupData.settings)
                updateWidgetData()

                // ✅ إرسال إشارة للـ UI إن الاستعادة خلصت
                withContext(Dispatchers.Main) {
                    _restoreCompleted.value = true
                }

            } catch (e: Exception) {
                Log.e("Restore", "خطأ في استعادة البيانات: ${e.message}")
            }
        }
    }

    @SuppressLint("UseKtx")
    private suspend fun restoreSettings(settings: SettingsData) {
        // ✅ استرجاع الإعدادات العادية من DataStore
        settingsRepository.setRemindersEnabled(settings.remindersEnabled)
        settingsRepository.setReminderTime(settings.reminderTime)
        settingsRepository.setFontSize(settings.fontSize)
        settingsRepository.setSelectedCurrency(settings.selectedCurrency)
        // ✅ استرجاع اللغة من الـ Backup وتخزينها فى SharedPreferences
        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPref.edit { putString("language", settings.language) }
        // ✅ تطبيق اللغة فورياً باستخدام LanguageManager
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
                // 1. جلب جميع الفلاتر المسجلة
                val filters = filterRepository.getAllFilters().first()

                filters.forEach { filter ->
                    // 2. إلغاء أي إشعار قديم مجدول لهذه الشمعة
                    WorkManager.getInstance(context).cancelAllWorkByTag("candle_${filter.candleNumber}")

                    // 3. جدولة إشعار جديد بناءً على آخر تاريخ تغيير
                    scheduleNotification(filter.candleNumber, filter.lastChangedDate)
                }
                Log.d("MainViewModel", "All notifications have been rescheduled.")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error rescheduling notifications: ${e.message}")
            }
        }
    }
    private fun updateWidgetData() {
        Log.d("MainViewModel", "Enqueuing WidgetUpdateWorker...") // <-- Add this log
        val workRequest = OneTimeWorkRequestBuilder<com.ahmedgamal.aquamemo.widget.WidgetUpdateWorker>().build()
        WorkManager.getInstance(app.applicationContext).enqueueUniqueWork(
            "widget_update_unique_work", // اسم فريد للعملية
            ExistingWorkPolicy.REPLACE,  // استبدال أي عملية قديمة معلقة
            workRequest
        )
    }
    data class BackupData(
        val filters: List<Filter>,
        val candlePrices: List<CandlePrice>,
        val settings: SettingsData,
        val backupDate: Long,
        val version: Int
    )
    data class SettingsData(
        val remindersEnabled: Boolean,
        val reminderTime: String,
        val fontSize: String,
        val language: String,
        val selectedCurrency: String
    )
}