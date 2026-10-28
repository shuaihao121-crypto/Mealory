package com.mealory.app

import android.content.Context
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.preferences.SettingsRepository
import com.mealory.app.data.repository.MealRecorder
import com.mealory.app.data.repository.DeletedMealManager
import com.mealory.app.data.repository.MealRepository
import com.mealory.app.storage.ExifReader
import com.mealory.app.storage.PhotoProcessor
import com.mealory.app.storage.PhotoStorage

class AppContainer(context: Context) {
    val database: MealoryDatabase = MealoryDatabase.build(context)
    val mealRepository: MealRepository = MealRepository(database)
    val settingsRepository: SettingsRepository = SettingsRepository.create(context)
    val photoStorage = PhotoStorage(context.applicationContext)
    val deletedMealManager = DeletedMealManager(mealRepository, photoStorage)
    val exifReader = ExifReader(context.contentResolver)
    val photoProcessor = PhotoProcessor(context.contentResolver, photoStorage, exifReader)
    val mealRecorder = MealRecorder(mealRepository, photoProcessor, photoStorage)
}
