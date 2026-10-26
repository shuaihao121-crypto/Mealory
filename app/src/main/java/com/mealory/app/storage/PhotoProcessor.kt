package com.mealory.app.storage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.mealory.app.data.preferences.PhotoQuality
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoProcessor(
    private val contentResolver: ContentResolver,
    private val photoStorage: PhotoStorage,
    private val exifReader: ExifReader,
) {
    suspend fun prepare(
        source: Uri,
        mealId: String,
        photoId: String,
        date: LocalDate,
        quality: PhotoQuality,
    ): PreparedPhoto = withContext(Dispatchers.IO) {
        val exif = exifReader.read(source)
        val bounds = decodeBounds(source)
        require(bounds.first > 0 && bounds.second > 0) { "Unsupported or damaged image" }

        val maxEdge = if (quality == PhotoQuality.HIGH) 4_096 else 2_560
        val jpegQuality = if (quality == PhotoQuality.HIGH) 95 else 90
        val sampled = decodeBitmap(source, calculateSampleSize(bounds.first, bounds.second, maxEdge))
        val oriented = applyOrientation(sampled, exif.orientation)
        if (oriented !== sampled) sampled.recycle()
        val scaled = scaleDown(oriented, maxEdge)
        if (scaled !== oriented) oriented.recycle()

        val (stagingFile, finalFile) = photoStorage.createPreparedTarget(mealId, photoId, date)
        try {
            stagingFile.outputStream().buffered().use { output ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                    "Could not encode image"
                }
            }
            PreparedPhoto(
                id = photoId,
                mealId = mealId,
                stagingFile = stagingFile,
                finalFile = finalFile,
                relativePath = photoStorage.relativePath(finalFile),
                width = scaled.width,
                height = scaled.height,
                fileSize = stagingFile.length(),
                originalTakenAt = exif.originalTakenAt,
            )
        } catch (error: Throwable) {
            stagingFile.delete()
            throw error
        } finally {
            scaled.recycle()
        }
    }

    private fun decodeBounds(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        return options.outWidth to options.outHeight
    }

    private fun decodeBitmap(uri: Uri, sampleSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Could not decode image")
    }

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width / (sample * 2), height / (sample * 2)) >= maxEdge) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        return bitmap.scale(
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
        )
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
