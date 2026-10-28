package com.mealory.app.ui.memories

import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnnualMemoryViewModelTest {
    @Test
    fun annualMemoryUsesRealMealsForStatsHighlightsMilestonesAndSeasons() {
        val mother = person("mother", "妈妈")
        val friend = person("friend", "小王")
        val home = place("home", "家")
        val meals = listOf(
            meal("first", LocalDate.of(2025, 1, 2), people = listOf(mother), place = home, withPhoto = true),
            meal("spring-1", LocalDate.of(2025, 3, 8), people = listOf(mother, friend), place = home, withPhoto = true),
            meal("spring-2", LocalDate.of(2025, 3, 9), people = listOf(friend), place = home, withPhoto = true),
            meal("last", LocalDate.of(2025, 12, 30), withPhoto = true),
        )

        val state = buildAnnualMemory(year = 2025, meals = meals.reversed(), activeBeforeYear = 99)

        assertEquals(4, state.mealCount)
        assertEquals(4, state.recordedDayCount)
        assertEquals("first", state.firstMeal!!.meal.id)
        assertEquals("last", state.lastMeal!!.meal.id)
        assertEquals(listOf(100L), state.milestones.map(AnnualMilestone::position))
        assertEquals(listOf("妈妈", "小王"), state.topPeople!!.names)
        assertEquals(2, state.topPeople!!.mealCount)
        assertEquals(listOf("家"), state.topPlace!!.names)
        assertEquals(3, state.topPlace!!.mealCount)
        assertEquals(2, state.seasonalPhotos[AnnualSeason.WINTER]!!.size)
        assertEquals(2, state.seasonalPhotos[AnnualSeason.SPRING]!!.size)
    }

    @Test
    fun singleOccurrencesDoNotCreateMostFrequentClaims() {
        val state = buildAnnualMemory(
            year = 2025,
            meals = listOf(
                meal(
                    id = "only",
                    date = LocalDate.of(2025, 7, 1),
                    people = listOf(person("guest", "访客")),
                    place = place("cafe", "咖啡馆"),
                ),
            ),
            activeBeforeYear = 0,
        )

        assertNull(state.topPeople)
        assertNull(state.topPlace)
        assertEquals(1, state.recordedDayCount)
        assertEquals(state.firstMeal, state.lastMeal)
    }

    private fun meal(
        id: String,
        date: LocalDate,
        people: List<PersonEntity> = emptyList(),
        place: PlaceEntity? = null,
        withPhoto: Boolean = false,
    ): MealWithDetails {
        val photo = MealPhotoEntity(
            id = "$id-photo",
            mealId = id,
            relativePath = "pictures/$id.jpg",
            sortOrder = 0,
            width = 100,
            height = 100,
            fileSize = 100,
            mimeType = "image/jpeg",
            originalTakenAt = null,
            createdAt = 1,
        )
        return MealWithDetails(
            meal = MealEntity(
                id = id,
                dateEpochDay = date.toEpochDay(),
                minuteOfDay = 12 * 60,
                mealType = MealType.LUNCH,
                title = id,
                note = null,
                placeId = place?.id,
                coverPhotoId = photo.id.takeIf { withPhoto },
                timeZoneId = "Asia/Shanghai",
                createdAt = 1,
                updatedAt = 1,
                deletedAt = null,
            ),
            photos = if (withPhoto) listOf(photo) else emptyList(),
            place = place,
            people = people,
        )
    }

    private fun person(id: String, name: String) = PersonEntity(id, name, 1, 1)

    private fun place(id: String, name: String) = PlaceEntity(id, name, 1, 1)
}
