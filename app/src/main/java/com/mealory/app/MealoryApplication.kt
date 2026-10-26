package com.mealory.app

import android.app.Application

class MealoryApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
