package com.mealory.app.ui.memories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MemoriesScreen(
    onMealClick: (String) -> Unit,
    onAnnualClick: (Int) -> Unit,
    viewModel: MemoriesViewModel = viewModel(),
) {
    val stats by viewModel.journeyStats.collectAsStateWithLifecycle()
    val pastToday by viewModel.pastToday.collectAsStateWithLifecycle()
    val randomMeal by viewModel.randomMeal.collectAsStateWithLifecycle()
    val monthGroups by viewModel.pastMonthGroups.collectAsStateWithLifecycle()
    val annualYears by viewModel.annualYears.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("回忆", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (stats.mealCount == 0L) {
                    "从第一顿开始，未来会慢慢有回声。"
                } else {
                    "和 Mealory 一起 第 ${stats.dayNumber} 天\n已经留下 ${stats.mealCount} 顿饭"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            SectionTitle("过去的今天")
            if (pastToday == null) {
                QuietEmptyCard("去年今天前后，还没有可以带回来的记录。")
            } else {
                MemoryMealCard(
                    meal = pastToday!!,
                    eyebrow = "${viewModel.daysAgo(pastToday!!)}天前",
                    onClick = { onMealClick(pastToday!!.meal.id) },
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("随机一顿", Modifier.weight(1f))
                TextButton(onClick = viewModel::pickRandomMeal) { Text("再来一顿") }
            }
            AnimatedContent(
                targetState = randomMeal,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.97f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 1.03f))
                },
                label = "随机一顿",
            ) { meal ->
                if (meal == null) {
                    QuietEmptyCard("记录几顿饭以后，这里会随机翻开过去。")
                } else {
                    MemoryMealCard(
                        meal = meal,
                        eyebrow = LocalDate.ofEpochDay(meal.meal.dateEpochDay)
                            .format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)),
                        onClick = { onMealClick(meal.meal.id) },
                    )
                }
            }
        }

        item { SectionTitle("过去的这个月") }
        if (monthGroups.isEmpty()) {
            item { QuietEmptyCard("往年的这个月还没有记录。") }
        } else {
            items(monthGroups, key = PastMonthGroup::year) { group ->
                PastMonthCard(
                    group = group,
                    month = viewModel.today.monthValue,
                    onClick = { onMealClick(group.meals.first().mealId) },
                )
            }
        }

        item { SectionTitle("年度回忆") }
        if (annualYears.isEmpty()) {
            item { QuietEmptyCard("记下一顿饭以后，这里会保存属于你的年度回忆。") }
        } else {
            items(annualYears, key = { "annual-$it" }) { year ->
                AnnualEntryCard(year = year, onClick = { onAnnualClick(year) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
private fun QuietEmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun MemoryMealCard(
    meal: MealWithDetails,
    eyebrow: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val cover = meal.photos.firstOrNull { it.id == meal.meal.coverPhotoId } ?: meal.photos.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (cover != null) {
            MealPhotoImage(
                model = File(context.filesDir, cover.relativePath),
                contentDescription = meal.meal.title ?: "回忆照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eyebrow, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
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
        }
    }
}

@Composable
private fun PastMonthCard(
    group: PastMonthGroup,
    month: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val photos = group.meals.mapNotNull { it.coverRelativePath }.distinct().take(3)
            repeat(3) { index ->
                val photo = photos.getOrNull(index)
                if (photo == null) {
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                } else {
                    MealPhotoImage(
                        model = File(context.filesDir, photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("${group.year}年${month}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${group.meals.size} 顿饭", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnnualEntryCard(year: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("我的 $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("看看这一年真实留下的饭与生活", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("打开", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
