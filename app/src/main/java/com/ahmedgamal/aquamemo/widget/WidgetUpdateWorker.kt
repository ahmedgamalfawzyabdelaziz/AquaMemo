package com.ahmedgamal.aquamemo.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmedgamal.aquamemo.billing.BillingManager
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.SettingsRepository
import com.ahmedgamal.aquamemo.data.model.Filter
import com.ahmedgamal.aquamemo.ui.getCandleNameForWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val filterRepository: FilterRepository,
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager
) : CoroutineWorker(context, workerParams) {
    private val tag = "WidgetUpdateWorker"
    data class NearestMaintenanceInfo(
        val candleName: String,
        val nextChangeDate: String,
        val daysRemaining: Int
    )
    override suspend fun doWork(): Result {
        Log.d(tag, "Worker started.")
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = try {
            glanceManager.getGlanceIds(AquaMemoWidget::class.java)
        } catch (e: Exception) {
            Log.e(tag, "Error getting Glance IDs: ${e.message}", e)
            return Result.failure()
        }

        if (glanceIds.isEmpty()) {
            Log.d(tag, "No widgets found to update. Worker finished.")
            return Result.success()
        }
        Log.d(tag, "Found ${glanceIds.size} widget(s) to update.")

        try {
            // 1. Fetch Data (Filters, Nearest Maintenance)
            val allFilters = filterRepository.getAllFilters().first()
            Log.d(tag, "Fetched ${allFilters.size} filters from repository.")
            val nearestInfo = findNearestMaintenance(allFilters, settingsRepository)
            Log.d(tag, "Nearest maintenance info: $nearestInfo")
            // 2. Fetch Settings
            val themePreference = settingsRepository.themePreferenceFlow.first()
            Log.d(tag, "Fetched theme preference: $themePreference")
            // 3. Fetch Pro Status
            val isPro = billingManager.isPremium.first().toString()
            Log.d(tag, "Fetched Pro status: $isPro")
            // 4. Update all active widgets
            glanceIds.forEach { glanceId ->
                Log.d(tag, "Updating widget state for glanceId: $glanceId")
                val currentState = getAppWidgetState(context, AquaMemoWidget.stateDefinition, glanceId)
                val currentFontSize = currentState[WidgetData.fontSizeScaleKey] ?: 1.0f // Default if not found
                val currentOpacity = currentState[WidgetData.backgroundOpacityKey] ?: 1.0f // Default if not found
                Log.d(tag, "  Read existing FontScale: $currentFontSize, Opacity: $currentOpacity")

                updateAppWidgetState(context, glanceId) { prefs ->
                    // Save Pro status
                    prefs[WidgetData.isProKey] = isPro
                    Log.d(tag, "  Saving IsPro: $isPro")
                    // Save Filter Data (if available)
                    if (nearestInfo != null) {
                        Log.d(tag, "  Saving CandleName: ${nearestInfo.candleName}")
                        Log.d(tag, "  Saving NextDate: ${nearestInfo.nextChangeDate}")
                        Log.d(tag, "  Saving DaysRemaining: ${nearestInfo.daysRemaining}")
                        prefs[WidgetData.candleNameKey] = nearestInfo.candleName
                        prefs[WidgetData.nextDateKey] = nearestInfo.nextChangeDate
                        prefs[WidgetData.daysRemainingKey] = nearestInfo.daysRemaining
                    } else {
                        Log.d(tag, "  No nearest maintenance info, removing filter keys.")
                        prefs.remove(WidgetData.candleNameKey)
                        prefs.remove(WidgetData.nextDateKey)
                        prefs.remove(WidgetData.daysRemainingKey)
                    }
                    // Save Theme
                    Log.d(tag, "  Saving Theme: $themePreference")
                    prefs[WidgetData.themeKey] = themePreference
                    Log.d(tag, "  Preserving FontScale: $currentFontSize")
                    Log.d(tag, "  Preserving Opacity: $currentOpacity")
                    prefs[WidgetData.fontSizeScaleKey] = currentFontSize
                    prefs[WidgetData.backgroundOpacityKey] = currentOpacity
                }
                AquaMemoWidget.update(context, glanceId)
                Log.d(tag, "Triggered widget update for glanceId: $glanceId")
            }
            Log.d(tag, "Worker finished successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(tag, "Worker failed: ${e.message}", e)
            e.printStackTrace()
            return Result.failure()
        }
    }
    private suspend fun findNearestMaintenance(
        filters: List<Filter>,
        settingsRepository: SettingsRepository // 1. استقبال الـ Repository
    ): NearestMaintenanceInfo? {
        if (filters.isEmpty()){
            Log.d(tag, "findNearestMaintenance: Filter list is empty.")
            return null
        }
        val currentTime = System.currentTimeMillis()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // 2. استخدام mutableList لأننا سنحتاج لـ suspend fun
        val maintenanceInfos = mutableListOf<NearestMaintenanceInfo>()
        // 3. استخدام Loop (بدلاً من .map) للتعامل مع .first()
        for (filter in filters) {
            // 4. (هذا هو التعديل الأهم) قراءة المدة من DataStore
            val intervalMonths = settingsRepository.getIntervalForCandle(filter.candleNumber).first()
            // 5. إذا كانت المدة 0 (المستخدم ألغاها)، تجاهل هذه الشمعة
            if (intervalMonths <= 0) continue

            val nextChangeCalendar = Calendar.getInstance().apply {
                timeInMillis = filter.lastChangedDate
                add(Calendar.MONTH, intervalMonths)
            }
            val nextChangeDate = nextChangeCalendar.timeInMillis
            val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTime }
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0); currentCalendar.set(Calendar.MINUTE, 0); currentCalendar.set(Calendar.SECOND, 0); currentCalendar.set(Calendar.MILLISECOND, 0)
            nextChangeCalendar.set(Calendar.HOUR_OF_DAY, 0); nextChangeCalendar.set(Calendar.MINUTE, 0); nextChangeCalendar.set(Calendar.SECOND, 0); nextChangeCalendar.set(Calendar.MILLISECOND, 0)

            val diffMillis = nextChangeCalendar.timeInMillis - currentCalendar.timeInMillis
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
            val candleName = getCandleNameForWorker(filter.candleNumber, context)
            val formattedDate = dateFormatter.format(Date(nextChangeDate))

            Log.d(tag, "  Filter ${filter.candleNumber}: LastChange=${Date(filter.lastChangedDate)}, Interval=$intervalMonths months, NextChange=${Date(nextChangeDate)}, DaysRemaining=$daysRemaining")

            maintenanceInfos.add(NearestMaintenanceInfo(candleName, formattedDate, daysRemaining))
        }

        val nearest = maintenanceInfos.minByOrNull { it.daysRemaining }
        Log.d(tag, "findNearestMaintenance: Found nearest: $nearest")
        return nearest
    }
}