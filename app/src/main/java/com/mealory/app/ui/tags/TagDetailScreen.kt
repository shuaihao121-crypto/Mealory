package com.mealory.app.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    onMealClick: (String) -> Unit,
    viewModel: PersonDetailViewModel = viewModel(),
) {
    val person by viewModel.person.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    TagDetailScreen(
        topBarTitle = "人物",
        name = person?.name ?: "人物",
        meals = meals,
        onBack = onBack,
        onMealClick = onMealClick,
    )
}

@Composable
fun PlaceDetailScreen(
    onBack: () -> Unit,
    onMealClick: (String) -> Unit,
    viewModel: PlaceDetailViewModel = viewModel(),
) {
    val place by viewModel.place.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    TagDetailScreen(
        topBarTitle = "地点",
        name = place?.name ?: "地点",
        meals = meals,
        onBack = onBack,
        onMealClick = onMealClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagDetailScreen(
    topBarTitle: String,
    name: String,
    meals: List<MealWithDetails>,
    onBack: () -> Unit,
    onMealClick: (String) -> Unit,
) {
    val dates = meals.map { it.meal.dateEpochDay }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("一起记录过 ${meals.size} 顿", style = MaterialTheme.typography.titleMedium)
                if (dates.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
                    Text(
                        "第一次：${LocalDate.ofEpochDay(dates.min()).format(formatter)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "最近一次：${LocalDate.ofEpochDay(dates.max()).format(formatter)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (meals.isEmpty()) {
                item { Text("还没有关联的 Meal。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(meals, key = { it.meal.id }) { meal ->
                    RelatedMealCard(meal, onClick = { onMealClick(meal.meal.id) })
                }
            }
        }
    }
}

@Composable
private fun RelatedMealCard(meal: MealWithDetails, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        if (meal.photos.isNotEmpty()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(meal.photos.sortedBy { it.sortOrder }, key = { it.id }) { photo ->
                    AsyncImage(
                        model = File(context.filesDir, photo.relativePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(13.dp)),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .size(8.dp),
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val date = LocalDate.ofEpochDay(meal.meal.dateEpochDay)
            Text(
                "%s · %s · %02d:%02d".format(
                    meal.meal.mealType.label,
                    date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)),
                    meal.meal.minuteOfDay / 60,
                    meal.meal.minuteOfDay % 60,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            meal.meal.title?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
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
