// FilterUtils.kt - النسخة المحسنة
package com.ahmedgamal.aquamemo.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmedgamal.aquamemo.R

// 🔽 الدوال الموحدة - تستخدم في Composables
@Composable
fun getCandleName(candleNumber: Int): String {
    return when (candleNumber) {
        1 -> stringResource(R.string.stage_1)
        2 -> stringResource(R.string.stage_2)
        3 -> stringResource(R.string.stage_3)
        4 -> stringResource(R.string.stage_4)
        5 -> stringResource(R.string.stage_5)
        6 -> stringResource(R.string.stage_6)
        7 -> stringResource(R.string.stage_7)
        else -> ""
    }
}

@Composable
fun getCandleIntervalInMonths(candleNumber: Int): Int {
    return getCandleIntervalInternal(candleNumber)
}

// 🔽 الدوال الموحدة - تستخدم في ViewModels و Workers
fun getCandleNameForWorker(candleNumber: Int, context: Context): String {
    return when (candleNumber) {
        1 -> context.getString(R.string.stage_1)
        2 -> context.getString(R.string.stage_2)
        3 -> context.getString(R.string.stage_3)
        4 -> context.getString(R.string.stage_4)
        5 -> context.getString(R.string.stage_5)
        6 -> context.getString(R.string.stage_6)
        7 -> context.getString(R.string.stage_7)
        else -> ""
    }
}

fun getCandleIntervalForWorker(candleNumber: Int): Int {
    return getCandleIntervalInternal(candleNumber)
}

// 🔽 الدالة الداخلية الموحدة
private fun getCandleIntervalInternal(candleNumber: Int): Int {
    return when (candleNumber) {
        1 -> 3    // المرحلة الأولى - 3 أشهر
        2, 3 -> 6 // المرحلة الثانية والثالثة - 6 أشهر
        4 -> 18   // المرحلة الرابعة - 18 شهراً
        5 -> 8    // المرحلة الخامسة - 8 أشهر
        6, 7 -> 18 // المرحلة السادسة والسابعة - 18 شهراً
        else -> 0
    }
}

// 🔽 دالة مساعدة جديدة للحصول على سعر افتراضي
fun getDefaultCandlePrice(candleNumber: Int): Double {
    return when (candleNumber) {
        1 -> 50.0   // المرحلة الأولى
        2, 3 -> 75.0 // المرحلة الثانية والثالثة
        4 -> 120.0  // المرحلة الرابعة
        5 -> 100.0  // المرحلة الخامسة
        6, 7 -> 120.0 // المرحلة السادسة والسابعة
        else -> 0.0
    }
}