package com.mealory.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.PhotoStorageStats
import com.mealory.app.data.preferences.MealorySettings
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: MealorySettings = MealorySettings(),
    val storage: PhotoStorageStats = PhotoStorageStats(0, 0),
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MealoryApplication).container

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.settings,
        container.mealRepository.observePhotoStorageStats(),
    ) { settings, storage -> SettingsUiState(settings, storage) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setPhotoQuality(quality: PhotoQuality) {
        viewModelScope.launch { container.settingsRepository.setPhotoQuality(quality) }
    }
}
