package com.mealory.app.ui.mealdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    private val mealId: String = checkNotNull(savedStateHandle["mealId"])

    val meal: StateFlow<MealWithDetails?> = repository.observeMeal(mealId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val deleted = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    fun saveEdits(
        date: LocalDate,
        minuteOfDay: Int,
        mealType: MealType,
        title: String,
        note: String,
    ) {
        val current = meal.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.updateMeal(
                    current.meal.copy(
                        dateEpochDay = date.toEpochDay(),
                        minuteOfDay = minuteOfDay,
                        mealType = mealType,
                        title = title.trim().takeIf(String::isNotEmpty),
                        note = note.trim().takeIf(String::isNotEmpty),
                        updatedAt = System.currentTimeMillis(),
                    ),
                    current.people.map { it.id }.toSet(),
                )
            }.onFailure { errorMessage.value = it.message }
        }
    }

    fun setCover(photoId: String) {
        viewModelScope.launch {
            runCatching {
                repository.setCoverPhoto(mealId, photoId, System.currentTimeMillis())
            }.onFailure { errorMessage.value = it.message }
        }
    }

    fun deleteMeal() {
        viewModelScope.launch {
            runCatching { repository.softDelete(mealId, System.currentTimeMillis()) }
                .onSuccess { deleted.value = true }
                .onFailure { errorMessage.value = it.message }
        }
    }
}
