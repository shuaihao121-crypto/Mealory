package com.mealory.app.storage

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class PhotoExif(
    val orientation: Int = ExifInterface.ORIENTATION_NORMAL,
    val originalTakenAt: Long? = null,
)

class ExifReader(
    private val contentResolver: ContentResolver,
) {
    fun read(uri: Uri): PhotoExif = runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            PhotoExif(
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                ),
                originalTakenAt = exif.originalTakenAt(),
            )
        }
    }.getOrNull() ?: PhotoExif()

    private fun ExifInterface.originalTakenAt(): Long? {
        val value = getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: getAttribute(ExifInterface.TAG_DATETIME)
            ?: return null
        return runCatching {
            val localDateTime = LocalDateTime.parse(value, ExifDateTimeFormatter)
            val offset = getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
                ?.let { ZoneOffset.of(it) }
            if (offset != null) {
                localDateTime.toInstant(offset).toEpochMilli()
            } else {
                localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }.getOrNull()
    }

    companion object {
        private val ExifDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    }
}
