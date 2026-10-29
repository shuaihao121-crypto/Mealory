package com.mealory.app.ui.addmeal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.repository.MealPhotoSource
import com.mealory.app.data.repository.MealInputLimits
import com.mealory.app.data.repository.MealRecordDraft
import com.mealory.app.storage.CameraCapture
import com.mealory.app.util.MealTypeClassifier
import com.mealory.app.util.toUserFacingMessage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AddMealMode {
    CAMERA,
    PICKER,
    NO_PHOTO,
}

data class SelectedPhoto(
    val uri: Uri,
    val disposableFile: File? = null,
)

data class AddMealUiState(
    val date: LocalDate = LocalDate.now(),
    val minuteOfDay: Int = LocalTime.now().let { it.hour * 60 + it.minute },
    val mealType: MealType = MealTypeClassifier.classify(
        LocalTime.now().let { it.hour * 60 + it.minute },
    ),
    val title: String = "",
    val place: String = "",
    val people: String = "",
    val note: String = "",
    val photos: List<SelectedPhoto> = emptyList(),
    val coverIndex: Int = 0,
    val isSaving: Boolean = false,
    val savedMealId: String? = null,
    val errorMessage: String? = null,
)

class AddMealViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val container = (application as MealoryApplication).container
    private val _state = MutableStateFlow(AddMealUiState())
    val state: StateFlow<AddMealUiState> = _state.asStateFlow()
    val availablePeople: StateFlow<List<PersonEntity>> = container.mealRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentPlaces: StateFlow<List<PlaceEntity>> = container.mealRepository.observeRecentlyUsedPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val initialMode: AddMealMode = runCatching {
        AddMealMode.valueOf(savedStateHandle.get<String>("mode") ?: AddMealMode.NO_PHOTO.name)
    }.getOrDefault(AddMealMode.NO_PHOTO)

    private var pendingCameraCapture: CameraCapture? = null

    fun createCameraCapture(): Uri {
        container.photoStorage.deleteCameraCapture(pendingCameraCapture)
        return container.photoStorage.createCameraCapture().also {
            pendingCameraCapture = it
        }.uri
    }

    fun onCameraResult(success: Boolean) {
        val capture = pendingCameraCapture
        pendingCameraCapture = null
        if (success && capture != null) addPhotos(listOf(SelectedPhoto(capture.uri, capture.file)))
        else container.photoStorage.deleteCameraCapture(capture)
    }

    fun onPhotosPicked(uris: List<Uri>) {
        addPhotos(uris.map { SelectedPhoto(it) })
    }

    private fun addPhotos(candidates: List<SelectedPhoto>) {
        val wasEmpty = _state.value.photos.isEmpty()
        val remaining = (9 - _state.value.photos.size).coerceAtLeast(0)
        val accepted = candidates.take(remaining)
        if (accepted.isEmpty()) {
            _state.update { it.copy(errorMessage = "一顿饭最多保存 9 张照片。") }
            return
        }
        _state.update {
            it.copy(
                photos = it.photos + accepted,
                errorMessage = if (candidates.size > remaining) "已保留前 9 张照片。" else null,
            )
        }

        if (wasEmpty) {
            viewModelScope.launch(Dispatchers.IO) {
                val takenAt = container.exifReader.read(accepted.first().uri).originalTakenAt ?: return@launch
                val local = Instant.ofEpochMilli(takenAt).atZone(ZoneId.systemDefault())
                val minute = local.hour * 60 + local.minute
                _state.update {
                    it.copy(
                        date = local.toLocalDate(),
                        minuteOfDay = minute,
                        mealType = MealTypeClassifier.classify(minute),
                    )
                }
            }
        }
    }

    fun removePhoto(index: Int) {
        val photos = _state.value.photos.toMutableList()
        val removed = photos.removeAt(index)
        removed.disposableFile?.delete()
        _state.update {
            it.copy(
                photos = photos,
                coverIndex = when {
                    photos.isEmpty() -> 0
                    index < it.coverIndex -> it.coverIndex - 1
                    it.coverIndex >= photos.size -> photos.lastIndex
                    else -> it.coverIndex
                },
            )
        }
    }

    fun movePhoto(index: Int, offset: Int) {
        val photos = _state.value.photos.toMutableList()
        val target = index + offset
        if (index !in photos.indices || target !in photos.indices) return
        val coverPhoto = photos.getOrNull(_state.value.coverIndex)
        val item = photos.removeAt(index)
        photos.add(target, item)
        _state.update { it.copy(photos = photos, coverIndex = photos.indexOf(coverPhoto).coerceAtLeast(0)) }
    }

    fun setCover(index: Int) = _state.update { it.copy(coverIndex = index) }
    fun setDate(value: LocalDate) = _state.update { it.copy(date = value) }
    fun setTime(hour: Int, minute: Int) = _state.update {
        val minuteOfDay = hour * 60 + minute
        it.copy(minuteOfDay = minuteOfDay, mealType = MealTypeClassifier.classify(minuteOfDay))
    }
    fun setMealType(value: MealType) = _state.update { it.copy(mealType = value) }
    fun setTitle(value: String) = _state.update { it.copy(title = value.take(MealInputLimits.TITLE)) }
    fun setPlace(value: String) = _state.update { it.copy(place = value.take(MealInputLimits.TAG_NAME)) }
    fun setPeople(value: String) = _state.update { it.copy(people = value.take(MealInputLimits.PEOPLE_FIELD)) }
    fun togglePerson(name: String) = _state.update { current ->
        val names = current.people.split('，', ',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toMutableList()
        val existingIndex = names.indexOfFirst { it.equals(name, ignoreCase = true) }
        if (existingIndex >= 0) names.removeAt(existingIndex) else names += name
        current.copy(people = names.joinToString("，").take(MealInputLimits.PEOPLE_FIELD))
    }
    fun setNote(value: String) = _state.update { it.copy(note = value.take(MealInputLimits.NOTE)) }

    fun save() {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val current = _state.value
                val settings = container.settingsRepository.settings.first()
                withContext(Dispatchers.IO) {
                    container.mealRecorder.record(
                        MealRecordDraft(
                            date = current.date,
                            minuteOfDay = current.minuteOfDay,
                            mealType = current.mealType,
                            title = current.title,
                            note = current.note,
                            placeName = current.place,
                            peopleNames = current.people.split('，', ',').map(String::trim),
                            photos = current.photos.map { MealPhotoSource(it.uri, it.disposableFile) },
                            coverPhotoIndex = current.coverIndex,
                            photoQuality = settings.photoQuality,
                        ),
                    )
                }
            }.onSuccess { mealId ->
                _state.update { it.copy(isSaving = false, savedMealId = mealId) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toUserFacingMessage("没有保存成功，请再试一次"),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        container.photoStorage.deleteCameraCapture(pendingCameraCapture)
        if (_state.value.savedMealId == null) {
            _state.value.photos.forEach { it.disposableFile?.delete() }
        }
    }
}
