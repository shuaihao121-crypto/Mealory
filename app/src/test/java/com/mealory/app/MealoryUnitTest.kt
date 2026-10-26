package com.mealory.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MealoryUnitTest {
    @Test
    fun packageNameIsStable() {
        assertEquals("com.mealory.app", BuildConfig.APPLICATION_ID)
    }
}
