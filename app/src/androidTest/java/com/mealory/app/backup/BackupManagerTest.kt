package com.mealory.app.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealPhotoEntity
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.preferences.SettingsRepository
import com.mealory.app.data.preferences.ThemeMode
import com.mealory.app.data.repository.MealRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: MealoryDatabase
    private lateinit var repository: MealRepository
    private lateinit var settings: SettingsRepository
    private lateinit var scope: CoroutineScope
    private lateinit var root: File
    private lateinit var manager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MealoryDatabase::class.java).build()
        repository = MealRepository(database)
        root = File(context.cacheDir, "backup-manager-test-${UUID.randomUUID()}").apply { mkdirs() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { File(root, "settings.preferences_pb") },
        )
        manager = BackupManager(
            context = context,
            database = database,
            settingsRepository = settings,
            filesRoot = File(root, "files").apply { mkdirs() },
            cacheRoot = File(root, "cache").apply { mkdirs() },
        )
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
        root.deleteRecursively()
    }

    @Test
    fun completeBackupAndRestoreRoundTripPreservesRowsSettingsAndPhotoBytes() = runTest {
        val photoBytes = byteArrayOf(1, 5, 9, 13)
        val meal = meal("backup-meal", "backup-photo")
        val photo = photo(meal.id, "backup-photo")
        val photoFile = File(File(root, "files"), photo.relativePath).apply {
            parentFile!!.mkdirs()
            writeBytes(photoBytes)
        }
        repository.createMeal(meal, listOf(photo))
        repository.updateMealWithTags(meal, "家", listOf("妈妈"))
        settings.setOnboardingCompleted(true)
        settings.setThemeMode(ThemeMode.DARK)

        val zip = File(root, "roundtrip.zip")
        val export = manager.exportTo(Uri.fromFile(zip))
        assertEquals(1, export.mealCount)
        assertEquals(1, export.photoCount)
        assertTrue(zip.length() > 0)

        repository.permanentlyDelete(meal.id)
        repository.createMeal(meal("replacement", null))
        photoFile.delete()
        settings.setThemeMode(ThemeMode.LIGHT)

        val prepared = manager.prepareRestore(Uri.fromFile(zip))
        assertEquals(1, prepared.preview.counts.meals)
        manager.commitRestore(prepared)

        assertEquals("backup-meal", repository.observeActiveMeals().first().single().meal.id)
        assertNull(repository.getMeal("replacement"))
        assertEquals(ThemeMode.DARK, settings.settings.first().themeMode)
        assertArrayEquals(photoBytes, File(File(root, "files"), photo.relativePath).readBytes())
    }

    @Test
    fun tamperedPhotoHashIsRejectedWithoutChangingCurrentDatabase() = runTest {
        val meal = meal("secure-meal", "secure-photo")
        val photo = photo(meal.id, "secure-photo")
        File(File(root, "files"), photo.relativePath).apply {
            parentFile!!.mkdirs()
            writeBytes(byteArrayOf(2, 4, 6, 8))
        }
        repository.createMeal(meal, listOf(photo))
        val valid = File(root, "valid.zip")
        manager.exportTo(Uri.fromFile(valid))
        val tampered = File(root, "tampered.zip")
        rewriteZip(valid, tampered) { name, bytes ->
            if (name == "photos/secure-photo.jpg") byteArrayOf(0, 0, 0, 0) else bytes
        }

        assertTrue(runCatching { manager.prepareRestore(Uri.fromFile(tampered)) }.isFailure)
        assertEquals("secure-meal", repository.observeActiveMeals().first().single().meal.id)
    }

    @Test
    fun missingPrivatePhotoStopsExportWithoutChangingDatabase() = runTest {
        val meal = meal("missing-photo-meal", "missing-photo")
        repository.createMeal(meal, listOf(photo(meal.id, "missing-photo")))

        val destination = File(root, "missing-photo.zip")
        val failure = runCatching { manager.exportTo(Uri.fromFile(destination)) }

        assertTrue(failure.isFailure)
        assertTrue(failure.exceptionOrNull()?.message?.contains("照片文件缺失") == true)
        assertEquals("missing-photo-meal", repository.observeActiveMeals().first().single().meal.id)
    }

    private fun rewriteZip(source: File, destination: File, transform: (String, ByteArray) -> ByteArray) {
        ZipInputStream(FileInputStream(source)).use { input ->
            ZipOutputStream(FileOutputStream(destination)).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    output.putNextEntry(java.util.zip.ZipEntry(entry.name))
                    output.write(transform(entry.name, input.readBytes()))
                    output.closeEntry()
                    input.closeEntry()
                }
            }
        }
    }

    private fun meal(id: String, coverPhotoId: String?) = MealEntity(
        id = id,
        dateEpochDay = 20_500,
        minuteOfDay = 12 * 60,
        mealType = MealType.LUNCH,
        title = "测试备份",
        note = "完整记录",
        placeId = null,
        coverPhotoId = coverPhotoId,
        timeZoneId = "Asia/Shanghai",
        createdAt = 1_000,
        updatedAt = 1_000,
        deletedAt = null,
    )

    private fun photo(mealId: String, id: String) = MealPhotoEntity(
        id = id,
        mealId = mealId,
        relativePath = "pictures/meals/2026/02/$mealId/$id.jpg",
        sortOrder = 0,
        width = 4,
        height = 4,
        fileSize = 4,
        mimeType = "image/jpeg",
        originalTakenAt = null,
        createdAt = 1_000,
    )
}
