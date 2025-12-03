package com.ahmedgamal.aquamemo.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val CANDLE_1_INTERVAL_KEY = intPreferencesKey("candle_1_interval")
private val CANDLE_2_INTERVAL_KEY = intPreferencesKey("candle_2_interval")
private val CANDLE_3_INTERVAL_KEY = intPreferencesKey("candle_3_interval")
private val CANDLE_4_INTERVAL_KEY = intPreferencesKey("candle_4_interval")
private val CANDLE_5_INTERVAL_KEY = intPreferencesKey("candle_5_interval")
private val CANDLE_6_INTERVAL_KEY = intPreferencesKey("candle_6_interval")
private val CANDLE_7_INTERVAL_KEY = intPreferencesKey("candle_7_interval")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val remindersEnabledKey = booleanPreferencesKey("reminders_enabled")
    private val reminderTimeKey = stringPreferencesKey("reminder_time")
    private val fontSizeKey = stringPreferencesKey("font_size")
    private val selectedCurrencyKey = stringPreferencesKey("selected_currency")
    private val themePreferenceKey = stringPreferencesKey("theme_preference")
    val technicianphonekey = stringPreferencesKey("technician_phone")
    val remindersEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[remindersEnabledKey] ?: true
        }
        .distinctUntilChanged()

    val reminderTimeFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[reminderTimeKey] ?: "09:00"
        }
        .distinctUntilChanged()

    val fontSizeFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[fontSizeKey] ?: "medium"
        }
        .distinctUntilChanged()

    val selectedCurrencyFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[selectedCurrencyKey] ?: "USD"
        }

    val themePreferenceFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[themePreferenceKey] ?: "system" // Default to system
        }
        .distinctUntilChanged()

    val technicianPhone: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[technicianphonekey] ?: ""
        }

    // أضف هذه الدالة لحفظ الرقم
    suspend fun saveTechnicianPhone(phone: String) {
        dataStore.edit { preferences ->
            preferences[technicianphonekey] = phone
        }
    }


    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[remindersEnabledKey] = enabled
        }
    }

    suspend fun setReminderTime(time: String) {
        dataStore.edit { preferences ->
            preferences[reminderTimeKey] = time
        }
    }

    suspend fun setFontSize(fontSize: String) {
        dataStore.edit { preferences ->
            preferences[fontSizeKey] = fontSize
        }
    }
    suspend fun setSelectedCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[selectedCurrencyKey] = currency
        }
    }
    suspend fun setThemePreference(theme: String) {
        dataStore.edit { preferences ->
            preferences[themePreferenceKey] = theme
        }
    }
    fun getIntervalForCandle(candleNumber: Int): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[getKeyForCandle(candleNumber)] ?: getDefaultIntervalForCandle(candleNumber)
        }
    }
    suspend fun setIntervalForCandle(candleNumber: Int, months: Int) {
        dataStore.edit { preferences ->
            preferences[getKeyForCandle(candleNumber)] = months
        }
    }
    suspend fun resetAllIntervalsToDefault() {
        dataStore.edit { preferences ->
            (1..7).forEach { candleNumber ->
                preferences.remove(getKeyForCandle(candleNumber))
            }
        }
    }
    private fun getKeyForCandle(candleNumber: Int): Preferences.Key<Int> {
        return when (candleNumber) {
            1 -> CANDLE_1_INTERVAL_KEY
            2 -> CANDLE_2_INTERVAL_KEY
            3 -> CANDLE_3_INTERVAL_KEY
            4 -> CANDLE_4_INTERVAL_KEY
            5 -> CANDLE_5_INTERVAL_KEY
            6 -> CANDLE_6_INTERVAL_KEY
            7 -> CANDLE_7_INTERVAL_KEY
            else -> throw IllegalArgumentException("Invalid candle number")
        }
    }

    /**
     * دالة مساعدة للحصول على المدة الافتراضية
     */
    private fun getDefaultIntervalForCandle(candleNumber: Int): Int {
        return when (candleNumber) {
            1 -> 3    // المرحلة الأولى - 3 أشهر
            2, 3 -> 6 // المرحلة الثانية والثالثة - 6 أشهر
            4 -> 18   // المرحلة الرابعة - 18 شهراً
            5 -> 8    // المرحلة الخامسة - 8 أشهر
            6, 7 -> 18 // المرحلة السادسة والسابعة - 18 شهراً
            else -> 0
        }
    }
}