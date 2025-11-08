package com.ahmedgamal.aquamemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedgamal.aquamemo.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ahmedgamal.aquamemo.billing.BillingManager

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    val billingManager: BillingManager
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = settingsRepository.remindersEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderTime: StateFlow<String> = settingsRepository.reminderTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "09:00")

    val fontSize: StateFlow<String> = settingsRepository.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "medium")

    val themePreference: StateFlow<String> = settingsRepository.themePreferenceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    // ✅ Expose Pro status from BillingManager
    val isPro: StateFlow<Boolean> = billingManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
        }
    }
    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
        }
    }
    fun setFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsRepository.setFontSize(fontSize)
        }
    }
    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
            updateWidgetData()
        }
    }


    private fun updateWidgetData() {
        val workRequest = OneTimeWorkRequestBuilder<com.ahmedgamal.aquamemo.widget.WidgetUpdateWorker>().build()
        WorkManager.getInstance(app.applicationContext).enqueue(workRequest)
    }
    fun acknowledgePurchase(purchaseToken: String) {
        viewModelScope.launch {
            // قم باستدعاء billingManager.acknowledgePurchase(purchaseToken)
        }
    }
}