package com.ahmedgamal.aquamemo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes // ✅ Import
import android.media.RingtoneManager // ✅ Import
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AquaMemoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // Define Channel IDs as constants
    companion object {
        const val DEFAULT_CHANNEL_ID = "aquamemo_channel_id" // For free users
        const val PRO_CHANNEL_ID = "aquamemo_pro_channel_id" // For pro users
    }

    override fun onCreate() {
        super.onCreate()
        // ✅ Changed to only create the default channel
        createDefaultNotificationChannel()
    }

    // ✅ Renamed to createDefaultNotificationChannel
    private fun createDefaultNotificationChannel() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // ✅ Create ONLY the DEFAULT Channel
        val defaultName = getString(R.string.notification_channel_name)
        val defaultDescription = getString(R.string.notification_channel_description)
        val defaultChannel = NotificationChannel(DEFAULT_CHANNEL_ID, defaultName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = defaultDescription
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
            setSound(defaultSoundUri, audioAttributes) // Set default sound
        }
        notificationManager.createNotificationChannel(defaultChannel)
    }
}