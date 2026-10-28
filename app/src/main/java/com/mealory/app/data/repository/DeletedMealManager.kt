package com.mealory.app.data.repository

import com.mealory.app.storage.PhotoStorage
import java.util.concurrent.TimeUnit

data class DeletedMealCleanupResult(
    val deletedCount: Int,
    val failedCount: Int,
)

class DeletedMealManager(
    private val repository: MealRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend fun restore(mealId: String, restoredAt: Long = System.currentTimeMillis()) {
        repository.restore(mealId, restoredAt)
    }

    suspend fun permanentlyDelete(mealId: String): Boolean {
        val meal = repository.getMeal(mealId) ?: return false
        require(meal.meal.deletedAt != null) { "Only meals in Recently Deleted can be permanently deleted" }
        val batch = photoStorage.stageForDeletion(mealId, meal.photos.map { it.relativePath })
        return try {
            if (!repository.permanentlyDelete(mealId)) {
                photoStorage.rollbackDeletion(batch)
                false
            } else {
                photoStorage.finishDeletion(batch)
                true
            }
        } catch (error: Throwable) {
            photoStorage.rollbackDeletion(batch)
            throw error
        }
    }

    suspend fun cleanupExpired(now: Long = System.currentTimeMillis()): DeletedMealCleanupResult {
        val expired = repository.getDeletedBefore(now - RETENTION_MILLIS)
        var deleted = 0
        var failed = 0
        expired.forEach { meal ->
            runCatching { permanentlyDelete(meal.meal.id) }
                .onSuccess { if (it) deleted++ else failed++ }
                .onFailure { failed++ }
        }
        return DeletedMealCleanupResult(deleted, failed)
    }

    companion object {
        val RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
