package com.mealory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mealory.app.ui.MealoryApp
import com.mealory.app.ui.theme.MealoryTheme

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
        setContent {
            MealoryTheme {
                MealoryApp()
            }
        }
    }
}

