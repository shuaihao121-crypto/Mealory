package com.mealory.app.ui.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PersonSummary
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.database.entity.PlaceSummary
import com.mealory.app.util.toUserFacingMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeopleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val people: StateFlow<List<PersonSummary>> = repository.observePersonSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val errorMessage = MutableStateFlow<String?>(null)

    fun create(name: String) = launchOperation { repository.createPerson(name) }
    fun rename(id: String, name: String) = launchOperation { repository.renamePerson(id, name) }
    fun delete(id: String) = launchOperation { repository.deletePerson(id) }

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            errorMessage.value = null
            runCatching { block() }.onFailure { errorMessage.value = it.toUserFacingMessage("操作没有完成") }
        }
    }
}

class PlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val places: StateFlow<List<PlaceSummary>> = repository.observePlaceSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val errorMessage = MutableStateFlow<String?>(null)

    fun create(name: String) = launchOperation { repository.createPlace(name) }
    fun rename(id: String, name: String) = launchOperation { repository.renamePlace(id, name) }
    fun delete(id: String) = launchOperation { repository.deletePlace(id) }

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            errorMessage.value = null
            runCatching { block() }.onFailure { errorMessage.value = it.toUserFacingMessage("操作没有完成") }
        }
    }
}

class PersonDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    private val personId: String = checkNotNull(savedStateHandle["personId"])
    val person: StateFlow<PersonEntity?> = repository.observePerson(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val meals: StateFlow<List<MealWithDetails>> = repository.observeMealsForPerson(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class PlaceDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    private val placeId: String = checkNotNull(savedStateHandle["placeId"])
    val place: StateFlow<PlaceEntity?> = repository.observePlace(placeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val meals: StateFlow<List<MealWithDetails>> = repository.observeMealsForPlace(placeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
