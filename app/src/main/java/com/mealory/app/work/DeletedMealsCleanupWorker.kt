package com.mealory.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mealory.app.MealoryApplication
import java.util.concurrent.TimeUnit

class DeletedMealsCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MealoryApplication
        val cleanup = app.container.deletedMealManager.cleanupExpired()
        return if (cleanup.failedCount == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val STARTUP_WORK = "mealory-deleted-meals-startup-cleanup"
        private const val PERIODIC_WORK = "mealory-deleted-meals-periodic-cleanup"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                STARTUP_WORK,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DeletedMealsCleanupWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DeletedMealsCleanupWorker>(24, TimeUnit.HOURS).build(),
            )
        }
    }
}
