package com.ahmedgamal.aquamemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CandleChangeReminderReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "aqua_memo_channel"
    private val CHANNEL_NAME = "Aqua Memo Reminders"
    private val CHANNEL_DESCRIPTION = "Reminders for water filter changes"
    private val NOTIFICATION_TONE_URI_KEY = "notification_tone_uri" // نفس المفتاح في SettingsActivity

    override fun onReceive(context: Context, intent: Intent) {
        val candleNumber = intent.getIntExtra("CANDLE_NUMBER", -1)
        Log.d("ReminderReceiver", "Received reminder for Candle $candleNumber")

        if (candleNumber != -1) {
            sendNotification(context, candleNumber)
            // بعد إرسال التذكير، قد ترغب في إعادة جدولة التذكير التالي إذا كان متكررًا،
            // أو في هذه الحالة، ربما يكون تذكيرًا لمرة واحدة حتى يغير المستخدم الفلتر.
            // إذا كنت تريد تكرار التذكير يوميًا بعد تاريخ الاستحقاق، ستحتاج لمنطق إضافي هنا.
        }
    }

    private fun sendNotification(context: Context, candleNumber: Int) {
        // تحقق من إذن الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("ReminderReceiver", "Notification permission not granted, cannot send notification.")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // إنشاء قناة الإشعارات (لـ Android 8.0 Oreo فأحدث)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
                // تعيين نغمة الإشعار على مستوى القناة
                val sharedPreferences = context.getSharedPreferences("CandleData", Context.MODE_PRIVATE)
                val toneUriString = sharedPreferences.getString(NOTIFICATION_TONE_URI_KEY, null)
                val notificationUri = if (toneUriString != null) {
                    Uri.parse(toneUriString)
                } else {
                    Settings.System.DEFAULT_NOTIFICATION_URI
                }
                setSound(notificationUri, null) // Use default audio attributes
                enableVibration(true) // يمكنك التحكم في الاهتزاز هنا أيضاً أو من إعدادات المستخدم
            }
            notificationManager.createNotificationChannel(channel)
        }

        // إنشاء Intent لفتح التطبيق عند النقر على الإشعار
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingIntentFlags)

        val title = context.getString(R.string.notification_title)
        val message = context.getString(R.string.notification_message, candleNumber)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // هنا
            .setContentTitle(title) // وهنا
            .setContentText(message) // وهنا
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // لإزالة الإشعار عند النقر عليه

        // إذا كان إصدار Android أقل من 8.0، قم بتعيين النغمة والاهتزاز هنا
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val sharedPreferences = context.getSharedPreferences("CandleData", Context.MODE_PRIVATE)
            val toneUriString = sharedPreferences.getString(NOTIFICATION_TONE_URI_KEY, null)
            val notificationUri = if (toneUriString != null) {
                Uri.parse(toneUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            notificationBuilder.setSound(notificationUri)
            notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000)) // نمط الاهتزاز
        }

        notificationManager.notify(candleNumber, notificationBuilder.build()) // استخدام رقم الشمعة كـ ID للإشعار
    }
}