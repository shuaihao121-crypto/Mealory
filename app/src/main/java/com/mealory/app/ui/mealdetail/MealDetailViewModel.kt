package com.mealory.app.ui.mealdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.util.toUserFacingMessage
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
    val people: StateFlow<List<PersonEntity>> = repository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val places: StateFlow<List<PlaceEntity>> = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveEdits(
        date: LocalDate,
        minuteOfDay: Int,
        mealType: MealType,
        title: String,
        note: String,
        placeName: String,
        peopleNames: String,
    ) {
        val current = meal.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.updateMealWithTags(
                    meal = current.meal.copy(
                        dateEpochDay = date.toEpochDay(),
                        minuteOfDay = minuteOfDay,
                        mealType = mealType,
                        title = title.trim().takeIf(String::isNotEmpty),
                        note = note.trim().takeIf(String::isNotEmpty),
                        updatedAt = System.currentTimeMillis(),
                    ),
                    placeName = placeName,
                    peopleNames = peopleNames.split('，', ',').map(String::trim),
                )
            }.onFailure { errorMessage.value = it.toUserFacingMessage("修改没有保存成功") }
        }
    }

    fun setCover(photoId: String) {
        viewModelScope.launch {
            runCatching {
                repository.setCoverPhoto(mealId, photoId, System.currentTimeMillis())
            }.onFailure { errorMessage.value = it.toUserFacingMessage("封面没有更新成功") }
        }
    }

    fun deleteMeal() {
        viewModelScope.launch {
            runCatching { repository.softDelete(mealId, System.currentTimeMillis()) }
                .onSuccess { deleted.value = true }
                .onFailure { errorMessage.value = it.toUserFacingMessage("删除没有完成") }
        }
    }
}
