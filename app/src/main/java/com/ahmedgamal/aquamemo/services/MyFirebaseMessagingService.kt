package com.ahmedgamal.aquamemo.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ahmedgamal.aquamemo.AquaMemoApp
import com.ahmedgamal.aquamemo.MainActivity
import com.ahmedgamal.aquamemo.R
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale
import android.content.res.Configuration

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var filterRepository: FilterRepository // لاستخدامه في الحفظ

    private val jobScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message Received From: ${remoteMessage.from}")

        // --- ✅ 1. (جديد) إنشاء سياق مُعدل باللغة الصحيحة ---
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("language", "en") ?: "en"
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        val localizedContext = createConfigurationContext(config)
        // --- نهاية الكود الجديد ---

        remoteMessage.data.let { data ->
            // ✅ 2. (معدل) استخدام "localizedContext"
            val title = data["title"] ?: localizedContext.getString(R.string.app_name)
            val message = data["message"] ?: localizedContext.getString(R.string.fcm_default_message)
            val timestamp = System.currentTimeMillis()

            // (كود حفظ الإشعار في قاعدة البيانات - كما هو)
            jobScope.launch {
                val notificationHistory = NotificationHistory(
                    type = "REMOTE_ADMIN",
                    title = title,
                    message = message,
                    timestamp = timestamp,
                    isRead = false,
                    iconType = "INFO"
                )
                try {
                    filterRepository.insertNotification(notificationHistory)
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to save remote message to DB", e)
                }
            }

            // (كود إظهار الإشعار - كما هو)
            sendLocalNotification(title, message, timestamp.toInt())
        }
    }

    /**
     * دالة لإنشاء إشعار محلي يظهر على شاشة المستخدم
     */
    private fun sendLocalNotification(title: String, message: String, notificationId: Int) {
        val channelId = AquaMemoApp.DEFAULT_CHANNEL_ID // استخدام القناة الافتراضية للرسائل الإدارية
        createAdminChannelIfNeeded() // التأكد من وجود القناة

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            notificationId, // استخدام ID فريد (الوقت)
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // الأيقونة
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * يتم استدعاؤها عند إنشاء توكن جديد للجهاز
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token Generated: $token")

        // (هام) جعل الجهاز يشترك في قناة "all_users"
        // هذا يسمح لك بإرسال رسالة "للجميع" بسهولة
        jobScope.launch {
            Firebase.messaging.subscribeToTopic("all_users")
                .addOnCompleteListener { task ->
                    var msg = "Subscribed to 'all_users' topic"
                    if (!task.isSuccessful) {
                        msg = "Failed to subscribe to 'all_users' topic"
                    }
                    Log.d("FCM", msg)
                }
        }
    }

    // (دالة مساعدة لإنشاء القناة إذا لم تكن موجودة - مثلما فعلنا في Worker)
    private fun createAdminChannelIfNeeded() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(AquaMemoApp.DEFAULT_CHANNEL_ID) == null) {

            // ✅ (تم الإصلاح) استخدام أسماء الـ strings الصحيحة
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(AquaMemoApp.DEFAULT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val Context.context: Context
        get() = this
}