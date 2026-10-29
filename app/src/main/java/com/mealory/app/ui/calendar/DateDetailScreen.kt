package com.mealory.app.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mealory.app.ui.components.MealPhotoImage
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    onBack: () -> Unit,
    onMealClick: (String) -> Unit,
    viewModel: DateDetailViewModel = viewModel(),
) {
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val date = LocalDate.ofEpochDay(viewModel.epochDay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("这一天") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    date.dayOfWeek.chineseLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (meals.isEmpty()) {
                item { Text("这一天还没有记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(meals, key = { it.meal.id }) { meal ->
                    DayMealCard(meal = meal, onClick = { onMealClick(meal.meal.id) })
                }
            }
        }
    }
}

@Composable
private fun DayMealCard(meal: MealWithDetails, onClick: () -> Unit) {
    val context = LocalContext.current
    val cover = meal.photos.firstOrNull { it.id == meal.meal.coverPhotoId } ?: meal.photos.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (cover != null) {
            MealPhotoImage(
                model = File(context.filesDir, cover.relativePath),
                contentDescription = meal.meal.title ?: "这一顿的照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                "%s · %02d:%02d".format(
                    meal.meal.mealType.label,
                    meal.meal.minuteOfDay / 60,
                    meal.meal.minuteOfDay % 60,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            meal.meal.title?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            meal.place?.let { Text("📍 ${it.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (meal.people.isNotEmpty()) {
                Text("👥 ${meal.people.joinToString("、") { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            meal.meal.note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private val DayOfWeek.chineseLabel: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "星期一"
        DayOfWeek.TUESDAY -> "星期二"
        DayOfWeek.WEDNESDAY -> "星期三"
        DayOfWeek.THURSDAY -> "星期四"
        DayOfWeek.FRIDAY -> "星期五"
        DayOfWeek.SATURDAY -> "星期六"
        DayOfWeek.SUNDAY -> "星期日"
    }

private val MealType.label: String
    get() = when (this) {
        MealType.BREAKFAST -> "早餐"
        MealType.LUNCH -> "午餐"
        MealType.DINNER -> "晚餐"
        MealType.LATE_NIGHT -> "夜宵"
        MealType.SNACK -> "零食"
        MealType.AFTERNOON_TEA -> "下午茶"
        MealType.OTHER -> "其他"
    }
