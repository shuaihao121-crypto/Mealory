package com.mealory.app.data

import android.content.Context
import androidx.room.Room
import androidx.paging.PagingSource
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

    @Test
    fun pagingSearchesTitleNotePlaceAndPersonInTimelineOrder() = runTest {
        val person = person("person-search", "小王")
        val place = place("place-search", "一食堂")
        database.personDao().insert(person)
        database.placeDao().insert(place)
        repository.createMeal(
            meal(id = "older", placeId = place.id, coverPhotoId = null, title = "牛肉面", dateEpochDay = 20_385L),
            personIds = setOf(person.id),
        )
        repository.createMeal(
            meal(id = "newer", placeId = null, coverPhotoId = null, title = "火锅", dateEpochDay = 20_386L)
                .copy(note = "青岛旅行"),
        )

        suspend fun search(query: String): List<String> {
            val result = database.mealDao().pagingSource(query).load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 30, placeholdersEnabled = false),
            ) as PagingSource.LoadResult.Page
            return result.data.map { it.meal.id }
        }

        assertEquals(listOf("newer", "older"), search(""))
        assertEquals(listOf("older"), search("牛肉"))
        assertEquals(listOf("older"), search("一食堂"))
        assertEquals(listOf("older"), search("小王"))
        assertEquals(listOf("newer"), search("青岛"))

        repository.softDelete("newer", 9_000L)
        assertTrue(search("青岛").isEmpty())
    }

    @Test
    fun coverCanOnlyReferencePhotoFromSameMeal() = runTest {
        val meal = meal(placeId = null, coverPhotoId = "photo-1")
        repository.createMeal(
            meal,
            photos = listOf(
                photo(meal.id, "photo-1"),
                photo(meal.id, "photo-2").copy(sortOrder = 1),
            ),
        )
        val otherMeal = meal(id = "meal-2", placeId = null, coverPhotoId = "other-photo")
        repository.createMeal(otherMeal, photos = listOf(photo(otherMeal.id, "other-photo")))

        repository.setCoverPhoto(meal.id, "photo-2", 5_000L)
        assertEquals("photo-2", repository.getMeal(meal.id)!!.meal.coverPhotoId)

        assertTrue(runCatching { repository.setCoverPhoto(meal.id, "missing", 6_000L) }.isFailure)
        assertTrue(runCatching { repository.setCoverPhoto(meal.id, "other-photo", 7_000L) }.isFailure)
        assertEquals("photo-2", repository.getMeal(meal.id)!!.meal.coverPhotoId)
    }

    @Test
    fun calendarPreviewsUseCoverAndDayQueryFiltersDeletedMeals() = runTest {
        val breakfast = meal(
            id = "breakfast",
            placeId = null,
            coverPhotoId = null,
            title = "早餐",
            dateEpochDay = 20_400L,
        ).copy(minuteOfDay = 8 * 60)
        val dinner = meal(
            id = "dinner",
            placeId = null,
            coverPhotoId = "dinner-cover",
            title = "晚餐",
            dateEpochDay = 20_400L,
        ).copy(minuteOfDay = 19 * 60)
        val deleted = meal(
            id = "deleted-calendar",
            placeId = null,
            coverPhotoId = null,
            title = "已删除",
            dateEpochDay = 20_401L,
        )

        repository.createMeal(breakfast)
        repository.createMeal(
            dinner,
            photos = listOf(
                photo(dinner.id, "dinner-first"),
                photo(dinner.id, "dinner-cover").copy(sortOrder = 1),
            ),
        )
        repository.createMeal(deleted)
        repository.softDelete(deleted.id, 8_000L)

        val previews = repository.observeCalendarPreviews(20_400L, 20_402L).first()
        assertEquals(listOf("breakfast", "dinner"), previews.map { it.mealId })
        assertNull(previews.first().coverRelativePath)
        assertTrue(previews.last().coverRelativePath!!.endsWith("dinner-cover.jpg"))

        val dayMeals = repository.observeMealsForDay(20_400L).first()
        assertEquals(listOf("breakfast", "dinner"), dayMeals.map { it.meal.id })
        assertTrue(repository.observeMealsForDay(20_401L).first().isEmpty())
    }

    private fun meal(
        id: String = "meal-1",
        placeId: String?,
        coverPhotoId: String?,
        title: String = "牛肉面",
        dateEpochDay: Long = 20_386L,
    ) = MealEntity(
        id = id,
        dateEpochDay = dateEpochDay,
        minuteOfDay = 18 * 60 + 42,
        mealType = MealType.DINNER,
        title = title,
        note = null,
        placeId = placeId,
        coverPhotoId = coverPhotoId,
        timeZoneId = "Asia/Shanghai",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private fun photo(mealId: String, id: String = "photo-1") = MealPhotoEntity(
        id = id,
        mealId = mealId,
        relativePath = "pictures/meals/2026/10/$mealId/$id.jpg",
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
