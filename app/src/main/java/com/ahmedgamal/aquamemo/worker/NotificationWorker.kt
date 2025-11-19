package com.ahmedgamal.aquamemo.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmedgamal.aquamemo.AquaMemoApp
import com.ahmedgamal.aquamemo.MainActivity
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.billing.BillingManager
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import com.ahmedgamal.aquamemo.ui.getCandleNameForWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale
import android.content.res.Configuration

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val billingManager: BillingManager,
    private val filterRepository: FilterRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val candleNumber = inputData.getInt("candleNumber", 0)
        if (candleNumber == 0) {
            Log.e("NotificationWorker", "No candleNumber provided.")
            return Result.failure()
        }

        try {
            sendNotification(candleNumber)
            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error sending notification", e)
            return Result.failure()
        }
    }

    // 🔽 قم باستبدال هذه الدالة بالكامل 🔽
    private suspend fun sendNotification(candleNumber: Int) {
        val isPro = billingManager.isPremium.first()
        val channelId = if (isPro) AquaMemoApp.PRO_CHANNEL_ID else AquaMemoApp.DEFAULT_CHANNEL_ID
        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("language", "en") ?: "en"
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        // التأكد من إنشاء القناة
        createNotificationChannelIfNeeded(channelId)

        val candleName = getCandleNameForWorker(candleNumber, localizedContext)
        val title = localizedContext.getString(R.string.notification_title)
        val message = localizedContext.getString(R.string.notification_message, candleName)
        val timestamp = System.currentTimeMillis()

        val notificationHistory = NotificationHistory(
            type = "LOCAL_CANDLE",
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = false,
            iconType = "CANDLE"
        )

        try {
            filterRepository.insertNotification(notificationHistory)
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Failed to save notification to DB", e)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            candleNumber,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = if (isPro) {
            val soundSetting = Settings.System.getString(context.contentResolver, Settings.System.NOTIFICATION_SOUND)
            soundSetting?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setLights(Color.BLUE, 500, 500)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("NotificationWorker", "POST_NOTIFICATIONS permission not granted.")
            return
        }

        NotificationManagerCompat.from(context).notify(candleNumber, builder.build())
    }

    private fun createNotificationChannelIfNeeded(channelId: String) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) != null) {
            Log.d("NotificationWorker", "Channel $channelId already exists.")
            return
        }

        Log.d("NotificationWorker", "Channel $channelId does not exist. Creating it.")

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val (name, descriptionText) = if (channelId == AquaMemoApp.PRO_CHANNEL_ID) {
            Pair(
                context.getString(R.string.notification_channel_name_pro),
                context.getString(R.string.notification_channel_description_pro)
            )
        } else {
            Pair(
                context.getString(R.string.notification_channel_name),
                context.getString(R.string.notification_channel_description)
            )
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            vibrationPattern = vibrationPattern
            setSound(defaultSoundUri, audioAttributes)
        }

        notificationManager.createNotificationChannel(channel)
        Log.d("NotificationWorker", "Notification channel $channelId created.")
    }
}