package com.mealory.app.data.database

import androidx.room.TypeConverter
import com.mealory.app.data.database.entity.MealType

class MealoryConverters {
    @TypeConverter
    fun mealTypeToString(value: MealType): String = value.name

    @TypeConverter
    fun stringToMealType(value: String): MealType = MealType.valueOf(value)
}
