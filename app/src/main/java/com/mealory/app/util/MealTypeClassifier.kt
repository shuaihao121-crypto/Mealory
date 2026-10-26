package com.mealory.app.util

import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.preferences.MealTimeRanges

object MealTypeClassifier {
    fun classify(
        minuteOfDay: Int,
        ranges: MealTimeRanges = MealTimeRanges.Default,
    ): MealType {
        require(minuteOfDay in 0..1_439)
        return when (minuteOfDay) {
            in ranges.breakfast -> MealType.BREAKFAST
            in ranges.lunch -> MealType.LUNCH
            in ranges.dinner -> MealType.DINNER
            in ranges.lateNight -> MealType.LATE_NIGHT
            else -> MealType.OTHER
        }
    }
}
