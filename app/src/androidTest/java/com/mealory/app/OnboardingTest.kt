package com.mealory.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mealory.app.data.database.entity.MealEntity
import com.mealory.app.data.database.entity.MealType
import java.time.LocalDate
import java.time.ZoneId
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
            application.container.database.clearAllTables()
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("从这一顿开始").fetchSemanticsNodes().isNotEmpty()
        }
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
        composeRule.onNodeWithText("无照片补记").performClick()
        composeRule.onNodeWithText("保存").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("这一顿，记下了。").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("打开这顿饭").performClick()
        composeRule.onNodeWithText("这一顿").assertIsDisplayed()
        composeRule.onNodeWithText("编辑").performClick()
        composeRule.onNodeWithText("保存修改").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("删除").performClick()
        composeRule.onNodeWithText("删除").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("从这一顿开始").fetchSemanticsNodes().isNotEmpty()
        }

        val today = LocalDate.now()
        runBlocking {
            application.container.mealRepository.createMeal(
                MealEntity(
                    id = "calendar-ui-meal",
                    dateEpochDay = today.toEpochDay(),
                    minuteOfDay = 12 * 60,
                    mealType = MealType.LUNCH,
                    title = "日历测试",
                    note = null,
                    placeId = null,
                    coverPhotoId = null,
                    timeZoneId = ZoneId.systemDefault().id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null,
                ),
            )
        }

        composeRule.onNodeWithContentDescription("日历", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("月视图").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("$today，1顿饭").performClick()
        composeRule.onNodeWithText("查看这一天").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("日历测试").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("年视图").performClick()
        composeRule.onNodeWithText("1月").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("月视图").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("回忆", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Mealory 会在合适的时候，把过去轻轻带回来。").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("我的", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("人物、地点、备份和外观设置会安静地放在这里。").assertIsDisplayed()
    }
}

