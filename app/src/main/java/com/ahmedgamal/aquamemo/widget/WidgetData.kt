package com.ahmedgamal.aquamemo.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetData {
    // Keys to store the widget's state
    val candleNameKey = stringPreferencesKey("widget_candle_name")
    val nextDateKey = stringPreferencesKey("widget_next_date")
    val daysRemainingKey = intPreferencesKey("widget_days_remaining")
    val themeKey = stringPreferencesKey("widget_theme_preference")
    val isProKey = stringPreferencesKey("widget_is_pro") // Using string for boolean
    val fontSizeScaleKey = floatPreferencesKey("widget_font_scale")
    val backgroundOpacityKey = floatPreferencesKey("widget_opacity_scale")
}