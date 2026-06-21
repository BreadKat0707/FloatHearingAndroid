package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.model.SettingItem
import cn.lemondrop.fhreborn.data.model.SettingType
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppSettingsRepository(application)

    private val db = AppDatabase.getInstance(application)
    private val statisticsRepository = PlayStatisticsRepository(db.playRecordDao())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun selectCategory(key: String?) {
        _selectedCategory.value = key
    }

    fun navigateBack() {
        _selectedCategory.value = null
    }

    // 通用开关切换
    fun toggleSetting(item: SettingItem) {
        if (item.type !is SettingType.Toggle) return
        viewModelScope.launch {
            val current = repository.getBoolean(item.key, item.defaultValue as? Boolean ?: false).first()
            repository.setBoolean(item.key, !current)
        }
    }

    fun getToggleValue(key: String, default: Boolean = false) = repository.getBoolean(key, default)

    fun getStringValue(key: String, default: String = "") = repository.getString(key, default)

    fun getIntValue(key: String, default: Int = 0) = repository.getInt(key, default)

    fun setIntSetting(key: String, value: Int) {
        viewModelScope.launch {
            repository.setInt(key, value)
        }
    }

    fun setStringSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.setString(key, value)
        }
    }

    // 统计数据
    val totalPlayDuration: Flow<Long?> = statisticsRepository.getTotalPlayDuration()
    val totalPlayCount: Flow<Int?> = statisticsRepository.getTotalPlayCount()
    val todayPlayDuration: Flow<Long?> = statisticsRepository.getTodayPlayDuration()
    val todayPlayCount: Flow<Int?> = statisticsRepository.getTodayPlayCount()
    val weekPlayDuration: Flow<Long?> = statisticsRepository.getWeekPlayDuration()
    val mostPlayed = statisticsRepository.getMostPlayed(10)

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
