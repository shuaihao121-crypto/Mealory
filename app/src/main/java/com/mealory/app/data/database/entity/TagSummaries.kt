package com.mealory.app.data.database.entity

data class PersonSummary(
    val id: String,
    val name: String,
    val mealCount: Long,
    val firstDateEpochDay: Long?,
    val lastDateEpochDay: Long?,
)

data class PlaceSummary(
    val id: String,
    val name: String,
    val mealCount: Long,
    val firstDateEpochDay: Long?,
    val lastDateEpochDay: Long?,
)
