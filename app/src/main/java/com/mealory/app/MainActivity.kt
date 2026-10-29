package com.mealory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mealory.app.ui.MealoryApp
import com.mealory.app.ui.theme.MealoryTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.mealory.app.data.preferences.MealorySettings
import com.mealory.app.data.preferences.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = 0xFFFFF8EE.toInt(),
                darkScrim = 0xFF241C19.toInt(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = 0xFFFFF8EE.toInt(),
                darkScrim = 0xFF241C19.toInt(),
            ),
        )
        val settingsRepository = (application as MealoryApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(MealorySettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MealoryTheme(darkTheme = darkTheme) {
                MealoryApp()
            }
        }
    }
}

