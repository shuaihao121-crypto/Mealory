package com.mealory.app.data.database.entity

data class CalendarMealPreview(
    val mealId: String,
    val dateEpochDay: Long,
    val minuteOfDay: Int,
    val mealType: MealType,
    val title: String?,
    val coverRelativePath: String?,
)
