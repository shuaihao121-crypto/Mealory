package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mealory.app.data.database.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: PlaceEntity)

    @Update
    suspend fun update(place: PlaceEntity): Int

    @Query("SELECT * FROM places WHERE id = :placeId LIMIT 1")
    suspend fun getById(placeId: String): PlaceEntity?

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deleteById(placeId: String): Int
}
