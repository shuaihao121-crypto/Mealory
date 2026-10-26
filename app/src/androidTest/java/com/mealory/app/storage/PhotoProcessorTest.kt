package com.mealory.app.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealory.app.data.preferences.PhotoQuality
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoProcessorTest {
    @Test
    fun rotatesScalesAndCommitsIntoPrivateStorage() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "photo-processor-source.jpg")
        createExifPhoto(source)
        val storage = PhotoStorage(context)
        val processor = PhotoProcessor(context.contentResolver, storage, ExifReader(context.contentResolver))

        val prepared = processor.prepare(
            source = Uri.fromFile(source),
            mealId = "meal-photo-test",
            photoId = "photo-test",
            date = LocalDate.of(2026, 10, 26),
            quality = PhotoQuality.STANDARD,
        )

        assertEquals(1_280, prepared.width)
        assertEquals(2_560, prepared.height)
        assertTrue(prepared.stagingFile.exists())
        assertTrue(prepared.fileSize > 0)
        assertNotNull(prepared.originalTakenAt)
        assertEquals(
            "pictures/meals/2026/10/meal-photo-test/photo-test.jpg",
            prepared.relativePath,
        )

        storage.commit(prepared)
        assertFalse(prepared.stagingFile.exists())
        assertTrue(prepared.finalFile.exists())
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(prepared.finalFile.absolutePath, bounds)
        assertEquals(1_280, bounds.outWidth)
        assertEquals(2_560, bounds.outHeight)

        storage.discard(prepared)
        source.delete()
    }

    private fun createExifPhoto(file: File) {
        val bitmap = Bitmap.createBitmap(3_000, 1_500, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(180, 90, 60))
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
        }
        bitmap.recycle()

        ExifInterface(file).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString(),
            )
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2020:01:02 03:04:05")
            saveAttributes()
        }
    }
}
