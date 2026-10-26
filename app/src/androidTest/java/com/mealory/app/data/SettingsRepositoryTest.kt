package com.mealory.app.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealory.app.data.preferences.MealTimeRanges
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.preferences.SettingsRepository
import com.mealory.app.data.preferences.ThemeMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private lateinit var scope: CoroutineScope
    private lateinit var dataFile: File
    private lateinit var repository: SettingsRepository

    @Before
    fun createRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataFile = File(context.filesDir, "datastore/test-${UUID.randomUUID()}.preferences_pb")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { dataFile },
        )
    }

    @After
    fun closeRepository() {
        scope.cancel()
        dataFile.delete()
    }

    @Test
    fun defaultsAndUpdatesRoundTrip() = runTest {
        val defaults = repository.settings.first()
        assertFalse(defaults.onboardingCompleted)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertEquals(PhotoQuality.STANDARD, defaults.photoQuality)
        assertNull(defaults.lastBackupAt)
        assertEquals(MealTimeRanges.Default, defaults.defaultMealTimeRanges)

        repository.setOnboardingCompleted(true)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setPhotoQuality(PhotoQuality.HIGH)
        repository.setLastBackupAt(123_456L)
        repository.setDefaultMealTimeRanges(MealTimeRanges.Default.copy(version = 1))

        val updated = repository.settings.first { it.onboardingCompleted }
        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertEquals(PhotoQuality.HIGH, updated.photoQuality)
        assertEquals(123_456L, updated.lastBackupAt)
        assertEquals(MealTimeRanges.Default, updated.defaultMealTimeRanges)

        repository.setLastBackupAt(null)
        assertNull(repository.settings.first { it.lastBackupAt == null }.lastBackupAt)
    }
}
