package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.database.entity.PlaceSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: PlaceEntity)

    @Update
    suspend fun update(place: PlaceEntity): Int

    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    suspend fun getById(placeId: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    fun observeById(placeId: String): Flow<PlaceEntity?>

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query(
        """
        SELECT places.id AS id,
               places.name AS name,
               COUNT(meals.id) AS mealCount,
               MIN(meals.dateEpochDay) AS firstDateEpochDay,
               MAX(meals.dateEpochDay) AS lastDateEpochDay
        FROM places
        LEFT JOIN meals ON meals.placeId = places.id AND meals.deletedAt IS NULL
        GROUP BY places.id, places.name
        ORDER BY mealCount DESC, places.name COLLATE NOCASE ASC
        """,
    )
    fun observeSummaries(): Flow<List<PlaceSummary>>

    @Query(
        """
        SELECT places.* FROM places
        INNER JOIN meals ON meals.placeId = places.id
        WHERE meals.deletedAt IS NULL
        GROUP BY places.id
        ORDER BY MAX(meals.dateEpochDay) DESC,
                 MAX(meals.minuteOfDay) DESC,
                 MAX(meals.updatedAt) DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyUsed(limit: Int): Flow<List<PlaceEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE placeId = :placeId AND deletedAt IS NULL
        ORDER BY dateEpochDay DESC, minuteOfDay DESC, createdAt DESC
        """,
    )
    fun observeMeals(placeId: String): Flow<List<MealWithDetails>>

    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deleteById(placeId: String): Int
}
