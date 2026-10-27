package com.mealory.app.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.CalendarMealPreview
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class CalendarMode {
    MONTH,
    YEAR,
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository

    val selectedMonth = MutableStateFlow(YearMonth.now())
    val mode = MutableStateFlow(CalendarMode.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val previews: StateFlow<List<CalendarMealPreview>> = combine(selectedMonth, mode) { month, currentMode ->
        val start = if (currentMode == CalendarMode.MONTH) month.atDay(1) else month.withMonth(1).atDay(1)
        val end = if (currentMode == CalendarMode.MONTH) start.plusMonths(1) else start.plusYears(1)
        start.toEpochDay() to end.toEpochDay()
    }.flatMapLatest { (start, end) -> repository.observeCalendarPreviews(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousPeriod() {
        selectedMonth.value = when (mode.value) {
            CalendarMode.MONTH -> selectedMonth.value.minusMonths(1)
            CalendarMode.YEAR -> selectedMonth.value.minusYears(1)
        }
    }

    fun nextPeriod() {
        selectedMonth.value = when (mode.value) {
            CalendarMode.MONTH -> selectedMonth.value.plusMonths(1)
            CalendarMode.YEAR -> selectedMonth.value.plusYears(1)
        }
    }

    fun showMonth(month: Int) {
        selectedMonth.value = selectedMonth.value.withMonth(month)
        mode.value = CalendarMode.MONTH
    }
}
