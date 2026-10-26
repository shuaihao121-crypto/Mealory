package com.mealory.app

import com.mealory.app.data.database.entity.MealType
import com.mealory.app.util.MealTypeClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class MealTypeClassifierTest {
    @Test
    fun defaultBoundariesMatchProductRules() {
        assertEquals(MealType.LATE_NIGHT, MealTypeClassifier.classify(4 * 60 + 59))
        assertEquals(MealType.BREAKFAST, MealTypeClassifier.classify(5 * 60))
        assertEquals(MealType.BREAKFAST, MealTypeClassifier.classify(10 * 60 + 29))
        assertEquals(MealType.LUNCH, MealTypeClassifier.classify(10 * 60 + 30))
        assertEquals(MealType.LUNCH, MealTypeClassifier.classify(14 * 60 + 59))
        assertEquals(MealType.DINNER, MealTypeClassifier.classify(15 * 60))
        assertEquals(MealType.DINNER, MealTypeClassifier.classify(20 * 60 + 59))
        assertEquals(MealType.LATE_NIGHT, MealTypeClassifier.classify(21 * 60))
        assertEquals(MealType.LATE_NIGHT, MealTypeClassifier.classify(23 * 60 + 59))
    }
}
