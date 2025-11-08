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
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmedgamal.aquamemo.AquaMemoApp // ✅ Import App constants
import com.ahmedgamal.aquamemo.MainActivity
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.billing.BillingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NotificationWorker
    @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val billingManager: BillingManager
) : CoroutineWorker(context, workerParams) {

    private val vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)

    override suspend fun doWork(): Result {
        return try {
            val candleNumber = inputData.getInt("candleNumber", -1)
            val candleName = inputData.getString("candleName") ?: ""

            if (candleNumber != -1 && candleName.isNotEmpty()) {

                val isPro = billingManager.isPremium.first()

                // ✅ Choose Channel ID based on Pro status
                val channelId = if (isPro) {
                    Log.d("NotificationWorker", "User is Pro. Using PRO channel.")
                    AquaMemoApp.PRO_CHANNEL_ID
                } else {
                    Log.d("NotificationWorker", "User is Free. Using DEFAULT channel.")
                    AquaMemoApp.DEFAULT_CHANNEL_ID
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                // ✅ Pass channelId and audioAttributes to showNotification
                showNotification(candleNumber, candleName, channelId, audioAttributes)
                Log.d("NotificationWorker", "Notification show trigger successful for candle $candleNumber")
                Result.success()
            } else {
                Log.e("NotificationWorker", "Insufficient data for notification")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error in doWork: ${e.message}", e)
            Result.failure()
        }
    }

    private fun showNotification(
        candleNumber: Int,
        candleName: String,
        channelId: String,
        audioAttributes: AudioAttributes // ✅ Receive attributes
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ This function will now create the specific channel if it's missing
        createNotificationChannelIfNeeded(notificationManager, channelId, audioAttributes)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("candleNumber", candleNumber)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, candleNumber, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message, candleName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        // ✅ No sound or vibration set on the builder. It will use the channel's settings.

        val notification = notificationBuilder.build()

        Log.d("NotificationWorker", "Posting to channel: $channelId")
        Log.d("NotificationWorker", "FINAL SOUND (from channel): ${notificationManager.getNotificationChannel(channelId)?.sound}")
        Log.d("NotificationWorker", "FINAL VIBRATE (from channel): ${notificationManager.getNotificationChannel(channelId)?.vibrationPattern}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationWorker", "POST_NOTIFICATIONS permission not granted.")
                return
            }
        }
        try {
            notificationManager.notify(candleNumber, notification)
            Log.d("NotificationWorker", "NotificationManager.notify called successfully for ID $candleNumber")
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error calling NotificationManager.notify: ${e.message}", e)
        }
    }

    // ✅ Renamed and updated function to create *only the needed channel*
    private fun createNotificationChannelIfNeeded(
        notificationManager: NotificationManager,
        channelId: String,
        audioAttributes: AudioAttributes
    ) {

        // If channel already exists, do nothing.
        if (notificationManager.getNotificationChannel(channelId) != null) {
            Log.d("NotificationWorker", "Channel $channelId already exists.")
            return
        }

        Log.d("NotificationWorker", "Channel $channelId does not exist. Creating it.")

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Configure the channel based on which one we are creating
        val (name, descriptionText) = if (channelId == AquaMemoApp.PRO_CHANNEL_ID) {
            // We are creating the Pro channel
            Pair(
                context.getString(R.string.notification_channel_name_pro),
                context.getString(R.string.notification_channel_description_pro)
            )
        } else {
            // We are creating the Default channel
            Pair(
                context.getString(R.string.notification_channel_name),
                context.getString(R.string.notification_channel_description)
            )
        }

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            vibrationPattern = vibrationPattern
            setSound(defaultSoundUri, audioAttributes) // Set default sound
        }

        notificationManager.createNotificationChannel(channel)
        Log.d("NotificationWorker", "Notification channel $channelId created.")
    }
}