package com.mealory.app

import android.content.Context
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.preferences.SettingsRepository
import com.mealory.app.data.repository.MealRepository

class AppContainer(context: Context) {
    val database: MealoryDatabase = MealoryDatabase.build(context)
    val mealRepository: MealRepository = MealRepository(database)
    val settingsRepository: SettingsRepository = SettingsRepository.create(context)
}
