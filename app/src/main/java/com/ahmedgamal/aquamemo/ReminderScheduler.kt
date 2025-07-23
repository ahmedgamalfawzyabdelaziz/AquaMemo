package com.ahmedgamal.aquamemo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderScheduler {

    private const val TAG = "ReminderScheduler" // تعريف TAG
    private const val REMINDER_HOUR_KEY = "reminder_hour" // نفس المفتاح في SettingsActivity
    private const val REMINDER_MINUTE_KEY = "reminder_minute" // نفس المفتاح في SettingsActivity
    private const val FILTER_TYPE_KEY = "filter_type"

    private val candleReplacementPeriods = listOf(3, 6, 6, 21, 21, 24, 24)

    fun scheduleCandleReminders(context: Context, sharedPreferences: SharedPreferences) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // إلغاء أي تنبيهات مجدولة سابقًا لمنع التكرار
        cancelAllReminders(context, alarmManager, sharedPreferences)

        val selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)
        val defaultReminderHour = 9 // Default to 9 AM
        val defaultReminderMinute = 0 // Default to 0 minutes

        // الحصول على وقت التذكير المفضل من SharedPreferences
        val reminderHour = sharedPreferences.getInt(REMINDER_HOUR_KEY, defaultReminderHour)
        val reminderMinute = sharedPreferences.getInt(REMINDER_MINUTE_KEY, defaultReminderMinute)

        Log.d(TAG, "Scheduling reminders for $selectedFilterType filters at $reminderHour:$reminderMinute")

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

        for (i in 0 until selectedFilterType) {
            val installDateString = sharedPreferences.getString("candle_${i + 1}_install_date", null)
            val installTimeString = sharedPreferences.getString("candle_${i + 1}_install_time", "00:00") // Time might not be saved, default to midnight

            if (installDateString != null) {
                try {
                    val installDateTimeString = "$installDateString $installTimeString"
                    val installDate = dateFormat.parse(installDateTimeString)

                    if (installDate != null) {
                        val nextChangeCalendar = Calendar.getInstance().apply {
                            time = installDate
                            // Add months based on the replacement period
                            add(Calendar.MONTH, candleReplacementPeriods.getOrNull(i) ?: 0)
                            // Set the reminder time of day
                            set(Calendar.HOUR_OF_DAY, reminderHour)
                            set(Calendar.MINUTE, reminderMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val now = Calendar.getInstance()
                        now.set(Calendar.SECOND, 0)
                        now.set(Calendar.MILLISECOND, 0)

                        // إذا كان وقت التذكير في الماضي لليوم الحالي، اجعله لليوم التالي
                        if (nextChangeCalendar.before(now)) {
                            // إذا كان تاريخ التغيير التالي نفسه في الماضي، هذا يعني أن الشمعة مستحقة بالفعل
                            // ولكن إذا كان الوقت في نفس اليوم قد مر، فاجعل التذكير لليوم التالي
                            if (nextChangeCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                                nextChangeCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                                nextChangeCalendar.add(Calendar.DAY_OF_YEAR, 1) // Move to next day
                                Log.d(TAG, "Candle ${i+1} reminder time passed today, rescheduling for tomorrow: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(nextChangeCalendar.time)}")
                            } else {
                                Log.d(TAG, "Candle ${i+1} next change date ${SimpleDateFormat("dd/MM/yyyy").format(nextChangeCalendar.time)} is in the past. Not scheduling reminder.")
                                continue // لا تجدول تذكيرًا لتاريخ فات
                            }
                        }

                        val reminderIntent = Intent(context, CandleChangeReminderReceiver::class.java).apply {
                            putExtra("CANDLE_NUMBER", i + 1)
                            // يمكنك إضافة المزيد من البيانات إذا احتجت
                        }

                        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }

                        // استخدم requestCode فريد لكل شمعة
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            i + 1, // Use candle number as unique request code
                            reminderIntent,
                            pendingIntentFlags
                        )

                        // جدولة التنبيه باستخدام setExactAndAllowWhileIdle لضمان الدقة
                        // أو setExact إذا لم يكن وضع الخمول مشكلة (API 19+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nextChangeCalendar.timeInMillis,
                                pendingIntent
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                nextChangeCalendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                nextChangeCalendar.timeInMillis,
                                pendingIntent
                            )
                        }
                        Log.d(TAG, "Scheduled reminder for Candle ${i+1} at ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(nextChangeCalendar.time)}")

                    } else {
                        Log.e(TAG, "Failed to parse install date for Candle ${i+1}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling reminder for Candle ${i+1}: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "No install date found for Candle ${i+1}, skipping reminder scheduling.")
            }
        }
    }

    private fun cancelAllReminders(context: Context, alarmManager: AlarmManager, sharedPreferences: SharedPreferences) {
        val selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        for (i in 0 until selectedFilterType) {
            val reminderIntent = Intent(context, CandleChangeReminderReceiver::class.java).apply {
                putExtra("CANDLE_NUMBER", i + 1)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i + 1, // Use the same unique request code
                reminderIntent,
                pendingIntentFlags
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled reminder for Candle ${i+1}")
        }
    }
}