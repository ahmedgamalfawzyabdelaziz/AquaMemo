package com.ahmedgamal.aquamemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedgamal.aquamemo.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Application
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ahmedgamal.aquamemo.billing.BillingManager
import kotlinx.coroutines.flow.Flow
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.worker.NotificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    private val filterRepository: FilterRepository,
    private val repository: SettingsRepository,
    val billingManager: BillingManager
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = settingsRepository.remindersEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderTime: StateFlow<String> = settingsRepository.reminderTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "09:00")

    val fontSize: StateFlow<String> = settingsRepository.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "medium")

    val themePreference: StateFlow<String> = settingsRepository.themePreferenceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val isPro: StateFlow<Boolean> = billingManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val technicianPhone = repository.technicianPhone.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun saveTechnicianPhone(phone: String) {
        viewModelScope.launch {
            repository.saveTechnicianPhone(phone)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
        }
    }
    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
        }
    }
    fun setFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsRepository.setFontSize(fontSize)
        }
    }
    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
            updateWidgetData()
        }
    }
    fun getIntervalForCandle(candleNumber: Int): Flow<Int> {
        return settingsRepository.getIntervalForCandle(candleNumber)
    }
    fun setIntervalForCandle(candleNumber: Int, months: Int) {
        viewModelScope.launch {
            settingsRepository.setIntervalForCandle(candleNumber, months)
            // تحديث الإشعارات بعد تغيير مدة شمعة واحدة
            rescheduleNotificationsForCandle(candleNumber)
        }
    }

    fun resetAllIntervalsToDefault() {
        viewModelScope.launch {
            settingsRepository.resetAllIntervalsToDefault()
            // تحديث *كل* الإشعارات بعد استعادة الافتراضي
            rescheduleAllNotifications()
        }
    }

    private fun updateWidgetData() {
        val workRequest = OneTimeWorkRequestBuilder<com.ahmedgamal.aquamemo.widget.WidgetUpdateWorker>().build()
        WorkManager.getInstance(app.applicationContext).enqueue(workRequest)
    }

    /**
     * إعادة جدولة إشعارات شمعة معينة (بعد تعديل مدتها)
     */
    private fun rescheduleNotificationsForCandle(candleNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. (معدل) ابحث عن كل الفلاتر التي تستخدم رقم الشمعة هذا
            val allFilters = filterRepository.getAllFilters().first()
            val filtersToReschedule = allFilters.filter { it.candleNumber == candleNumber }

            filtersToReschedule.forEach { filter ->
                // 2. (معدل) إلغاء وجدولة بناءً على الـ ID الفريد للفلتر
                cancelNotification(filter.id)
                scheduleNotification(filter.id, filter.candleNumber, filter.lastChangedDate)
            }
        }
    }

    /**
     * إعادة جدولة *كل* الإشعارات (بعد استعادة الافتراضي)
     */
    private fun rescheduleAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            // أولاً: إلغاء *كل* الإشعارات القديمة (بناءً على التاج العام)
            cancelAllNotifications()

            // ثانياً: جدولة إشعارات جديدة لكل الفلاتر
            val allFilters = filterRepository.getAllFilters().first()
            allFilters.forEach { filter ->
                // 3. (معدل) جدولة بناءً على الـ ID الفريد
                scheduleNotification(filter.id, filter.candleNumber, filter.lastChangedDate)
            }
        }
    }

    /**
     * دالة لجدولة إشعار واحد (مبنية على لوجيك MainViewModel)
     */
    private fun scheduleNotification(filterId: Int, candleNumber: Int, lastChangedDateMillis: Long) { // 4. (معدل) استقبال filterId
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // قراءة الإعدادات
                val remindersEnabled = remindersEnabled.first()
                if (!remindersEnabled) {
                    Log.d("Notification", "تم تخطي الجدولة لأن التذكيرات معطلة.")
                    return@launch
                }

                val reminderTime = reminderTime.first()
                val intervalMonths = settingsRepository.getIntervalForCandle(candleNumber).first()

                // إذا كانت المدة 0، لا تقم بالجدولة
                if (intervalMonths <= 0) {
                    Log.d("Notification", "تم تخطي جدولة الشمعة $candleNumber (ID: $filterId) لأن مدتها 0.")
                    return@launch
                }

                // حساب موعد التغيير القادم
                val nextChangeDate = Calendar.getInstance().apply {
                    timeInMillis = lastChangedDateMillis
                    add(Calendar.MONTH, intervalMonths)
                }.timeInMillis

                // حساب وقت الإشعار المحدد (مثلاً 9:00 صباحاً)
                val (hour, minute) = reminderTime.split(":").map { it.toInt() }
                val notificationTime = Calendar.getInstance().apply {
                    timeInMillis = nextChangeDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                // حساب مدة التأخير من الآن
                val delay = notificationTime - System.currentTimeMillis()


                if (delay > 0) {
                    val uniqueWorkName = "candle_notif_$filterId"

                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(androidx.work.Data.Builder()
                            .putInt("candleNumber", candleNumber)
                            .build())
                        .addTag("notification_candle_tag")
                        .build()

                    WorkManager.getInstance(app.applicationContext).enqueueUniqueWork(
                        uniqueWorkName,
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                }
            } catch (e: Exception) {
                Log.e("Notification", "خطأ في جدولة الإشعار: ${e.message}")
            }
        }
    }

    /**
     * دالة لإلغاء إشعار واحد
     */
    private fun cancelNotification(filterId: Int) {
        val uniqueWorkName = "candle_notif_$filterId"
        WorkManager.getInstance(app.applicationContext).cancelUniqueWork(uniqueWorkName)
    }

    /**
     * دالة لإلغاء كل الإشعارات
     */
    private fun cancelAllNotifications() {
        WorkManager.getInstance(app.applicationContext).cancelAllWorkByTag("notification_candle_tag")
        Log.d("Notification", "تم إلغاء جميع الإشعارات المجدولة.")
    }
}