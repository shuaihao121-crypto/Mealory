package com.mealory.app.backup

import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.preferences.MealorySettings

data class BackupCounts(
    val meals: Int,
    val photos: Int,
    val people: Int,
    val places: Int,
    val mealPeople: Int,
)

data class BackupPhotoFile(
    val photoId: String,
    val zipPath: String,
    val size: Long,
    val sha256: String,
)

data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val counts: BackupCounts,
    val photoFiles: List<BackupPhotoFile>,
)

data class BackupSnapshot(
    val meals: List<MealEntity>,
    val photos: List<MealPhotoEntity>,
    val people: List<PersonEntity>,
    val places: List<PlaceEntity>,
    val mealPeople: List<MealPersonCrossRef>,
    val settings: MealorySettings,
) {
    val counts: BackupCounts
        get() = BackupCounts(meals.size, photos.size, people.size, places.size, mealPeople.size)
}

data class BackupPreview(
    val createdAt: Long,
    val appVersion: String,
    val counts: BackupCounts,
)

data class BackupOperationResult(
    val mealCount: Int,
    val photoCount: Int,
    val completedAt: Long,
)

const val BACKUP_FORMAT_VERSION = 1
