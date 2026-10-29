package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPersonCrossRef
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM meals ORDER BY dateEpochDay, minuteOfDay, createdAt, id")
    suspend fun getMeals(): List<MealEntity>

    @Query("SELECT * FROM meal_photos ORDER BY mealId, sortOrder, createdAt, id")
    suspend fun getPhotos(): List<MealPhotoEntity>

    @Query("SELECT * FROM people ORDER BY id")
    suspend fun getPeople(): List<PersonEntity>

    @Query("SELECT * FROM places ORDER BY id")
    suspend fun getPlaces(): List<PlaceEntity>

    @Query("SELECT * FROM meal_people ORDER BY mealId, personId")
    suspend fun getMealPeople(): List<MealPersonCrossRef>

    @Query("DELETE FROM meal_people")
    suspend fun clearMealPeople()

    @Query("DELETE FROM meal_photos")
    suspend fun clearPhotos()

    @Query("DELETE FROM meals")
    suspend fun clearMeals()

    @Query("DELETE FROM people")
    suspend fun clearPeople()

    @Query("DELETE FROM places")
    suspend fun clearPlaces()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeals(values: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhotos(values: List<MealPhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPeople(values: List<PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaces(values: List<PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMealPeople(values: List<MealPersonCrossRef>)
}
