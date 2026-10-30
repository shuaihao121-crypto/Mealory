package com.mealory.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.repository.MealPhotoSource
import com.mealory.app.data.repository.MealRecordDraft
import com.mealory.app.ui.privacy.CurrentPrivacyPolicyVersion
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in helper for creating non-personal demo content before capturing store screenshots.
 * It is skipped during the normal connected test suite and is never packaged in the app.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotSeedTest {
    @Test
    fun seedStoreScreenshotData() = runTest {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("seedStoreScreenshots") == "true",
        )

        val application = instrumentation.targetContext.applicationContext as MealoryApplication
        val container = application.container
        val cache = application.cacheDir
        val breakfast = File(cache, "store-demo-breakfast.png").requireReadable()
        val lunch = File(cache, "store-demo-lunch.png").requireReadable()
        val dinner = File(cache, "store-demo-dinner.png").requireReadable()

        container.database.clearAllTables()
        File(application.filesDir, "pictures").deleteRecursively()
        container.settingsRepository.setPrivacyPolicyVersionAccepted(CurrentPrivacyPolicyVersion)
        container.settingsRepository.setOnboardingCompleted(true)

        suspend fun record(
            date: LocalDate,
            minuteOfDay: Int,
            mealType: MealType,
            title: String,
            note: String,
            place: String,
            people: List<String>,
            photo: File,
        ) {
            container.mealRecorder.record(
                MealRecordDraft(
                    date = date,
                    minuteOfDay = minuteOfDay,
                    mealType = mealType,
                    title = title,
                    note = note,
                    placeName = place,
                    peopleNames = people,
                    photos = listOf(MealPhotoSource(Uri.fromFile(photo))),
                    photoQuality = PhotoQuality.STANDARD,
                ),
            )
        }

        record(
            LocalDate.of(2026, 10, 29), 8 * 60 + 10, MealType.BREAKFAST,
            "清晨的热粥", "慢慢吃完，再开始今天。", "家", listOf("妈妈"), breakfast,
        )
        record(
            LocalDate.of(2026, 10, 29), 12 * 60 + 30, MealType.LUNCH,
            "家常午饭", "番茄炒蛋还是最熟悉的味道。", "一食堂", listOf("小王"), lunch,
        )
        record(
            LocalDate.of(2026, 10, 29), 18 * 60 + 45, MealType.DINNER,
            "一碗热汤面", "晚上的风有点凉。", "家", emptyList(), dinner,
        )
        record(
            LocalDate.of(2026, 10, 28), 12 * 60 + 5, MealType.LUNCH,
            "番茄炒蛋", "普通的一顿，也值得留下。", "一食堂", listOf("小王"), lunch,
        )
        record(
            LocalDate.of(2026, 10, 25), 19 * 60 + 20, MealType.DINNER,
            "周末饺子", "回家吃饭。", "家", listOf("爸爸", "妈妈"), dinner,
        )
        record(
            LocalDate.of(2026, 9, 24), 11 * 60 + 32, MealType.LUNCH,
            "第一次记录", "从这一顿开始。", "校园", emptyList(), breakfast,
        )
        record(
            LocalDate.of(2025, 10, 29), 18 * 60 + 10, MealType.DINNER,
            "去年的这一天", "时间会把一顿饭变成回忆。", "家", listOf("妈妈"), dinner,
        )
    }
}

private fun File.requireReadable(): File = apply {
    require(isFile && canRead()) { "Missing screenshot seed image: $absolutePath" }
}
