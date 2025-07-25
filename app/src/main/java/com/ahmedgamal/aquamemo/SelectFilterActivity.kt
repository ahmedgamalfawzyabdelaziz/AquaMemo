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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.util.Locale

class SelectFilterActivity : AppCompatActivity() {
    private lateinit var settingsChangeReceiver: BroadcastReceiver
    private lateinit var sharedPreferences: SharedPreferences
    // تأكد أن هذا المفتاح يطابق المفتاح في MainActivity
    private val FILTER_TYPE_KEY = "filter_type"
    // المفتاح الخاص باللغة، يجب أن يطابق المفتاح في SettingsActivity و MainActivity
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
        // **تأكد أن لديك ملف activity_select_filter.xml في res/layout/**
        setContentView(R.layout.activity_select_filter)
        // تهيئة وتسجيل الـ BroadcastReceiver للاستماع لتغيرات الإعدادات (اللغة وحجم الخط)
        settingsChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AppConstants.ACTION_REFRESH_APP_LANGUAGE -> { // استخدم AppConstants
                        recreate() // إعادة إنشاء النشاط لتحديث اللغة
                    }
                    AppConstants.ACTION_REFRESH_APP_FONT_SIZE -> { // استخدم AppConstants
                        recreate() // إعادة إنشاء النشاط لتحديث حجم الخط
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
        // استخدم PreferenceManager.getDefaultSharedPreferences(this) هنا أيضًا
        // لضمان استخدام نفس الـ SharedPreferences الخاصة باللغة والتحكمات الأخرى
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)


        // تهيئة الأزرار بالـ IDs الصحيحة من activity_select_filter.xml
        // **تأكد أن هذه الـ IDs موجودة في activity_select_filter.xml لديك**
        val btn3Candles: Button = findViewById(R.id.btn_3_candles)
        val btn5Candles: Button = findViewById(R.id.btn_5_candles)
        val btn7Candles: Button = findViewById(R.id.btn_7_candles)

        btn3Candles.setOnClickListener {
            saveFilterType(3)
            // استخدام String Resource هنا
            Toast.makeText(this, getString(R.string.filter_type_3_selected), Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        btn5Candles.setOnClickListener {
            saveFilterType(5)
            // استخدام String Resource هنا
            Toast.makeText(this, getString(R.string.filter_type_5_selected), Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        btn7Candles.setOnClickListener {
            saveFilterType(7)
            // استخدام String Resource هنا
            Toast.makeText(this, getString(R.string.filter_type_7_selected), Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    private fun saveFilterType(type: Int) {
        sharedPreferences.edit().putInt(FILTER_TYPE_KEY, type).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // إغلاق SelectFilterActivity بعد الانتقال إلى MainActivity
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsChangeReceiver)
    }
}