package com.ahmedgamal.aquamemo // تأكد أن هذا هو الـ Package Name الصحيح لمشروعك

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Re-scheduling reminders.")
            val sharedPreferences = context.getSharedPreferences("CandleData", Context.MODE_PRIVATE)
            // استدعاء دالة الجدولة من ReminderScheduler
            ReminderScheduler.scheduleCandleReminders(context, sharedPreferences)
        }
    }
}