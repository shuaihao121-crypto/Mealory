package com.mealory.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileStats(
    val firstDateEpochDay: Long? = null,
    val mealCount: Long = 0,
    val recordedDayCount: Long = 0,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository

    val stats: StateFlow<ProfileStats> = combine(
        repository.observeFirstActiveMealDate(),
        repository.observeActiveMealCount(),
        repository.observeActiveRecordedDayCount(),
    ) { firstDate, meals, days -> ProfileStats(firstDate, meals, days) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())
}
