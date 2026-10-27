package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.paging.PagingSource
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(meal: MealEntity)

    @Update
    suspend fun update(meal: MealEntity): Int

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getById(mealId: String): MealWithDetails?

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    fun observeById(mealId: String): Flow<MealWithDetails?>

    @Transaction
    @Query(
        """
        SELECT DISTINCT meals.* FROM meals
        LEFT JOIN places ON places.id = meals.placeId
        LEFT JOIN meal_people ON meal_people.mealId = meals.id
        LEFT JOIN people ON people.id = meal_people.personId
        WHERE meals.deletedAt IS NULL
          AND (
            :query = '' OR
            meals.title LIKE '%' || :query || '%' ESCAPE '\' OR
            meals.note LIKE '%' || :query || '%' ESCAPE '\' OR
            places.name LIKE '%' || :query || '%' ESCAPE '\' OR
            people.name LIKE '%' || :query || '%' ESCAPE '\'
          )
        ORDER BY meals.dateEpochDay DESC, meals.minuteOfDay DESC, meals.createdAt DESC
        """,
    )
    fun pagingSource(query: String): PagingSource<Int, MealWithDetails>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE deletedAt IS NULL
        ORDER BY dateEpochDay DESC, minuteOfDay DESC, createdAt DESC
        """,
    )
    fun observeActiveMeals(): Flow<List<MealWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        """,
    )
    fun observeDeletedMeals(): Flow<List<MealWithDetails>>

    @Query(
        """
        UPDATE meals
        SET deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE id = :mealId AND deletedAt IS NULL
        """,
    )
    suspend fun softDelete(mealId: String, deletedAt: Long, updatedAt: Long): Int

    @Query(
        """
        UPDATE meals
        SET deletedAt = NULL, updatedAt = :updatedAt
        WHERE id = :mealId AND deletedAt IS NOT NULL
        """,
    )
    suspend fun restore(mealId: String, updatedAt: Long): Int

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun permanentlyDelete(mealId: String): Int

    @Query(
        """
        UPDATE meals
        SET coverPhotoId = :photoId, updatedAt = :updatedAt
        WHERE id = :mealId
          AND EXISTS (
            SELECT 1 FROM meal_photos
            WHERE meal_photos.id = :photoId AND meal_photos.mealId = :mealId
          )
        """,
    )
    suspend fun setCoverPhoto(mealId: String, photoId: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeople(crossRefs: List<MealPersonCrossRef>)

    @Query("DELETE FROM meal_people WHERE mealId = :mealId")
    suspend fun deletePeopleForMeal(mealId: String)
}
