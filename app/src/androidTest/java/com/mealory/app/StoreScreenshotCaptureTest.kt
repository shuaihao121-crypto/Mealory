package com.mealory.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Opt-in store screenshot capture that reads pixels from Compose directly.
 * This avoids emulator framebuffer/screencap issues and is skipped normally.
 */
class StoreScreenshotCaptureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureStoreScreenshots() {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("captureStoreScreenshots") == "true",
        )

        waitForText("一碗热汤面")
        capture("01-home")

        composeRule.onNodeWithContentDescription("记录一顿饭").performClick()
        waitForText("无照片补记")
        composeRule.onNodeWithText("无照片补记").performClick()
        waitForText("吃了什么")
        capture("02-record")
        composeRule.onNodeWithText("取消").performClick()
        waitForText("一碗热汤面")

        composeRule.onNodeWithContentDescription("日历", useUnmergedTree = true).performClick()
        waitForText("月视图")
        capture("03-calendar")

        composeRule.onNodeWithContentDescription("回忆", useUnmergedTree = true).performClick()
        waitForText("过去的今天")
        capture("04-memories")

        composeRule.onNodeWithContentDescription("我的", useUnmergedTree = true).performClick()
        waitForText("数据与备份")
        composeRule.onNodeWithText("数据与备份").performClick()
        waitForText("导出完整备份")
        capture("05-backup")
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDirectory = File(requireNotNull(context.getExternalFilesDir(null)), "store-screenshots")
        check(outputDirectory.exists() || outputDirectory.mkdirs())
        FileOutputStream(File(outputDirectory, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
