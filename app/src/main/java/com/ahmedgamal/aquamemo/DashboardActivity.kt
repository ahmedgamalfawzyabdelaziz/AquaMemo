package com.ahmedgamal.aquamemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    // استخدم نفس فترات التغيير بالأشهر الموجودة في MainActivity لضمان الاتساق
    private val candleReplacementPeriods = listOf(3, 6, 6, 21, 21, 24, 24)

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsChangeReceiver: BroadcastReceiver
    private val FILTER_TYPE_KEY = "filter_type"
    private var selectedFilterType: Int = 7
    // المفتاح الخاص باللغة، يجب أن يطابق ما هو في SettingsActivity و MainActivity و SelectFilterActivity
    private val APP_LANGUAGE_KEY = "selected_language"

    // --- كود تطبيق اللغة: يتم تجاوز attachBaseContext ---
    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        // استخدم الثوابت من SettingsActivity للوصول إلى قيم الشيرد بريفيرنسز
        val languageCode = sharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar") ?: "ar"
        val fontSizeScale = sharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f)
        val config = Configuration(newBase.resources.configuration)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)

        config.fontScale = fontSizeScale // تطبيق حجم الخط

        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        super.attachBaseContext(context)
    }
    // --- نهاية كود تطبيق اللغة ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        // تهيئة وتسجيل الـ BroadcastReceiver للاستماع لتغيرات الإعدادات (اللغة وحجم الخط)
        settingsChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AppConstants.ACTION_REFRESH_APP_LANGUAGE -> { // استخدم AppConstants
                        recreate() // إعادة إنشاء النشاط لتحديث اللغة
                    }
                    AppConstants.ACTION_REFRESH_APP_FONT_SIZE -> { // استخدم AppConstants
                        recreate()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(this).apply {
            val intentFilter = IntentFilter()
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_LANGUAGE)
            intentFilter.addAction(AppConstants.ACTION_REFRESH_APP_FONT_SIZE)
            registerReceiver(settingsChangeReceiver, intentFilter)
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        selectedFilterType = sharedPreferences.getInt(FILTER_TYPE_KEY, 7)

        val dashboardTextView: TextView = findViewById(R.id.text_view_dashboard)
        val backToMainButton: Button = findViewById(R.id.btn_back_to_main_dashboard)
        val modifyDataButton: Button = findViewById(R.id.btn_modify_data)
        val dashboardTitle: TextView = findViewById(R.id.dashboard_title)
        val dashboardFooter: TextView = findViewById(R.id.dashboard_footer)

        // Set texts from strings.xml
        dashboardTitle.text = getString(R.string.dashboard_title)
        backToMainButton.text = getString(R.string.back_to_main_button_text)
        modifyDataButton.text = getString(R.string.modify_data_button_text)
        dashboardFooter.text = getString(R.string.dashboard_footer)

        val stringBuilder = StringBuilder()

        // استخدام Locale.getDefault() ليتوافق مع اللغة الحالية
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (i in 0 until selectedFilterType) {
            val lastChangeDateString = sharedPreferences.getString("candle_${i + 1}_install_date", null)
            val lastChangeTimeString = sharedPreferences.getString("candle_${i + 1}_install_time", "00:00")

            if (lastChangeDateString != null) {
                try {
                    val lastChangeDateTimeString = "$lastChangeDateString $lastChangeTimeString"
                    val lastChangeDateTime = dateFormat.parse(lastChangeDateTimeString)

                    if (lastChangeDateTime != null) {
                        val nextChangeCalendar = Calendar.getInstance()
                        nextChangeCalendar.time = lastChangeDateTime
                        nextChangeCalendar.add(Calendar.MONTH, candleReplacementPeriods.getOrNull(i) ?: 0)

                        val today = Calendar.getInstance()
                        today.set(Calendar.HOUR_OF_DAY, 0)
                        today.set(Calendar.MINUTE, 0)
                        today.set(Calendar.SECOND, 0)
                        today.set(Calendar.MILLISECOND, 0)
                        nextChangeCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        nextChangeCalendar.set(Calendar.MINUTE, 0)
                        nextChangeCalendar.set(Calendar.SECOND, 0)
                        nextChangeCalendar.set(Calendar.MILLISECOND, 0)

                        val diffMillis = nextChangeCalendar.timeInMillis - today.timeInMillis
                        var daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)

                        if (daysLeft < 0) {
                            daysLeft = 0L
                        }

                        if (daysLeft <= 0) {
                            stringBuilder.append(getString(R.string.dashboard_message_today, i + 1)).append("\n")
                        } else {
                            stringBuilder.append(getString(R.string.dashboard_message_days_left, i + 1, daysLeft)).append("\n")
                        }
                    } else {
                        // استخدام string resource للرسالة هنا
                        stringBuilder.append(getString(R.string.dashboard_invalid_data, i + 1)).append("\n")
                    }
                } catch (e: Exception) {
                    // استخدام string resource للرسالة هنا
                    stringBuilder.append(getString(R.string.dashboard_read_error, i + 1)).append("\n")
                    e.printStackTrace()
                }
            } else {
                // استخدام string resource للرسالة هنا
                stringBuilder.append(getString(R.string.dashboard_no_data_entered, i + 1)).append("\n")
            }
        }
        dashboardTextView.text = stringBuilder.toString()

        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        modifyDataButton.setOnClickListener {
            val intent = Intent(this, InputDataActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsChangeReceiver)
    }
}