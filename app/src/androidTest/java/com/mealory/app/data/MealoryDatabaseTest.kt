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
import com.mealory.app.data.repository.DeletedMealManager
import com.mealory.app.storage.PhotoStorage
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class MealoryDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: MealoryDatabase
    private lateinit var repository: MealRepository
    private lateinit var deletedMealManager: DeletedMealManager

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MealoryDatabase::class.java).build()
        repository = MealRepository(database)
        deletedMealManager = DeletedMealManager(repository, PhotoStorage(context))
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

    @Test
    fun peopleAndPlacesAreReusedAndSummariesTrackActiveMeals() = runTest {
        val firstMeal = meal(
            id = "tag-meal-1",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = 20_410L,
        )
        val secondMeal = meal(
            id = "tag-meal-2",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = 20_411L,
        )
        repository.createMeal(firstMeal)
        repository.createMeal(secondMeal)
        repository.updateMealWithTags(firstMeal, "家", listOf("妈妈", "爸爸"))
        repository.updateMealWithTags(secondMeal, " 家 ", listOf("妈妈", " 妈妈 "))

        val people = repository.observePeople().first()
        val places = repository.observePlaces().first()
        assertEquals(setOf("妈妈", "爸爸"), people.map { it.name }.toSet())
        assertEquals(listOf("家"), places.map { it.name })

        val personSummaries = repository.observePersonSummaries().first()
        assertEquals("妈妈", personSummaries.first().name)
        assertEquals(2L, personSummaries.first().mealCount)
        assertEquals(20_410L, personSummaries.first().firstDateEpochDay)
        assertEquals(20_411L, personSummaries.first().lastDateEpochDay)
        assertEquals(2L, repository.observePlaceSummaries().first().single().mealCount)
        assertEquals("tag-meal-2", repository.observeMealsForPerson(personSummaries.first().id).first().first().meal.id)
        assertEquals("家", repository.observeRecentlyUsedPlaces().first().single().name)

        repository.renamePerson(personSummaries.first().id, "母亲")
        repository.renamePlace(places.single().id, "家里")
        assertEquals("母亲", repository.observePerson(personSummaries.first().id).first()!!.name)
        assertEquals("家里", repository.observePlace(places.single().id).first()!!.name)

        repository.softDelete(secondMeal.id, 9_000L)
        assertEquals(1L, repository.observePersonSummaries().first().first { it.id == personSummaries.first().id }.mealCount)
        assertEquals(1L, repository.observePlaceSummaries().first().single().mealCount)
    }

    @Test
    fun memoriesPreferExactDateAndRandomMealPrefersOlderHistory() = runTest {
        val target = LocalDate.of(2025, 10, 27).toEpochDay()
        val exact = meal(
            id = "memory-exact",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = target,
        )
        val nearby = meal(
            id = "memory-nearby",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = target - 2,
        )
        val recent = meal(
            id = "memory-recent",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
        )
        repository.createMeal(nearby)
        repository.createMeal(exact)
        repository.createMeal(recent)

        val window = repository.observeMemoryWindow(target).first()
        assertEquals("memory-exact", window.first().meal.id)
        assertEquals(3L, repository.observeActiveMealCount().first())
        assertEquals(target - 2, repository.observeFirstActiveMealDate().first())

        val cutoff = LocalDate.of(2026, 9, 27).toEpochDay()
        val firstRandom = repository.pickRandomMeal(cutoff, random = Random(7))!!
        assertTrue(firstRandom.meal.id in setOf("memory-exact", "memory-nearby"))
        val secondRandom = repository.pickRandomMeal(
            cutoff,
            excludingMealId = firstRandom.meal.id,
            random = Random(7),
        )!!
        assertNotEquals(firstRandom.meal.id, secondRandom.meal.id)

        val monthGroups = repository.observePastMonthPreviews(month = 10, currentYear = 2026).first()
        assertEquals(setOf("memory-exact", "memory-nearby"), monthGroups.map { it.mealId }.toSet())
        assertEquals(setOf(2025), monthGroups.map { it.year }.toSet())

        repository.softDelete(exact.id, 10_000L)
        assertEquals("memory-nearby", repository.observeMemoryWindow(target).first().single().meal.id)
    }

    @Test
    fun annualQueriesReturnOnlyActiveMealsAndCorrectYearBoundaries() = runTest {
        val last2024 = meal(
            id = "annual-2024",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = LocalDate.of(2024, 12, 31).toEpochDay(),
        )
        val first2025 = meal(
            id = "annual-2025-first",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = LocalDate.of(2025, 1, 1).toEpochDay(),
        )
        val last2025 = meal(
            id = "annual-2025-last",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = LocalDate.of(2025, 12, 31).toEpochDay(),
        )
        val deleted2026 = meal(
            id = "annual-deleted",
            placeId = null,
            coverPhotoId = null,
            dateEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
        )
        repository.createMeal(last2024)
        repository.createMeal(last2025)
        repository.createMeal(first2025)
        repository.createMeal(deleted2026)
        repository.softDelete(deleted2026.id, 10_000L)

        val start = LocalDate.of(2025, 1, 1).toEpochDay()
        val end = LocalDate.of(2026, 1, 1).toEpochDay()
        val annualMeals = repository.observeAnnualMeals(start, end).first()

        assertEquals(listOf("annual-2025-first", "annual-2025-last"), annualMeals.map { it.meal.id })
        assertEquals(1L, repository.observeActiveCountBefore(start).first())
        assertEquals(listOf(2025, 2024), repository.observeActiveYears().first())
    }

    @Test
    fun recentlyDeletedRestoresOrPermanentlyDeletesDatabaseAndPhotoFiles() = runTest {
        val now = System.currentTimeMillis()
        val photoMeal = meal(
            id = "deleted-with-photo",
            placeId = null,
            coverPhotoId = "deleted-photo",
        )
        val relativePath = "pictures/meals/2026/10/${photoMeal.id}/deleted-photo.jpg"
        val photoFile = File(context.filesDir, relativePath).apply {
            parentFile!!.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        repository.createMeal(
            photoMeal,
            photos = listOf(photo(photoMeal.id, "deleted-photo").copy(relativePath = relativePath)),
        )
        repository.softDelete(photoMeal.id, now)

        deletedMealManager.restore(photoMeal.id, now + 1)
        assertNull(repository.getMeal(photoMeal.id)!!.meal.deletedAt)
        repository.softDelete(photoMeal.id, now + 2)
        assertTrue(deletedMealManager.permanentlyDelete(photoMeal.id))
        assertNull(repository.getMeal(photoMeal.id))
        assertFalse(photoFile.exists())

        val expired = meal(id = "expired-deleted", placeId = null, coverPhotoId = null)
        val recent = meal(id = "recent-deleted", placeId = null, coverPhotoId = null)
        repository.createMeal(expired)
        repository.createMeal(recent)
        repository.softDelete(expired.id, now - DeletedMealManager.RETENTION_MILLIS - 1)
        repository.softDelete(recent.id, now - DeletedMealManager.RETENTION_MILLIS + 1)

        val cleanup = deletedMealManager.cleanupExpired(now)
        assertEquals(1, cleanup.deletedCount)
        assertEquals(0, cleanup.failedCount)
        assertNull(repository.getMeal(expired.id))
        assertTrue(repository.getMeal(recent.id) != null)
    }

    @Test
    fun pagingLoadsOnlyOnePageFromLargeMealHistory() = runTest {
        val meals = (0 until 1_200).map { index ->
            meal(
                id = "bulk-$index",
                dateEpochDay = 18_000L + index,
                minuteOfDay = index % 1_440,
                createdAt = index.toLong(),
            )
        }
        database.backupDao().insertMeals(meals)

        val result = database.mealDao().pagingSource("").load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val page = result as PagingSource.LoadResult.Page
        assertEquals(30, page.data.size)
        assertEquals("bulk-1199", page.data.first().meal.id)
        assertTrue(page.nextKey != null)
    }

    private fun meal(
        id: String = "meal-1",
        placeId: String? = null,
        coverPhotoId: String? = null,
        title: String = "牛肉面",
        dateEpochDay: Long = 20_386L,
        minuteOfDay: Int = 18 * 60 + 42,
        createdAt: Long = 1_000L,
    ) = MealEntity(
        id = id,
        dateEpochDay = dateEpochDay,
        minuteOfDay = minuteOfDay,
        mealType = MealType.DINNER,
        title = title,
        note = null,
        placeId = placeId,
        coverPhotoId = coverPhotoId,
        timeZoneId = "Asia/Shanghai",
        createdAt = createdAt,
        updatedAt = createdAt,
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
