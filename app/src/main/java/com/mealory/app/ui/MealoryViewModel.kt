package com.mealory.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.preferences.MealorySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealoryViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = (application as MealoryApplication).container.settingsRepository

    val settings: StateFlow<MealorySettings?> = settingsRepository.settings
        .map { it as MealorySettings? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun acceptPrivacyPolicy(version: Int) {
        viewModelScope.launch {
            settingsRepository.setPrivacyPolicyVersionAccepted(version)
        }
    }
}
