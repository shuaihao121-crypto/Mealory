package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mealory.app.data.database.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity)

    @Update
    suspend fun update(person: PersonEntity): Int

    @Query("SELECT * FROM people WHERE id = :personId LIMIT 1")
    suspend fun getById(personId: String): PersonEntity?

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deleteById(personId: String): Int
}
