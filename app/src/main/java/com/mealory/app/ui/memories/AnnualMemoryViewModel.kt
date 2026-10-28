package com.mealory.app.ui.memories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AnnualSeason(val label: String) {
    SPRING("春天"),
    SUMMER("夏天"),
    AUTUMN("秋天"),
    WINTER("冬天"),
}

data class AnnualTagHighlight(
    val names: List<String>,
    val mealCount: Int,
)

data class AnnualMilestone(
    val position: Long,
    val meal: MealWithDetails,
)

data class AnnualSeasonPhoto(
    val mealId: String,
    val relativePath: String,
)

data class AnnualMemoryState(
    val year: Int,
    val mealCount: Int = 0,
    val recordedDayCount: Int = 0,
    val firstMeal: MealWithDetails? = null,
    val lastMeal: MealWithDetails? = null,
    val milestones: List<AnnualMilestone> = emptyList(),
    val topPeople: AnnualTagHighlight? = null,
    val topPlace: AnnualTagHighlight? = null,
    val seasonalPhotos: Map<AnnualSeason, List<AnnualSeasonPhoto>> = emptyMap(),
)

class AnnualMemoryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val year: Int = checkNotNull(savedStateHandle["year"])
    private val yearStart = LocalDate.of(year, 1, 1).toEpochDay()
    private val nextYearStart = LocalDate.of(year + 1, 1, 1).toEpochDay()

    val state: StateFlow<AnnualMemoryState> = combine(
        repository.observeAnnualMeals(yearStart, nextYearStart),
        repository.observeActiveCountBefore(yearStart),
    ) { meals, activeBeforeYear ->
        buildAnnualMemory(year, meals, activeBeforeYear)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AnnualMemoryState(year),
    )
}

internal fun buildAnnualMemory(
    year: Int,
    meals: List<MealWithDetails>,
    activeBeforeYear: Long,
): AnnualMemoryState {
    val orderedMeals = meals.sortedWith(
        compareBy<MealWithDetails> { it.meal.dateEpochDay }
            .thenBy { it.meal.minuteOfDay }
            .thenBy { it.meal.createdAt },
    )
    val topPeople = orderedMeals
        .flatMap { it.people }
        .groupingBy { it.id to it.name }
        .eachCount()
        .toHighlight()
    val topPlace = orderedMeals
        .mapNotNull { it.place }
        .groupingBy { it.id to it.name }
        .eachCount()
        .toHighlight()
    val seasonalPhotos = orderedMeals
        .mapNotNull { meal ->
            val photo = meal.photos.firstOrNull { it.id == meal.meal.coverPhotoId } ?: meal.photos.firstOrNull()
            photo?.let {
                seasonFor(LocalDate.ofEpochDay(meal.meal.dateEpochDay).monthValue) to
                    AnnualSeasonPhoto(meal.meal.id, it.relativePath)
            }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, photos) -> photos.distinctBy(AnnualSeasonPhoto::relativePath).take(4) }

    return AnnualMemoryState(
        year = year,
        mealCount = orderedMeals.size,
        recordedDayCount = orderedMeals.distinctBy { it.meal.dateEpochDay }.size,
        firstMeal = orderedMeals.firstOrNull(),
        lastMeal = orderedMeals.lastOrNull(),
        milestones = orderedMeals.mapIndexedNotNull { index, meal ->
            val position = activeBeforeYear + index + 1
            position.takeIf(::isMeaningfulMilestone)?.let { AnnualMilestone(it, meal) }
        },
        topPeople = topPeople,
        topPlace = topPlace,
        seasonalPhotos = seasonalPhotos,
    )
}

private fun Map<Pair<String, String>, Int>.toHighlight(): AnnualTagHighlight? {
    val highestCount = values.maxOrNull()?.takeIf { it >= 2 } ?: return null
    return AnnualTagHighlight(
        names = filterValues { it == highestCount }.keys.map { it.second }.sorted(),
        mealCount = highestCount,
    )
}

private fun isMeaningfulMilestone(position: Long): Boolean =
    position == 100L || position >= 500L && position % 500L == 0L

private fun seasonFor(month: Int): AnnualSeason = when (month) {
    in 3..5 -> AnnualSeason.SPRING
    in 6..8 -> AnnualSeason.SUMMER
    in 9..11 -> AnnualSeason.AUTUMN
    else -> AnnualSeason.WINTER
}
