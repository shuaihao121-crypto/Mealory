package com.mealory.app.backup

import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.preferences.MealTimeRanges
import com.mealory.app.data.preferences.MealorySettings
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.preferences.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

object BackupJson {
    fun manifestToString(value: BackupManifest): String = JSONObject()
        .put("formatVersion", value.formatVersion)
        .put("appVersion", value.appVersion)
        .put("createdAt", value.createdAt)
        .put("counts", value.counts.toJson())
        .put("photoFiles", JSONArray().apply { value.photoFiles.forEach { put(it.toJson()) } })
        .toString(2)

    fun manifestFromString(value: String): BackupManifest {
        val json = JSONObject(value)
        return BackupManifest(
            formatVersion = json.getInt("formatVersion"),
            appVersion = json.getString("appVersion"),
            createdAt = json.getLong("createdAt"),
            counts = json.getJSONObject("counts").toCounts(),
            photoFiles = json.getJSONArray("photoFiles").mapObjects { it.toPhotoFile() },
        )
    }

    fun mealsToString(values: List<MealEntity>): String = values.toJsonArray { meal ->
        JSONObject()
            .put("id", meal.id)
            .put("dateEpochDay", meal.dateEpochDay)
            .put("minuteOfDay", meal.minuteOfDay)
            .put("mealType", meal.mealType.name)
            .putNullable("title", meal.title)
            .putNullable("note", meal.note)
            .putNullable("placeId", meal.placeId)
            .putNullable("coverPhotoId", meal.coverPhotoId)
            .put("timeZoneId", meal.timeZoneId)
            .put("createdAt", meal.createdAt)
            .put("updatedAt", meal.updatedAt)
            .putNullable("deletedAt", meal.deletedAt)
    }

    fun mealsFromString(value: String): List<MealEntity> = JSONArray(value).mapObjects { json ->
        MealEntity(
            id = json.getString("id"),
            dateEpochDay = json.getLong("dateEpochDay"),
            minuteOfDay = json.getInt("minuteOfDay"),
            mealType = MealType.valueOf(json.getString("mealType")),
            title = json.nullableString("title"),
            note = json.nullableString("note"),
            placeId = json.nullableString("placeId"),
            coverPhotoId = json.nullableString("coverPhotoId"),
            timeZoneId = json.getString("timeZoneId"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
        )
    }

    fun photosToString(values: List<MealPhotoEntity>): String = values.toJsonArray { photo ->
        JSONObject()
            .put("id", photo.id)
            .put("mealId", photo.mealId)
            .put("relativePath", photo.relativePath)
            .put("sortOrder", photo.sortOrder)
            .put("width", photo.width)
            .put("height", photo.height)
            .put("fileSize", photo.fileSize)
            .put("mimeType", photo.mimeType)
            .putNullable("originalTakenAt", photo.originalTakenAt)
            .put("createdAt", photo.createdAt)
    }

    fun photosFromString(value: String): List<MealPhotoEntity> = JSONArray(value).mapObjects { json ->
        MealPhotoEntity(
            id = json.getString("id"),
            mealId = json.getString("mealId"),
            relativePath = json.getString("relativePath"),
            sortOrder = json.getInt("sortOrder"),
            width = json.getInt("width"),
            height = json.getInt("height"),
            fileSize = json.getLong("fileSize"),
            mimeType = json.getString("mimeType"),
            originalTakenAt = json.nullableLong("originalTakenAt"),
            createdAt = json.getLong("createdAt"),
        )
    }

    fun peopleToString(values: List<PersonEntity>): String = values.toJsonArray { person ->
        JSONObject().put("id", person.id).put("name", person.name)
            .put("createdAt", person.createdAt).put("updatedAt", person.updatedAt)
    }

    fun peopleFromString(value: String): List<PersonEntity> = JSONArray(value).mapObjects { json ->
        PersonEntity(json.getString("id"), json.getString("name"), json.getLong("createdAt"), json.getLong("updatedAt"))
    }

    fun placesToString(values: List<PlaceEntity>): String = values.toJsonArray { place ->
        JSONObject().put("id", place.id).put("name", place.name)
            .put("createdAt", place.createdAt).put("updatedAt", place.updatedAt)
    }

    fun placesFromString(value: String): List<PlaceEntity> = JSONArray(value).mapObjects { json ->
        PlaceEntity(json.getString("id"), json.getString("name"), json.getLong("createdAt"), json.getLong("updatedAt"))
    }

    fun mealPeopleToString(values: List<MealPersonCrossRef>): String = values.toJsonArray { relation ->
        JSONObject().put("mealId", relation.mealId).put("personId", relation.personId)
    }

    fun mealPeopleFromString(value: String): List<MealPersonCrossRef> = JSONArray(value).mapObjects { json ->
        MealPersonCrossRef(json.getString("mealId"), json.getString("personId"))
    }

    fun settingsToString(value: MealorySettings): String = JSONObject()
        .put("onboardingCompleted", value.onboardingCompleted)
        .put("privacyPolicyVersionAccepted", value.privacyPolicyVersionAccepted)
        .put("themeMode", value.themeMode.name)
        .put("photoQuality", value.photoQuality.name)
        .putNullable("lastBackupAt", value.lastBackupAt)
        .put("defaultMealTimeRanges", JSONObject(value.defaultMealTimeRanges.toJson()))
        .toString(2)

    fun settingsFromString(value: String): MealorySettings {
        val json = JSONObject(value)
        return MealorySettings(
            onboardingCompleted = json.getBoolean("onboardingCompleted"),
            privacyPolicyVersionAccepted = json.optInt("privacyPolicyVersionAccepted", 0),
            themeMode = ThemeMode.valueOf(json.getString("themeMode")),
            photoQuality = PhotoQuality.valueOf(json.getString("photoQuality")),
            lastBackupAt = json.nullableLong("lastBackupAt"),
            defaultMealTimeRanges = MealTimeRanges.fromJson(json.getJSONObject("defaultMealTimeRanges").toString()),
        )
    }
}

private fun BackupCounts.toJson(): JSONObject = JSONObject()
    .put("meals", meals).put("photos", photos).put("people", people)
    .put("places", places).put("mealPeople", mealPeople)

private fun JSONObject.toCounts() = BackupCounts(
    meals = getInt("meals"), photos = getInt("photos"), people = getInt("people"),
    places = getInt("places"), mealPeople = getInt("mealPeople"),
)

private fun BackupPhotoFile.toJson(): JSONObject = JSONObject()
    .put("photoId", photoId).put("zipPath", zipPath).put("size", size).put("sha256", sha256)

private fun JSONObject.toPhotoFile() = BackupPhotoFile(
    photoId = getString("photoId"), zipPath = getString("zipPath"),
    size = getLong("size"), sha256 = getString("sha256"),
)

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

private inline fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): String =
    JSONArray().apply { forEach { put(transform(it)) } }.toString(2)

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }
