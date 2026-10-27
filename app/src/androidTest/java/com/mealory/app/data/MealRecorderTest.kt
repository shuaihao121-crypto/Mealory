package com.mealory.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.repository.MealPhotoSource
import com.mealory.app.data.repository.MealRecordDraft
import com.mealory.app.data.repository.MealRecorder
import com.mealory.app.data.repository.MealRepository
import com.mealory.app.storage.ExifReader
import com.mealory.app.storage.PhotoProcessor
import com.mealory.app.storage.PhotoStorage
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealRecorderTest {
    private lateinit var context: Context
    private lateinit var database: MealoryDatabase
    private lateinit var repository: MealRepository
    private lateinit var recorder: MealRecorder
    private lateinit var storage: PhotoStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MealoryDatabase::class.java).build()
        repository = MealRepository(database)
        storage = PhotoStorage(context)
        recorder = MealRecorder(
            repository,
            PhotoProcessor(context.contentResolver, storage, ExifReader(context.contentResolver)),
            storage,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordsMealWithoutPhotoAndKeepsOptionalRelations() = runTest {
        val mealId = recorder.record(
            MealRecordDraft(
                date = LocalDate.of(2026, 10, 26),
                minuteOfDay = 12 * 60 + 36,
                mealType = MealType.LUNCH,
                title = "  牛肉面  ",
                note = "  很暖和  ",
                placeName = "  一食堂  ",
                peopleNames = listOf("小王", " 小王 ", "妈妈"),
            ),
        )

        val saved = repository.getMeal(mealId)!!
        assertEquals("牛肉面", saved.meal.title)
        assertEquals("很暖和", saved.meal.note)
        assertEquals("一食堂", saved.place?.name)
        assertEquals(setOf("小王", "妈妈"), saved.people.map { it.name }.toSet())
        assertTrue(saved.photos.isEmpty())
        assertNull(saved.meal.coverPhotoId)

        recorder.record(
            MealRecordDraft(
                date = LocalDate.of(2026, 10, 27),
                minuteOfDay = 18 * 60,
                mealType = MealType.DINNER,
                placeName = "一食堂",
                peopleNames = listOf("小王", "妈妈"),
            ),
        )
        assertEquals(1, repository.observePlaces().first().size)
        assertEquals(2, repository.observePeople().first().size)
        assertEquals(2L, repository.observePlaceSummaries().first().single().mealCount)
        assertEquals(2L, repository.observePersonSummaries().first().first().mealCount)
    }

    @Test
    fun recordsPhotoAndDeletesDisposableCameraFile() = runTest {
        val cameraFile = File(context.cacheDir, "meal-recorder-camera.jpg")
        createPhoto(cameraFile)

        val mealId = recorder.record(
            MealRecordDraft(
                date = LocalDate.of(2026, 10, 26),
                minuteOfDay = 18 * 60,
                mealType = MealType.DINNER,
                photos = listOf(MealPhotoSource(Uri.fromFile(cameraFile), cameraFile)),
                photoQuality = PhotoQuality.STANDARD,
            ),
        )

        val saved = repository.getMeal(mealId)!!
        assertEquals(1, saved.photos.size)
        assertEquals(saved.photos.single().id, saved.meal.coverPhotoId)
        val archived = File(context.filesDir, saved.photos.single().relativePath)
        assertTrue(archived.exists())
        assertFalse(cameraFile.exists())

        archived.delete()
        archived.parentFile?.delete()
    }

    @Test
    fun rejectsMoreThanNinePhotosBeforeWriting() = runTest {
        val repeated = Uri.parse("content://mealory.invalid/photo")
        val result = runCatching {
            recorder.record(
                MealRecordDraft(
                    date = LocalDate.of(2026, 10, 26),
                    minuteOfDay = 12 * 60,
                    mealType = MealType.LUNCH,
                    photos = List(10) { MealPhotoSource(repeated) },
                ),
            )
        }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(repository.observeActiveMeals().first().isEmpty())
    }

    private fun createPhoto(file: File) {
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(130, 90, 60))
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
        }
        bitmap.recycle()
    }
}
