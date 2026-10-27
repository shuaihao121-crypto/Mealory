package com.mealory.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mealory.app.MealoryApplication
import com.mealory.app.data.database.entity.MealWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealoryApplication).container.mealRepository
    val searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val meals: Flow<PagingData<MealWithDetails>> = searchQuery
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest(repository::pagedActiveMeals)
        .cachedIn(viewModelScope)
}
