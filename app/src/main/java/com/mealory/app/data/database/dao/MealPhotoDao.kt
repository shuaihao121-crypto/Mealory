package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.PhotoStorageStats
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPhotoDao {
    @Query("SELECT COUNT(*) AS photoCount, COALESCE(SUM(fileSize), 0) AS totalBytes FROM meal_photos")
    fun observeStorageStats(): Flow<PhotoStorageStats>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(photos: List<MealPhotoEntity>)

    @Update
    suspend fun update(photo: MealPhotoEntity): Int

    @Delete
    suspend fun delete(photo: MealPhotoEntity): Int

    @Query("SELECT * FROM meal_photos WHERE mealId = :mealId ORDER BY sortOrder, createdAt")
    suspend fun getForMeal(mealId: String): List<MealPhotoEntity>
}
