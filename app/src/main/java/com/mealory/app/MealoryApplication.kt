package com.mealory.app

import android.app.Application
import com.mealory.app.work.DeletedMealsCleanupWorker

class MealoryApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        DeletedMealsCleanupWorker.schedule(this)
    }
}
