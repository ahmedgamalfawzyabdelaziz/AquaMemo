package com.ahmedgamal.aquamemo.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AquaMemoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = AquaMemoWidget

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
    }

    companion object {
        const val WIDGET_UPDATE_WORK_NAME = "com.ahmedgamal.aquamemo.widget.WidgetUpdateWorker"

        fun schedulePeriodicUpdates(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}