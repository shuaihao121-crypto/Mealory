package com.mealory.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PersonSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity)

    @Update
    suspend fun update(person: PersonEntity): Int

    @Query("SELECT * FROM people WHERE id = :personId LIMIT 1")
    suspend fun getById(personId: String): PersonEntity?

    @Query("SELECT * FROM people WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :personId LIMIT 1")
    fun observeById(personId: String): Flow<PersonEntity?>

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query(
        """
        SELECT people.id AS id,
               people.name AS name,
               COUNT(meals.id) AS mealCount,
               MIN(meals.dateEpochDay) AS firstDateEpochDay,
               MAX(meals.dateEpochDay) AS lastDateEpochDay
        FROM people
        LEFT JOIN meal_people ON meal_people.personId = people.id
        LEFT JOIN meals ON meals.id = meal_people.mealId AND meals.deletedAt IS NULL
        GROUP BY people.id, people.name
        ORDER BY mealCount DESC, people.name COLLATE NOCASE ASC
        """,
    )
    fun observeSummaries(): Flow<List<PersonSummary>>

    @Transaction
    @Query(
        """
        SELECT meals.* FROM meals
        INNER JOIN meal_people ON meal_people.mealId = meals.id
        WHERE meal_people.personId = :personId AND meals.deletedAt IS NULL
        ORDER BY meals.dateEpochDay DESC, meals.minuteOfDay DESC, meals.createdAt DESC
        """,
    )
    fun observeMeals(personId: String): Flow<List<MealWithDetails>>

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deleteById(personId: String): Int
}
