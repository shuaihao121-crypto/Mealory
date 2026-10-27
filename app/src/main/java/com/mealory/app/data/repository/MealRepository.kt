package com.mealory.app.data.repository

import androidx.room.withTransaction
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.database.entity.CalendarMealPreview
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PersonSummary
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.database.entity.PlaceSummary
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class MealRepository(
    private val database: MealoryDatabase,
) {
    private val mealDao = database.mealDao()
    private val photoDao = database.mealPhotoDao()
    private val personDao = database.personDao()
    private val placeDao = database.placeDao()

    fun observeActiveMeals(): Flow<List<MealWithDetails>> = mealDao.observeActiveMeals()

    fun observeDeletedMeals(): Flow<List<MealWithDetails>> = mealDao.observeDeletedMeals()

    fun observeMeal(mealId: String): Flow<MealWithDetails?> = mealDao.observeById(mealId)

    fun pagedActiveMeals(query: String): Flow<PagingData<MealWithDetails>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 10,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { mealDao.pagingSource(query.escapeLikeQuery()) },
    ).flow

    fun observeCalendarPreviews(
        startEpochDay: Long,
        endEpochDayExclusive: Long,
    ): Flow<List<CalendarMealPreview>> =
        mealDao.observeCalendarPreviews(startEpochDay, endEpochDayExclusive)

    fun observeMealsForDay(epochDay: Long): Flow<List<MealWithDetails>> =
        mealDao.observeMealsForDay(epochDay)

    fun observePeople(): Flow<List<PersonEntity>> = personDao.observeAll()

    fun observePersonSummaries(): Flow<List<PersonSummary>> = personDao.observeSummaries()

    fun observePerson(personId: String): Flow<PersonEntity?> = personDao.observeById(personId)

    fun observeMealsForPerson(personId: String): Flow<List<MealWithDetails>> = personDao.observeMeals(personId)

    fun observePlaces(): Flow<List<PlaceEntity>> = placeDao.observeAll()

    fun observePlaceSummaries(): Flow<List<PlaceSummary>> = placeDao.observeSummaries()

    fun observePlace(placeId: String): Flow<PlaceEntity?> = placeDao.observeById(placeId)

    fun observeMealsForPlace(placeId: String): Flow<List<MealWithDetails>> = placeDao.observeMeals(placeId)

    fun observeRecentlyUsedPlaces(limit: Int = 5): Flow<List<PlaceEntity>> =
        placeDao.observeRecentlyUsed(limit)

    suspend fun getMeal(mealId: String): MealWithDetails? = mealDao.getById(mealId)

    suspend fun findPersonByName(name: String): PersonEntity? =
        name.normalizedName().takeIf(String::isNotEmpty)?.let { personDao.getByName(it) }

    suspend fun findPlaceByName(name: String): PlaceEntity? =
        name.normalizedName().takeIf(String::isNotEmpty)?.let { placeDao.getByName(it) }

    suspend fun createPerson(name: String): String = database.withTransaction {
        val normalized = name.normalizedName()
        require(normalized.isNotEmpty()) { "Person name cannot be blank" }
        personDao.getByName(normalized)?.id ?: UUID.randomUUID().toString().also { id ->
            val now = System.currentTimeMillis()
            personDao.insert(PersonEntity(id, normalized, now, now))
        }
    }

    suspend fun renamePerson(personId: String, name: String) = database.withTransaction {
        val normalized = name.normalizedName()
        require(normalized.isNotEmpty()) { "Person name cannot be blank" }
        val current = checkNotNull(personDao.getById(personId)) { "Person does not exist: $personId" }
        val duplicate = personDao.getByName(normalized)
        require(duplicate == null || duplicate.id == personId) { "Person already exists: $normalized" }
        check(personDao.update(current.copy(name = normalized, updatedAt = System.currentTimeMillis())) == 1)
    }

    suspend fun deletePerson(personId: String): Boolean = personDao.deleteById(personId) == 1

    suspend fun createPlace(name: String): String = database.withTransaction {
        val normalized = name.normalizedName()
        require(normalized.isNotEmpty()) { "Place name cannot be blank" }
        placeDao.getByName(normalized)?.id ?: UUID.randomUUID().toString().also { id ->
            val now = System.currentTimeMillis()
            placeDao.insert(PlaceEntity(id, normalized, now, now))
        }
    }

    suspend fun renamePlace(placeId: String, name: String) = database.withTransaction {
        val normalized = name.normalizedName()
        require(normalized.isNotEmpty()) { "Place name cannot be blank" }
        val current = checkNotNull(placeDao.getById(placeId)) { "Place does not exist: $placeId" }
        val duplicate = placeDao.getByName(normalized)
        require(duplicate == null || duplicate.id == placeId) { "Place already exists: $normalized" }
        check(placeDao.update(current.copy(name = normalized, updatedAt = System.currentTimeMillis())) == 1)
    }

    suspend fun deletePlace(placeId: String): Boolean = placeDao.deleteById(placeId) == 1

    suspend fun updateMealWithTags(
        meal: MealEntity,
        placeName: String,
        peopleNames: List<String>,
    ) = database.withTransaction {
        val now = System.currentTimeMillis()
        val normalizedPlace = placeName.normalizedName()
        val place = normalizedPlace.takeIf(String::isNotEmpty)?.let { name ->
            placeDao.getByName(name) ?: PlaceEntity(UUID.randomUUID().toString(), name, now, now).also {
                placeDao.insert(it)
            }
        }
        val people = peopleNames
            .map(String::normalizedName)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .map { name ->
                personDao.getByName(name) ?: PersonEntity(UUID.randomUUID().toString(), name, now, now).also {
                    personDao.insert(it)
                }
            }
        val updated = meal.copy(placeId = place?.id, updatedAt = now)
        require(
            updated.coverPhotoId == null || photoDao.getForMeal(updated.id).any { it.id == updated.coverPhotoId },
        ) { "coverPhotoId must reference a photo belonging to the meal" }
        check(mealDao.update(updated) == 1) { "Meal does not exist: ${updated.id}" }
        mealDao.deletePeopleForMeal(updated.id)
        if (people.isNotEmpty()) {
            mealDao.insertPeople(people.map { MealPersonCrossRef(updated.id, it.id) })
        }
    }

    suspend fun createMeal(
        meal: MealEntity,
        photos: List<MealPhotoEntity> = emptyList(),
        newPlace: PlaceEntity? = null,
        newPeople: List<PersonEntity> = emptyList(),
        personIds: Set<String> = emptySet(),
    ) {
        require(meal.minuteOfDay in 0..1_439) { "minuteOfDay must be between 0 and 1439" }
        require(photos.size <= MAX_PHOTOS_PER_MEAL) { "A meal can contain at most 9 photos" }
        require(photos.all { it.mealId == meal.id }) { "Every photo must belong to the meal" }
        require(photos.map { it.id }.toSet().size == photos.size) { "Photo IDs must be unique" }
        require(meal.coverPhotoId == null || photos.any { it.id == meal.coverPhotoId }) {
            "coverPhotoId must reference one of the supplied photos"
        }

        database.withTransaction {
            if (newPlace != null) placeDao.insert(newPlace)
            newPeople.forEach { personDao.insert(it) }
            mealDao.insert(meal)
            if (photos.isNotEmpty()) photoDao.insertAll(photos)
            if (personIds.isNotEmpty()) {
                mealDao.insertPeople(personIds.map { MealPersonCrossRef(meal.id, it) })
            }
        }
    }

    suspend fun updateMeal(
        meal: MealEntity,
        personIds: Set<String>,
    ) {
        require(meal.minuteOfDay in 0..1_439) { "minuteOfDay must be between 0 and 1439" }
        database.withTransaction {
            require(
                meal.coverPhotoId == null ||
                    photoDao.getForMeal(meal.id).any { it.id == meal.coverPhotoId },
            ) { "coverPhotoId must reference a photo belonging to the meal" }
            check(mealDao.update(meal) == 1) { "Meal does not exist: ${meal.id}" }
            mealDao.deletePeopleForMeal(meal.id)
            if (personIds.isNotEmpty()) {
                mealDao.insertPeople(personIds.map { MealPersonCrossRef(meal.id, it) })
            }
        }
    }

    suspend fun softDelete(mealId: String, deletedAt: Long) {
        check(mealDao.softDelete(mealId, deletedAt, deletedAt) == 1) {
            "Meal does not exist or is already deleted: $mealId"
        }
    }

    suspend fun restore(mealId: String, restoredAt: Long) {
        check(mealDao.restore(mealId, restoredAt) == 1) {
            "Meal does not exist or is not deleted: $mealId"
        }
    }

    suspend fun permanentlyDelete(mealId: String): Boolean =
        mealDao.permanentlyDelete(mealId) == 1

    suspend fun setCoverPhoto(mealId: String, photoId: String, updatedAt: Long) {
        check(mealDao.setCoverPhoto(mealId, photoId, updatedAt) == 1) {
            "Photo does not belong to meal: $photoId"
        }
    }

    companion object {
        const val MAX_PHOTOS_PER_MEAL = 9
    }
}

private fun String.escapeLikeQuery(): String = trim()
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")

private fun String.normalizedName(): String = trim().replace(Regex("\\s+"), " ")
