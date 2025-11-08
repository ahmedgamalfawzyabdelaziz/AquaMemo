package com.ahmedgamal.aquamemo.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
    private val REMINDER_TIME_KEY = stringPreferencesKey("reminder_time")
    private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
    private val SELECTED_CURRENCY_KEY = stringPreferencesKey("selected_currency")
    private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
    // ✅ REMOVED: NOTIFICATION_TONE_URI_KEY

    val remindersEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[REMINDERS_ENABLED_KEY] ?: true
        }
        .distinctUntilChanged()

    val reminderTimeFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[REMINDER_TIME_KEY] ?: "09:00"
        }
        .distinctUntilChanged()

    val fontSizeFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[FONT_SIZE_KEY] ?: "medium"
        }
        .distinctUntilChanged()

    val selectedCurrencyFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[SELECTED_CURRENCY_KEY] ?: "USD"
        }

    val themePreferenceFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[THEME_PREFERENCE_KEY] ?: "system" // Default to system
        }
        .distinctUntilChanged()

    // ✅ REMOVED: notificationToneUriFlow

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMINDERS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setReminderTime(time: String) {
        dataStore.edit { preferences ->
            preferences[REMINDER_TIME_KEY] = time
        }
    }

    suspend fun setFontSize(fontSize: String) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize
        }
    }
    suspend fun setSelectedCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_CURRENCY_KEY] = currency
        }
    }
    suspend fun setThemePreference(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = theme
        }
    }
}