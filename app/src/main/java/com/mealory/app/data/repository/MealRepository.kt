package com.mealory.app.data.repository

import androidx.room.withTransaction
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealWithDetails
import kotlinx.coroutines.flow.Flow

class MealRepository(
    private val database: MealoryDatabase,
) {
    private val mealDao = database.mealDao()
    private val photoDao = database.mealPhotoDao()

    fun observeActiveMeals(): Flow<List<MealWithDetails>> = mealDao.observeActiveMeals()

    fun observeDeletedMeals(): Flow<List<MealWithDetails>> = mealDao.observeDeletedMeals()

    suspend fun getMeal(mealId: String): MealWithDetails? = mealDao.getById(mealId)

    suspend fun createMeal(
        meal: MealEntity,
        photos: List<MealPhotoEntity> = emptyList(),
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

    companion object {
        const val MAX_PHOTOS_PER_MEAL = 9
    }
}
