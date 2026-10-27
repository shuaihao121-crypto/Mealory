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
import com.mealory.app.data.database.entity.PlaceEntity
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

    suspend fun getMeal(mealId: String): MealWithDetails? = mealDao.getById(mealId)

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
