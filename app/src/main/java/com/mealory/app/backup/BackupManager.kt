package com.mealory.app.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.mealory.app.BuildConfig
import com.mealory.app.data.database.MealoryDatabase
import com.mealory.app.data.preferences.MealorySettings
import com.mealory.app.data.preferences.SettingsRepository
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PreparedRestore internal constructor(
    internal val workspace: File,
    internal val stagedPhotosRoot: File,
    internal val snapshot: BackupSnapshot,
    internal val manifest: BackupManifest,
) {
    val preview = BackupPreview(manifest.createdAt, manifest.appVersion, manifest.counts)
}

class BackupManager(
    context: Context,
    private val database: MealoryDatabase,
    private val settingsRepository: SettingsRepository,
    private val filesRoot: File = context.applicationContext.filesDir,
    private val cacheRoot: File = context.applicationContext.cacheDir,
) {
    private val appContext = context.applicationContext
    private val picturesRoot = File(filesRoot, "pictures")
    private val mealsPhotosRoot = File(picturesRoot, "meals")

    suspend fun exportTo(uri: Uri): BackupOperationResult = withContext(Dispatchers.IO) {
        val snapshot = readSnapshot()
        validateSnapshot(snapshot)
        val createdAt = System.currentTimeMillis()
        val resolvedPhotos = LinkedHashMap<String, File>(snapshot.photos.size)
        val photoFiles = snapshot.photos.map { photo ->
            val file = resolvePrivatePhoto(photo.relativePath)
            require(file.isFile) { "照片文件缺失：${photo.id}" }
            resolvedPhotos[photo.id] = file
            BackupPhotoFile(
                photoId = photo.id,
                zipPath = "photos/${photo.id}.jpg",
                size = file.length(),
                sha256 = file.sha256(),
            )
        }
        val manifest = BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = createdAt,
            counts = snapshot.counts,
            photoFiles = photoFiles,
        )
        val temporaryZip = File(cacheRoot, "Mealory_Backup_${UUID.randomUUID()}.zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(temporaryZip))).use { zip ->
                zip.writeText(MANIFEST, BackupJson.manifestToString(manifest))
                zip.writeText(MEALS, BackupJson.mealsToString(snapshot.meals))
                zip.writeText(PHOTOS, BackupJson.photosToString(snapshot.photos))
                zip.writeText(PEOPLE, BackupJson.peopleToString(snapshot.people))
                zip.writeText(PLACES, BackupJson.placesToString(snapshot.places))
                zip.writeText(MEAL_PEOPLE, BackupJson.mealPeopleToString(snapshot.mealPeople))
                zip.writeText(SETTINGS, BackupJson.settingsToString(snapshot.settings))
                photoFiles.forEach { entry ->
                    zip.putNextEntry(ZipEntry(entry.zipPath))
                    FileInputStream(requireNotNull(resolvedPhotos[entry.photoId]))
                        .use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            val output = requireNotNull(appContext.contentResolver.openOutputStream(uri, "w")) {
                "无法写入所选位置"
            }
            output.use { destination -> FileInputStream(temporaryZip).use { it.copyTo(destination) } }
            settingsRepository.setLastBackupAt(createdAt)
            BackupOperationResult(snapshot.meals.size, snapshot.photos.size, createdAt)
        } finally {
            temporaryZip.delete()
        }
    }

    suspend fun prepareRestore(uri: Uri): PreparedRestore = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString()
        val workspace = File(cacheRoot, "mealory-restore-$token").apply { mkdirs() }
        val archive = File(workspace, "backup.zip")
        val extracted = File(workspace, "extracted").apply { mkdirs() }
        val stagedPhotos = File(picturesRoot, ".restore-prepared-$token")
        try {
            val input = requireNotNull(appContext.contentResolver.openInputStream(uri)) { "无法读取所选备份" }
            input.use { source -> FileOutputStream(archive).use { source.copyToLimited(it, MAX_ARCHIVE_BYTES) } }
            unzipSafely(archive, extracted)
            val manifest = BackupJson.manifestFromString(extracted.requiredFile(MANIFEST).readText())
            require(manifest.formatVersion == BACKUP_FORMAT_VERSION) {
                "不支持的备份格式版本：${manifest.formatVersion}"
            }
            val snapshot = BackupSnapshot(
                meals = BackupJson.mealsFromString(extracted.requiredFile(MEALS).readText()),
                photos = BackupJson.photosFromString(extracted.requiredFile(PHOTOS).readText()),
                people = BackupJson.peopleFromString(extracted.requiredFile(PEOPLE).readText()),
                places = BackupJson.placesFromString(extracted.requiredFile(PLACES).readText()),
                mealPeople = BackupJson.mealPeopleFromString(extracted.requiredFile(MEAL_PEOPLE).readText()),
                settings = BackupJson.settingsFromString(extracted.requiredFile(SETTINGS).readText()),
            )
            require(snapshot.counts == manifest.counts) { "备份计数与数据内容不一致" }
            validateSnapshot(snapshot)
            validateAndStagePhotos(extracted, stagedPhotos, snapshot, manifest)
            PreparedRestore(workspace, stagedPhotos, snapshot, manifest)
        } catch (error: Throwable) {
            workspace.deleteRecursively()
            stagedPhotos.deleteRecursively()
            throw error
        }
    }

    suspend fun commitRestore(prepared: PreparedRestore): BackupOperationResult = withContext(Dispatchers.IO) {
        val currentSnapshot = readSnapshot()
        val rollbackPhotos = File(picturesRoot, ".restore-rollback-${UUID.randomUUID()}")
        var photosSwapped = false
        try {
            picturesRoot.mkdirs()
            if (mealsPhotosRoot.exists()) moveDirectory(mealsPhotosRoot, rollbackPhotos)
            moveDirectory(prepared.stagedPhotosRoot, mealsPhotosRoot)
            photosSwapped = true
            replaceDatabase(prepared.snapshot)
            settingsRepository.replaceAll(
                prepared.snapshot.settings.copy(
                    onboardingCompleted = true,
                    lastBackupAt = prepared.manifest.createdAt,
                ),
            )
            rollbackPhotos.deleteRecursively()
            BackupOperationResult(
                prepared.snapshot.meals.size,
                prepared.snapshot.photos.size,
                System.currentTimeMillis(),
            )
        } catch (error: Throwable) {
            runCatching { replaceDatabase(currentSnapshot) }
            runCatching { settingsRepository.replaceAll(currentSnapshot.settings) }
            if (photosSwapped) {
                mealsPhotosRoot.deleteRecursively()
                if (rollbackPhotos.exists()) moveDirectory(rollbackPhotos, mealsPhotosRoot)
            }
            throw error
        } finally {
            discardPrepared(prepared)
            if (!photosSwapped && rollbackPhotos.exists()) rollbackPhotos.deleteRecursively()
        }
    }

    fun discardPrepared(prepared: PreparedRestore) {
        prepared.workspace.deleteRecursively()
        prepared.stagedPhotosRoot.deleteRecursively()
    }

    private suspend fun readSnapshot(): BackupSnapshot {
        val settings = settingsRepository.settings.first()
        return database.withTransaction {
            val dao = database.backupDao()
            BackupSnapshot(
                meals = dao.getMeals(),
                photos = dao.getPhotos(),
                people = dao.getPeople(),
                places = dao.getPlaces(),
                mealPeople = dao.getMealPeople(),
                settings = settings,
            )
        }
    }

    private suspend fun replaceDatabase(snapshot: BackupSnapshot) = database.withTransaction {
        val dao = database.backupDao()
        dao.clearMealPeople()
        dao.clearPhotos()
        dao.clearMeals()
        dao.clearPeople()
        dao.clearPlaces()
        if (snapshot.places.isNotEmpty()) dao.insertPlaces(snapshot.places)
        if (snapshot.people.isNotEmpty()) dao.insertPeople(snapshot.people)
        if (snapshot.meals.isNotEmpty()) dao.insertMeals(snapshot.meals)
        if (snapshot.photos.isNotEmpty()) dao.insertPhotos(snapshot.photos)
        if (snapshot.mealPeople.isNotEmpty()) dao.insertMealPeople(snapshot.mealPeople)
    }

    private fun validateSnapshot(snapshot: BackupSnapshot) {
        require(snapshot.meals.size <= MAX_MEALS) { "备份中的记录数量过大" }
        require(snapshot.photos.size <= MAX_PHOTOS) { "备份中的照片数量过大" }
        requireUnique(snapshot.meals.map { it.id }, "Meal")
        requireUnique(snapshot.photos.map { it.id }, "照片")
        requireUnique(snapshot.people.map { it.id }, "人物")
        requireUnique(snapshot.places.map { it.id }, "地点")
        requireUnique(snapshot.mealPeople.map { "${it.mealId}\u0000${it.personId}" }, "人物关系")
        val meals = snapshot.meals.associateBy { it.id }
        val photos = snapshot.photos.associateBy { it.id }
        val people = snapshot.people.map { it.id }.toSet()
        val places = snapshot.places.map { it.id }.toSet()
        snapshot.meals.forEach { meal ->
            validateId(meal.id)
            require(meal.minuteOfDay in 0..1_439) { "Meal 时间无效：${meal.id}" }
            require(meal.timeZoneId.length in 1..100) { "Meal 时区无效：${meal.id}" }
            require(meal.title == null || meal.title.length <= 500) { "饭名过长：${meal.id}" }
            require(meal.note == null || meal.note.length <= 100_000) { "备注过长：${meal.id}" }
            require(meal.placeId == null || meal.placeId in places) { "Meal 地点关系缺失：${meal.id}" }
            require(meal.coverPhotoId == null || photos[meal.coverPhotoId]?.mealId == meal.id) {
                "Meal 封面关系无效：${meal.id}"
            }
        }
        snapshot.photos.forEach { photo ->
            validateId(photo.id)
            require(photo.mealId in meals) { "照片对应的 Meal 不存在：${photo.id}" }
            require(photo.width > 0 && photo.height > 0 && photo.fileSize >= 0) { "照片元数据无效：${photo.id}" }
            require(photo.mimeType == "image/jpeg") { "不支持的照片类型：${photo.id}" }
            validatePhotoRelativePath(photo.relativePath)
        }
        snapshot.people.forEach { validateId(it.id); require(it.name.isNotBlank() && it.name.length <= 200) }
        snapshot.places.forEach { validateId(it.id); require(it.name.isNotBlank() && it.name.length <= 200) }
        snapshot.mealPeople.forEach {
            require(it.mealId in meals && it.personId in people) { "Meal 与人物关系无效" }
        }
    }

    private fun validateAndStagePhotos(
        extracted: File,
        stagedPhotos: File,
        snapshot: BackupSnapshot,
        manifest: BackupManifest,
    ) {
        require(manifest.photoFiles.size == snapshot.photos.size) { "照片清单数量不一致" }
        requireUnique(manifest.photoFiles.map { it.photoId }, "照片清单")
        val metadata = snapshot.photos.associateBy { it.id }
        manifest.photoFiles.forEach { entry ->
            validateId(entry.photoId)
            require(entry.zipPath == "photos/${entry.photoId}.jpg") { "照片 ZIP 路径无效" }
            require(entry.sha256.matches(Regex("[0-9a-f]{64}"))) { "照片哈希无效" }
            val photo = requireNotNull(metadata[entry.photoId]) { "照片清单引用不存在的照片" }
            val source = extracted.requiredFile(entry.zipPath)
            require(source.length() == entry.size) { "照片大小校验失败：${entry.photoId}" }
            require(source.sha256() == entry.sha256) { "照片哈希校验失败：${entry.photoId}" }
            val relativeUnderMeals = photo.relativePath.removePrefix("pictures/meals/")
            val destination = File(stagedPhotos, relativeUnderMeals).canonicalFile
            require(destination.toPath().startsWith(stagedPhotos.canonicalFile.toPath())) { "照片恢复路径越界" }
            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = true)
        }
        val actualPhotoEntries = File(extracted, "photos").walkTopDown().filter(File::isFile).count()
        require(actualPhotoEntries == manifest.photoFiles.size) { "ZIP 含有未声明的照片" }
    }

    private fun unzipSafely(archive: File, destination: File) {
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "ZIP 文件数量过大" }
                val name = entry.name.replace('\\', '/')
                require(!name.startsWith('/') && !name.contains("../") && name != "..") { "ZIP 路径不安全" }
                val output = File(destination, name).canonicalFile
                require(output.toPath().startsWith(destination.canonicalFile.toPath())) { "ZIP 路径越界" }
                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { target ->
                        val written = zip.copyToLimited(target, MAX_ENTRY_BYTES)
                        totalBytes += written
                        require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "ZIP 解压内容过大" }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun resolvePrivatePhoto(relativePath: String): File {
        validatePhotoRelativePath(relativePath)
        val file = File(filesRoot, relativePath).canonicalFile
        require(file.toPath().startsWith(mealsPhotosRoot.canonicalFile.toPath())) { "照片路径越界" }
        return file
    }

    private fun validatePhotoRelativePath(path: String) {
        require(path.startsWith("pictures/meals/") && !path.contains("..") && !path.contains('\\')) {
            "照片相对路径无效"
        }
    }

    private fun moveDirectory(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private fun validateId(id: String) {
        require(id.length in 1..128 && id.matches(Regex("[A-Za-z0-9._-]+"))) { "ID 格式无效" }
    }

    private fun requireUnique(values: List<String>, label: String) {
        require(values.toSet().size == values.size) { "$label 包含重复 ID" }
    }

    private fun File.requiredFile(relativePath: String): File {
        val file = File(this, relativePath)
        require(file.isFile) { "备份缺少 $relativePath" }
        return file
    }

    companion object {
        private const val MANIFEST = "manifest.json"
        private const val MEALS = "data/meals.json"
        private const val PHOTOS = "data/photos.json"
        private const val PEOPLE = "data/people.json"
        private const val PLACES = "data/places.json"
        private const val MEAL_PEOPLE = "data/meal_people.json"
        private const val SETTINGS = "data/settings.json"
        private const val MAX_ENTRIES = 200_000
        private const val MAX_MEALS = 100_000
        private const val MAX_PHOTOS = 900_000
        private const val MAX_ENTRY_BYTES = 128L * 1024 * 1024
        private const val MAX_ARCHIVE_BYTES = 20L * 1024 * 1024 * 1024
        private const val MAX_UNCOMPRESSED_BYTES = 40L * 1024 * 1024 * 1024
    }
}

private fun ZipOutputStream.writeText(path: String, value: String) {
    putNextEntry(ZipEntry(path))
    write(value.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(this).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun java.io.InputStream.copyToLimited(output: java.io.OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw IOException("输入内容超过允许大小")
        output.write(buffer, 0, count)
    }
    return total
}
