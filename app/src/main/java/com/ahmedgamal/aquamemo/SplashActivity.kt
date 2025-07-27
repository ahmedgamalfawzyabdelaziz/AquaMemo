package com.ahmedgamal.aquamemo

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val appSharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = appSharedPreferences.getString(AppConstants.APP_LANGUAGE_KEY, "ar") ?: "ar"
        val fontSizeScale = appSharedPreferences.getFloat(AppConstants.APP_FONT_SIZE_KEY, 1.0f)

        val config = Configuration(newBase.resources.configuration)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)

        config.fontScale = fontSizeScale

        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appNameTextView: TextView = findViewById(R.id.app_name_splash)

        val SPLASH_TIME_OUT: Long = 4000 // مدة ظهور الشاشة الافتتاحية بالكامل

        // البدء بجعل النص شفافاً تماماً (للتأكيد، بالرغم من وجودها في الـ XML)
        appNameTextView.alpha = 0.0f

        // استخدام Property Animation لظهور الكلمة تدريجياً
        appNameTextView.animate()
            .alpha(1.0f) // الانتقال إلى ظهور كامل
            .setDuration(3000) // مدة الأنيميشن نفسه بالمللي ثانية (هنا ثانية ونصف)
            .withEndAction {
                // هذا الكود سيتم تشغيله بعد انتهاء الأنيميشن،
                // لكننا سنعتمد على الـ Handler الأصلي للانتقال لـ MainActivity
            }
            .start() // بدء الأنيميشن

        // تأخير الانتقال للصفحة الرئيسية
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // إغلاق SplashActivity لمنع العودة إليها بالزر الخلفي
        }, SPLASH_TIME_OUT)
    }
}