package com.mealory.app.data.repository

import android.net.Uri
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.storage.PhotoProcessor
import com.mealory.app.storage.PhotoStorage
import com.mealory.app.storage.PreparedPhoto
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class MealPhotoSource(
    val uri: Uri,
    val disposableFile: File? = null,
)

data class MealRecordDraft(
    val date: LocalDate,
    val minuteOfDay: Int,
    val mealType: MealType,
    val title: String = "",
    val note: String = "",
    val placeName: String = "",
    val peopleNames: List<String> = emptyList(),
    val photos: List<MealPhotoSource> = emptyList(),
    val coverPhotoIndex: Int = 0,
    val photoQuality: PhotoQuality = PhotoQuality.STANDARD,
    val timeZoneId: String = ZoneId.systemDefault().id,
)

class MealRecorder(
    private val repository: MealRepository,
    private val photoProcessor: PhotoProcessor,
    private val photoStorage: PhotoStorage,
) {
    suspend fun record(draft: MealRecordDraft): String {
        require(draft.minuteOfDay in 0..1_439)
        require(draft.photos.size <= MealRepository.MAX_PHOTOS_PER_MEAL)
        require(draft.photos.isEmpty() || draft.coverPhotoIndex in draft.photos.indices)

        val mealId = UUID.randomUUID().toString()
        val prepared = mutableListOf<PreparedPhoto>()
        try {
            draft.photos.forEach { source ->
                prepared += photoProcessor.prepare(
                    source = source.uri,
                    mealId = mealId,
                    photoId = UUID.randomUUID().toString(),
                    date = draft.date,
                    quality = draft.photoQuality,
                )
            }
        } catch (error: Throwable) {
            prepared.forEach(photoStorage::discard)
            throw error
        }

        try {
            prepared.forEach(photoStorage::commit)
            val now = System.currentTimeMillis()
            val place = draft.placeName.trim().takeIf(String::isNotEmpty)?.let { name ->
                PlaceEntity(UUID.randomUUID().toString(), name, now, now)
            }
            val people = draft.peopleNames
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .map { name -> PersonEntity(UUID.randomUUID().toString(), name, now, now) }
            val photoEntities = prepared.mapIndexed { index, photo ->
                MealPhotoEntity(
                    id = photo.id,
                    mealId = mealId,
                    relativePath = photo.relativePath,
                    sortOrder = index,
                    width = photo.width,
                    height = photo.height,
                    fileSize = photo.fileSize,
                    mimeType = "image/jpeg",
                    originalTakenAt = photo.originalTakenAt,
                    createdAt = now,
                )
            }
            val meal = MealEntity(
                id = mealId,
                dateEpochDay = draft.date.toEpochDay(),
                minuteOfDay = draft.minuteOfDay,
                mealType = draft.mealType,
                title = draft.title.trim().takeIf(String::isNotEmpty),
                note = draft.note.trim().takeIf(String::isNotEmpty),
                placeId = place?.id,
                coverPhotoId = prepared.getOrNull(draft.coverPhotoIndex)?.id,
                timeZoneId = draft.timeZoneId,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
            repository.createMeal(
                meal = meal,
                photos = photoEntities,
                newPlace = place,
                newPeople = people,
                personIds = people.map { it.id }.toSet(),
            )
            draft.photos.forEach { it.disposableFile?.delete() }
            return mealId
        } catch (error: Throwable) {
            prepared.forEach(photoStorage::discard)
            throw error
        }
    }
}
