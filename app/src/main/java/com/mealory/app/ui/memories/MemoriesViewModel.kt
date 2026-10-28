package com.mealory.app.ui.memories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.database.entity.MemoryMonthMealPreview
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JourneyStats(
    val dayNumber: Long,
    val mealCount: Long,
)

data class PastMonthGroup(
    val year: Int,
    val meals: List<MemoryMonthMealPreview>,
)

class MemoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val today: LocalDate = LocalDate.now()
    private val targetDate = today.minusYears(1)

    val journeyStats: StateFlow<JourneyStats> = combine(
        repository.observeFirstActiveMealDate(),
        repository.observeActiveMealCount(),
    ) { firstDate, count ->
        JourneyStats(
            dayNumber = firstDate?.let {
                (ChronoUnit.DAYS.between(LocalDate.ofEpochDay(it), today) + 1).coerceAtLeast(1)
            } ?: 0,
            mealCount = count,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JourneyStats(0, 0))

    val pastToday: StateFlow<MealWithDetails?> = repository.observeMemoryWindow(targetDate.toEpochDay())
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pastMonthGroups: StateFlow<List<PastMonthGroup>> = repository
        .observePastMonthPreviews(today.monthValue, today.year)
        .map { previews ->
            previews.groupBy(MemoryMonthMealPreview::year)
                .map { (year, meals) -> PastMonthGroup(year, meals) }
                .sortedByDescending(PastMonthGroup::year)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val annualYears: StateFlow<List<Int>> = repository.observeActiveYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val randomMeal = MutableStateFlow<MealWithDetails?>(null)

    init {
        pickRandomMeal()
    }

    fun pickRandomMeal() {
        viewModelScope.launch {
            randomMeal.value = repository.pickRandomMeal(
                beforeEpochDay = today.minusDays(30).toEpochDay(),
                excludingMealId = randomMeal.value?.meal?.id,
            )
        }
    }

    fun daysAgo(meal: MealWithDetails): Long =
        ChronoUnit.DAYS.between(LocalDate.ofEpochDay(meal.meal.dateEpochDay), today)
}
