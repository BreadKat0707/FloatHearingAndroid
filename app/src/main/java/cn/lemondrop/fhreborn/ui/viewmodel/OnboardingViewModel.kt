package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.repository.MediaLibraryRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.scanner.MediaScanner
import cn.lemondrop.fhreborn.scanner.ScanProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val mediaRepository = MediaLibraryRepository(AppDatabase.getInstance(application))
    private val scanner = MediaScanner(application)

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun startFirstScan() {
        viewModelScope.launch {
            _isScanning.value = true

            scanner.scan().collect { progress ->
                _scanProgress.value = progress
                if (progress is ScanProgress.Completed && progress.songs.isNotEmpty()) {
                    mediaRepository.insertSongs(progress.songs)
                }
            }

            settingsRepository.setFirstScanDone(true)
            _isScanning.value = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(application) as T
        }
    }
}
