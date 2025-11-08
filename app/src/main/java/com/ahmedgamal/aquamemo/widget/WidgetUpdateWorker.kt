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
import com.ahmedgamal.aquamemo.ui.getCandleIntervalForWorker
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

    private val TAG = "WidgetUpdateWorker"

    data class NearestMaintenanceInfo(
        val candleName: String,
        val nextChangeDate: String,
        val daysRemaining: Int
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started.")
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = try {
            glanceManager.getGlanceIds(AquaMemoWidget::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Glance IDs: ${e.message}", e)
            return Result.failure()
        }

        if (glanceIds.isEmpty()) {
            Log.d(TAG, "No widgets found to update. Worker finished.")
            return Result.success()
        }
        Log.d(TAG, "Found ${glanceIds.size} widget(s) to update.")

        try {
            // 1. Fetch Data (Filters, Nearest Maintenance)
            val allFilters = filterRepository.getAllFilters().first()
            Log.d(TAG, "Fetched ${allFilters.size} filters from repository.")
            val nearestInfo = findNearestMaintenance(allFilters)
            Log.d(TAG, "Nearest maintenance info: $nearestInfo")

            // 2. Fetch Settings
            val themePreference = settingsRepository.themePreferenceFlow.first()
            Log.d(TAG, "Fetched theme preference: $themePreference")

            // 3. Fetch Pro Status
            val isPro = billingManager.isPremium.first().toString()
            Log.d(TAG, "Fetched Pro status: $isPro")

            // 4. Update all active widgets
            glanceIds.forEach { glanceId ->
                Log.d(TAG, "Updating widget state for glanceId: $glanceId")

                // ✅ START: Read existing config settings before updating
                val currentState = getAppWidgetState(context, AquaMemoWidget.stateDefinition, glanceId)
                val currentFontSize = currentState[WidgetData.fontSizeScaleKey] ?: 1.0f // Default if not found
                val currentOpacity = currentState[WidgetData.backgroundOpacityKey] ?: 1.0f // Default if not found
                Log.d(TAG, "  Read existing FontScale: $currentFontSize, Opacity: $currentOpacity")
                // ✅ END: Read existing config settings

                updateAppWidgetState(context, glanceId) { prefs ->
                    // Save Pro status
                    prefs[WidgetData.isProKey] = isPro
                    Log.d(TAG, "  Saving IsPro: $isPro")

                    // Save Filter Data (if available)
                    if (nearestInfo != null) {
                        Log.d(TAG, "  Saving CandleName: ${nearestInfo.candleName}")
                        Log.d(TAG, "  Saving NextDate: ${nearestInfo.nextChangeDate}")
                        Log.d(TAG, "  Saving DaysRemaining: ${nearestInfo.daysRemaining}")
                        prefs[WidgetData.candleNameKey] = nearestInfo.candleName
                        prefs[WidgetData.nextDateKey] = nearestInfo.nextChangeDate
                        prefs[WidgetData.daysRemainingKey] = nearestInfo.daysRemaining
                    } else {
                        Log.d(TAG, "  No nearest maintenance info, removing filter keys.")
                        prefs.remove(WidgetData.candleNameKey)
                        prefs.remove(WidgetData.nextDateKey)
                        prefs.remove(WidgetData.daysRemainingKey)
                    }

                    // Save Theme
                    Log.d(TAG, "  Saving Theme: $themePreference")
                    prefs[WidgetData.themeKey] = themePreference

                    // ✅ START: Preserve config settings
                    Log.d(TAG, "  Preserving FontScale: $currentFontSize")
                    Log.d(TAG, "  Preserving Opacity: $currentOpacity")
                    prefs[WidgetData.fontSizeScaleKey] = currentFontSize
                    prefs[WidgetData.backgroundOpacityKey] = currentOpacity
                    // ✅ END: Preserve config settings
                }
                // Force the widget to redraw
                AquaMemoWidget.update(context, glanceId)
                Log.d(TAG, "Triggered widget update for glanceId: $glanceId")
            }
            Log.d(TAG, "Worker finished successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.message}", e)
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun findNearestMaintenance(filters: List<Filter>): NearestMaintenanceInfo? {
        if (filters.isEmpty()){
            Log.d(TAG, "findNearestMaintenance: Filter list is empty.")
            return null
        }

        val currentTime = System.currentTimeMillis()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val maintenanceInfos = filters.map { filter ->
            val intervalMonths = getCandleIntervalForWorker(filter.candleNumber)
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

            Log.d(TAG, "  Filter ${filter.candleNumber}: LastChange=${Date(filter.lastChangedDate)}, Interval=$intervalMonths months, NextChange=${Date(nextChangeDate)}, DaysRemaining=$daysRemaining")

            NearestMaintenanceInfo(candleName, formattedDate, daysRemaining)
        }

        val nearest = maintenanceInfos.minByOrNull { it.daysRemaining }
        Log.d(TAG, "findNearestMaintenance: Found nearest: $nearest")
        return nearest
    }
}