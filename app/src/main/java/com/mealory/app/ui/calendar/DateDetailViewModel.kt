package com.mealory.app.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DateDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val epochDay: Long = checkNotNull(savedStateHandle["epochDay"])

    val meals: StateFlow<List<MealWithDetails>> = repository.observeMealsForDay(epochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
