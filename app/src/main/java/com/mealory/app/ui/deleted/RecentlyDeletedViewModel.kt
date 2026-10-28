package com.mealory.app.ui.deleted

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.repository.DeletedMealManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentlyDeletedViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MealoryApplication).container

    val meals: StateFlow<List<MealWithDetails>> = container.mealRepository.observeDeletedMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(mealId: String) {
        viewModelScope.launch { container.deletedMealManager.restore(mealId) }
    }

    fun permanentlyDelete(mealId: String) {
        viewModelScope.launch { container.deletedMealManager.permanentlyDelete(mealId) }
    }
}

internal fun remainingRetentionDays(deletedAt: Long, now: Long): Long {
    val remaining = deletedAt + DeletedMealManager.RETENTION_MILLIS - now
    if (remaining <= 0) return 0
    val day = TimeUnit.DAYS.toMillis(1)
    return ((remaining + day - 1) / day).coerceAtMost(30)
}
