package com.mealory.app.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.UUID

data class CameraCapture(
    val uri: Uri,
    val file: File,
)

data class PreparedPhoto(
    val id: String,
    val mealId: String,
    val stagingFile: File,
    val finalFile: File,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val originalTakenAt: Long?,
)

class PhotoStorage(
    private val context: Context,
) {
    private val filesRoot = context.filesDir
    private val stagingRoot = File(filesRoot, "pictures/.staging")

    fun createCameraCapture(): CameraCapture {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "camera-${UUID.randomUUID()}.jpg")
        check(file.createNewFile()) { "Could not create camera file" }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return CameraCapture(uri, file)
    }

    fun createPreparedTarget(
        mealId: String,
        photoId: String,
        date: LocalDate,
    ): Pair<File, File> {
        val staging = File(File(stagingRoot, mealId).apply { mkdirs() }, "$photoId.jpg")
        val final = File(
            filesRoot,
            "pictures/meals/${date.year}/${date.monthValue.toString().padStart(2, '0')}/$mealId/$photoId.jpg",
        )
        return staging to final
    }

    fun relativePath(file: File): String =
        file.relativeTo(filesRoot).invariantSeparatorsPath

    fun commit(photo: PreparedPhoto) {
        photo.finalFile.parentFile?.mkdirs()
        try {
            Files.move(
                photo.stagingFile.toPath(),
                photo.finalFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                photo.stagingFile.toPath(),
                photo.finalFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    fun discard(photo: PreparedPhoto) {
        photo.stagingFile.delete()
        photo.finalFile.delete()
        photo.stagingFile.parentFile?.delete()
        photo.finalFile.parentFile?.delete()
    }

    fun deleteCameraCapture(capture: CameraCapture?) {
        capture?.file?.delete()
    }
}
