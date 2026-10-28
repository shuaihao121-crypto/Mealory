package com.mealory.app.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mealory.app.data.database.entity.MealWithDetails
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnnualMemoryScreen(
    onBack: () -> Unit,
    onMealClick: (String) -> Unit,
    viewModel: AnnualMemoryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Column {
                    Text(
                        "我的 ${state.year}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("这一年的饭，也是一年的生活。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (state.mealCount == 0) {
            item { AnnualEmptyCard(state.year) }
        } else {
            item { AnnualStatsCard(state) }

            if (state.topPeople != null || state.topPlace != null) {
                item {
                    AnnualSectionTitle("一起吃过的日常")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.topPeople?.let {
                            AnnualHighlightCard(
                                label = "最常同行",
                                value = it.names.joinToString("、"),
                                detail = "一起留下 ${it.mealCount} 顿饭",
                            )
                        }
                        state.topPlace?.let {
                            AnnualHighlightCard(
                                label = "最常去的地方",
                                value = it.names.joinToString("、"),
                                detail = "在这里记录了 ${it.mealCount} 顿",
                            )
                        }
                    }
                }
            }

            if (state.milestones.isNotEmpty()) {
                item {
                    AnnualSectionTitle("这一年的里程碑")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.milestones.forEach { milestone ->
                            AnnualMealCard(
                                meal = milestone.meal,
                                label = "与 Mealory 的第 ${milestone.position} 顿饭",
                                onClick = { onMealClick(milestone.meal.meal.id) },
                            )
                        }
                    }
                }
            }

            if (state.seasonalPhotos.isNotEmpty()) {
                item {
                    AnnualSectionTitle("四季里的饭")
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnnualSeason.entries.forEach { season ->
                            val photos = state.seasonalPhotos[season].orEmpty()
                            if (photos.isNotEmpty()) {
                                SeasonPhotoGroup(season, photos, onMealClick)
                            }
                        }
                    }
                }
            }

            item {
                AnnualSectionTitle("从年初到年末")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sameMeal = state.firstMeal?.meal?.id == state.lastMeal?.meal?.id
                    state.firstMeal?.let {
                        AnnualMealCard(
                            meal = it,
                            label = if (sameMeal) "这一年的第一顿，也是最后一顿" else "这一年的第一顿",
                            onClick = { onMealClick(it.meal.id) },
                        )
                    }
                    if (!sameMeal) {
                        state.lastMeal?.let {
                            AnnualMealCard(
                                meal = it,
                                label = "这一年的最后一顿",
                                onClick = { onMealClick(it.meal.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualEmptyCard(year: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            "$year 年还没有留下饮食记录。\n真实发生以后，年度回忆才会出现。",
            modifier = Modifier.padding(22.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnnualStatsCard(state: AnnualMemoryState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${state.year} 年，你留下了", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(Modifier.fillMaxWidth()) {
                AnnualStat("${state.mealCount}", "顿饭", Modifier.weight(1f))
                AnnualStat("${state.recordedDayCount}", "个有记录的日子", Modifier.weight(1f))
            }
            Text(
                "没有打卡率，也没有评分。这里只有你真实记下来的生活。",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AnnualStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f))
    }
}

@Composable
private fun AnnualSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun AnnualHighlightCard(label: String, value: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeasonPhotoGroup(
    season: AnnualSeason,
    photos: List<AnnualSeasonPhoto>,
    onMealClick: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(season.label, style = MaterialTheme.typography.titleMedium)
        photos.chunked(2).forEach { rowPhotos ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowPhotos.forEach { photo ->
                    AsyncImage(
                        model = File(context.filesDir, photo.relativePath),
                        contentDescription = "${season.label}的饮食照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onMealClick(photo.mealId) },
                    )
                }
                if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnnualMealCard(
    meal: MealWithDetails,
    label: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val cover = meal.photos.firstOrNull { it.id == meal.meal.coverPhotoId } ?: meal.photos.firstOrNull()
    val date = LocalDate.ofEpochDay(meal.meal.dateEpochDay)
        .format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (cover != null) {
            AsyncImage(
                model = File(context.filesDir, cover.relativePath),
                contentDescription = meal.meal.title ?: label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("Mealory", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Text(
                meal.meal.title ?: "$date 的一顿饭",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text("$date · %02d:%02d".format(meal.meal.minuteOfDay / 60, meal.meal.minuteOfDay % 60))
        }
    }
}
