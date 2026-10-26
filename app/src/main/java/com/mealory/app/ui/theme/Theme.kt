package com.mealory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CoffeeBrown,
    onPrimary = WarmWhite,
    primaryContainer = WarmBeige,
    onPrimaryContainer = CoffeeBrown,
    secondary = Terracotta,
    onSecondary = WarmWhite,
    secondaryContainer = ColorTokens.TerracottaContainer,
    onSecondaryContainer = CoffeeBrown,
    tertiary = Olive,
    onTertiary = WarmWhite,
    background = WarmCream,
    onBackground = Ink,
    surface = WarmCream,
    onSurface = Ink,
    surfaceVariant = ColorTokens.SurfaceVariantLight,
    onSurfaceVariant = CoffeeBrownLight,
    outline = ColorTokens.OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = ColorTokens.PrimaryDark,
    onPrimary = WarmCreamDark,
    primaryContainer = CoffeeBrownLight,
    onPrimaryContainer = WarmWhite,
    secondary = ColorTokens.SecondaryDark,
    onSecondary = WarmCreamDark,
    secondaryContainer = ColorTokens.SecondaryContainerDark,
    onSecondaryContainer = WarmWhite,
    tertiary = ColorTokens.TertiaryDark,
    onTertiary = WarmCreamDark,
    background = WarmCreamDark,
    onBackground = WarmWhite,
    surface = WarmCreamDark,
    onSurface = WarmWhite,
    surfaceVariant = WarmBeigeDark,
    onSurfaceVariant = ColorTokens.OnSurfaceVariantDark,
    outline = ColorTokens.OutlineDark,
)

private object ColorTokens {
    val TerracottaContainer = androidx.compose.ui.graphics.Color(0xFFF7DDCF)
    val SurfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFF6E9D8)
    val OutlineLight = androidx.compose.ui.graphics.Color(0xFFAA8F7E)
    val PrimaryDark = androidx.compose.ui.graphics.Color(0xFFF0C6A7)
    val SecondaryDark = androidx.compose.ui.graphics.Color(0xFFFFB59A)
    val SecondaryContainerDark = androidx.compose.ui.graphics.Color(0xFF6D3929)
    val TertiaryDark = androidx.compose.ui.graphics.Color(0xFFC9CC95)
    val OnSurfaceVariantDark = androidx.compose.ui.graphics.Color(0xFFE7CFC0)
    val OutlineDark = androidx.compose.ui.graphics.Color(0xFF9C8172)
}

@Composable
fun MealoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MealoryTypography,
        content = content,
    )
}

