package com.ahmedgamal.aquamemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedgamal.aquamemo.data.TdsRepository
import com.ahmedgamal.aquamemo.data.model.TdsReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TdsViewModel @Inject constructor(
    private val repository: TdsRepository
) : ViewModel() {

    // ✅ حالة القائمة (List State): تحافظ على البيانات حتى عند تدوير الشاشة
    val readings = repository.allReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // توقف الجمع بعد 5 ثواني من الخروج
            initialValue = emptyList()
        )

    // ✅ حالة الرسم البياني (Chart Data)
    val chartData = repository.chartReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ✅ آخر قراءة مسجلة (Last Value)
    val lastReading = repository.lastReading
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ✅ دالة إضافة قراءة جديدة
    fun addReading(tdsValueStr: String, notes: String = "") {
        // تحويل النص لرقم، وإذا كان غير صالح (null) لا نفعل شيئاً
        val value = tdsValueStr.toIntOrNull()
        if (value != null) {
            viewModelScope.launch {
                repository.insertReading(
                    value = value,
                    date = System.currentTimeMillis(), // نستخدم الوقت الحالي
                    notes = notes
                )
            }
        }
    }

    // ✅ دالة حذف قراءة
    fun deleteReading(reading: TdsReading) {
        viewModelScope.launch {
            repository.deleteReading(reading)
        }
    }
}