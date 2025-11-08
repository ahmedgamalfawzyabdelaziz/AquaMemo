// LanguageManager.kt
package com.ahmedgamal.aquamemo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import com.ahmedgamal.aquamemo.MainActivity
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "AppSettings"
    private const val LANGUAGE_KEY = "language"

    fun getCurrentLanguage(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(LANGUAGE_KEY, "en") ?: "en"
    }

    fun setLanguage(context: Context, languageCode: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // حفظ اللغة بطرق متعددة
        sharedPref.edit { putString(LANGUAGE_KEY, languageCode) }

        // تطبيق اللغة فورياً
        applyLanguageImmediately(context, languageCode)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun applyLanguageImmediately(context: Context, languageCode: String) {
        val locale =
            Locale.forLanguageTag(languageCode)
                    Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        config.setLocale(locale)
        context.createConfigurationContext(config)

        // تحديث الـ Resources بطرق متعددة
        updateResourcesForAllDevices(context, locale)
    }

    private fun updateResourcesForAllDevices(context: Context, locale: Locale) {
        try {
            val resources = context.resources
            val config = Configuration(resources.configuration)

            // طريقة 1: الطريقة القياسية
            config.setLocale(locale)

            // طريقة 2: للمصنعين المختلفين
            when {
                Build.MANUFACTURER.equals("realme", ignoreCase = true) ->
                    applyRealmeFix(resources, config)
                Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ->
                    applyXiaomiFix(resources, config)
                Build.MANUFACTURER.equals("oppo", ignoreCase = true) ->
                    applyOppoFix(resources, config)
                else -> applyStandardUpdate(resources, config)
            }

        } catch (e: Exception) {
            Log.e("LanguageManager", "Error updating resources: ${e.message}")
        }
    }

    private fun applyRealmeFix(resources: Resources, config: Configuration) {
        // حل خاص لـ Realme
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // محاولة إضافية
        try {
            resources.configuration.setLocale(config.locales[0])
        } catch (_: Exception) {
            // تجاهل الخطأ واستمر
        }
    }

    private fun applyXiaomiFix(resources: Resources, config: Configuration) {
        // حل خاص لـ Xiaomi
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // إعادة تعيين
        val newConfig = Configuration(resources.configuration)
        newConfig.setLocale(config.locales[0])
        @Suppress("DEPRECATION")
        resources.updateConfiguration(newConfig, resources.displayMetrics)
    }

    private fun applyOppoFix(resources: Resources, config: Configuration) {
        // حل خاص لـ Oppo
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun applyStandardUpdate(resources: Resources, config: Configuration) {
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        activity.finish()

        // تأكيد الإنهاء
        Handler(Looper.getMainLooper()).postDelayed({
            activity.finishAffinity()
        }, 1000)
    }
}