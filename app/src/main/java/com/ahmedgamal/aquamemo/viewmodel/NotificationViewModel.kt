package com.ahmedgamal.aquamemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedgamal.aquamemo.data.FilterRepository
import com.ahmedgamal.aquamemo.data.model.NotificationHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val filterRepository: FilterRepository
) : ViewModel() {

    // جلب كل الإشعارات من قاعدة البيانات ومراقبتها
    val allNotifications: StateFlow<List<NotificationHistory>> =
        filterRepository.getAllNotifications()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /**
     * دالة لتحديد كل الإشعارات كمقروءة
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            filterRepository.markAllNotificationsAsRead()
        }
    }
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000) // تأخير وهمي لإظهار مؤشر التحديث
            _isRefreshing.value = false
        }
    }

    // ✅ 3. (جديد) إضافة دالة حذف الكل
    fun clearAllNotifications() {
        viewModelScope.launch {
            filterRepository.clearAllNotifications()
        }
    }
}