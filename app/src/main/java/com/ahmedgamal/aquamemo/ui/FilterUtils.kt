// FilterUtils.kt
package com.ahmedgamal.aquamemo.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmedgamal.aquamemo.R

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