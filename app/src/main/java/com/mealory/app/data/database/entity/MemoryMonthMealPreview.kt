package com.mealory.app.data.database.entity

data class MemoryMonthMealPreview(
    val year: Int,
    val mealId: String,
    val dateEpochDay: Long,
    val coverRelativePath: String?,
)
