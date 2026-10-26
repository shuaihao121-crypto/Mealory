package com.mealory.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnboardingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetOnboarding() {
        val application = composeRule.activity.application as MealoryApplication
        runBlocking {
            application.container.settingsRepository.setOnboardingCompleted(false)
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun onboardingAppearsAfterSplash() {
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("开始记录").assertIsDisplayed()
    }

    @Test
    fun mainNavigationAndMealEntrySheetAreReachable() {
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodesWithText("开始记录").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("开始记录").performClick()
        composeRule.onNodeWithText("从这一顿开始").assertIsDisplayed()

        val application = composeRule.activity.application as MealoryApplication
        runBlocking {
            application.container.settingsRepository.settings.first { it.onboardingCompleted }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodesWithText("从这一顿开始").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("记录第一顿饭").performClick()
        composeRule.onNodeWithText("拍照").assertIsDisplayed()
        composeRule.onNodeWithText("从相册选择").assertIsDisplayed()
        composeRule.onNodeWithText("无照片补记").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.onNodeWithContentDescription("日历", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("以后，每一天都会留下一张温暖的照片。").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("回忆", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Mealory 会在合适的时候，把过去轻轻带回来。").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("我的", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("人物、地点、备份和外观设置会安静地放在这里。").assertIsDisplayed()
    }
}

