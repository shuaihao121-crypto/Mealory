package com.mealory.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import com.mealory.app.data.repository.MealRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealoryDatabaseTest {
    private lateinit var database: MealoryDatabase
    private lateinit var repository: MealRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MealoryDatabase::class.java).build()
        repository = MealRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createUpdateSoftDeleteAndRestoreMeal() = runTest {
        val person = person("person-1", "小王")
        val place = place("place-1", "一食堂")
        database.personDao().insert(person)
        database.placeDao().insert(place)

        val meal = meal(placeId = place.id, coverPhotoId = "photo-1")
        val photo = photo(mealId = meal.id)
        repository.createMeal(meal, listOf(photo), personIds = setOf(person.id))

        val created = repository.getMeal(meal.id)!!
        assertEquals("牛肉面", created.meal.title)
        assertEquals(listOf(photo.id), created.photos.map { it.id })
        assertEquals(place.id, created.place?.id)
        assertEquals(setOf(person.id), created.people.map { it.id }.toSet())

        repository.updateMeal(
            meal.copy(title = "红烧牛肉面", note = "少辣", updatedAt = 2_000L),
            personIds = emptySet(),
        )
        val updated = repository.getMeal(meal.id)!!
        assertEquals("红烧牛肉面", updated.meal.title)
        assertEquals("少辣", updated.meal.note)
        assertTrue(updated.people.isEmpty())

        repository.softDelete(meal.id, deletedAt = 3_000L)
        assertTrue(repository.observeActiveMeals().first().isEmpty())
        assertEquals(meal.id, repository.observeDeletedMeals().first().single().meal.id)

        repository.restore(meal.id, restoredAt = 4_000L)
        assertNull(repository.getMeal(meal.id)!!.meal.deletedAt)
        assertEquals(meal.id, repository.observeActiveMeals().first().single().meal.id)
    }

    @Test
    fun deletingPersonOrPlacePreservesMealAndPermanentDeleteCascadesRows() = runTest {
        val person = person("person-1", "妈妈")
        val place = place("place-1", "家")
        database.personDao().insert(person)
        database.placeDao().insert(place)

        val meal = meal(placeId = place.id, coverPhotoId = "photo-1")
        repository.createMeal(meal, listOf(photo(meal.id)), personIds = setOf(person.id))

        assertEquals(1, database.personDao().deleteById(person.id))
        assertTrue(repository.getMeal(meal.id)!!.people.isEmpty())

        assertEquals(1, database.placeDao().deleteById(place.id))
        val afterPlaceDelete = repository.getMeal(meal.id)!!
        assertNull(afterPlaceDelete.meal.placeId)
        assertNull(afterPlaceDelete.place)

        assertTrue(repository.permanentlyDelete(meal.id))
        assertNull(repository.getMeal(meal.id))
        assertTrue(database.mealPhotoDao().getForMeal(meal.id).isEmpty())
        assertFalse(repository.permanentlyDelete(meal.id))
    }

    private fun meal(
        placeId: String?,
        coverPhotoId: String?,
    ) = MealEntity(
        id = "meal-1",
        dateEpochDay = 20_386L,
        minuteOfDay = 18 * 60 + 42,
        mealType = MealType.DINNER,
        title = "牛肉面",
        note = null,
        placeId = placeId,
        coverPhotoId = coverPhotoId,
        timeZoneId = "Asia/Shanghai",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private fun photo(mealId: String) = MealPhotoEntity(
        id = "photo-1",
        mealId = mealId,
        relativePath = "Pictures/meals/2026/10/$mealId/photo-1.jpg",
        sortOrder = 0,
        width = 2_560,
        height = 1_920,
        fileSize = 1_024L,
        mimeType = "image/jpeg",
        originalTakenAt = null,
        createdAt = 1_000L,
    )

    private fun person(id: String, name: String) = PersonEntity(
        id = id,
        name = name,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun place(id: String, name: String) = PlaceEntity(
        id = id,
        name = name,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )
}
