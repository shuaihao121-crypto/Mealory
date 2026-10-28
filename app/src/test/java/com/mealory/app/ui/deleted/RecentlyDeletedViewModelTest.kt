package com.mealory.app.ui.deleted

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyDeletedViewModelTest {
    @Test
    fun remainingDaysRoundsUpAndNeverExceedsRetentionWindow() {
        val deletedAt = 1_000L
        val day = TimeUnit.DAYS.toMillis(1)

        assertEquals(30L, remainingRetentionDays(deletedAt, deletedAt))
        assertEquals(30L, remainingRetentionDays(deletedAt, deletedAt + 1))
        assertEquals(1L, remainingRetentionDays(deletedAt, deletedAt + 29 * day + 1))
        assertEquals(0L, remainingRetentionDays(deletedAt, deletedAt + 30 * day))
    }
}
