package com.mealory.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mealory.app.data.database.entity.CalendarMealPreview
import com.mealory.app.data.database.entity.MealType
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onMealClick: (String) -> Unit,
    onOpenDay: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel(),
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val previews by viewModel.previews.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "日历",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = mode == CalendarMode.MONTH,
                onClick = { viewModel.mode.value = CalendarMode.MONTH },
                label = { Text("月视图") },
            )
            FilterChip(
                selected = mode == CalendarMode.YEAR,
                onClick = { viewModel.mode.value = CalendarMode.YEAR },
                label = { Text("年视图") },
            )
        }
        PeriodHeader(
            label = if (mode == CalendarMode.MONTH) {
                selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA))
            } else {
                "${selectedMonth.year}年"
            },
            onPrevious = viewModel::previousPeriod,
            onNext = viewModel::nextPeriod,
        )

        when (mode) {
            CalendarMode.MONTH -> MonthView(
                month = selectedMonth,
                previews = previews,
                onDayClick = { selectedDay = it },
                modifier = Modifier.weight(1f),
            )
            CalendarMode.YEAR -> YearView(
                year = selectedMonth.year,
                previews = previews,
                onMonthClick = viewModel::showMonth,
                modifier = Modifier.weight(1f),
            )
        }
    }

    selectedDay?.let { epochDay ->
        DayPreviewSheet(
            epochDay = epochDay,
            meals = previews.filter { it.dateEpochDay == epochDay },
            onDismiss = { selectedDay = null },
            onMealClick = {
                selectedDay = null
                onMealClick(it)
            },
            onOpenDay = {
                selectedDay = null
                onOpenDay(epochDay)
            },
        )
    }
}

@Composable
private fun PeriodHeader(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "上一周期")
        }
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "下一周期")
        }
    }
}

@Composable
private fun MonthView(
    month: YearMonth,
    previews: List<CalendarMealPreview>,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byDay = previews.groupBy(CalendarMealPreview::dateEpochDay)
    val firstDay = month.atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value % 7
    val usedCells = leadingEmptyCells + month.lengthOfMonth()
    val cellCount = ((usedCells + 6) / 7) * 7

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            userScrollEnabled = false,
        ) {
            items(cellCount) { index ->
                val dayNumber = index - leadingEmptyCells + 1
                if (dayNumber !in 1..month.lengthOfMonth()) {
                    Spacer(Modifier.aspectRatio(0.82f))
                } else {
                    val date = month.atDay(dayNumber)
                    DayCell(
                        date = date,
                        meals = byDay[date.toEpochDay()].orEmpty(),
                        onClick = { if (byDay.containsKey(date.toEpochDay())) onDayClick(date.toEpochDay()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    meals: List<CalendarMealPreview>,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(9.dp)
    val isToday = date == LocalDate.now()
    Box(
        modifier = Modifier
            .aspectRatio(0.82f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, shape) else Modifier)
            .semantics { contentDescription = "$date，${meals.size}顿饭" }
            .clickable(enabled = meals.isNotEmpty(), onClick = onClick),
    ) {
        meals.firstOrNull { it.coverRelativePath != null }?.coverRelativePath?.let { path ->
            AsyncImage(
                model = File(context.filesDir, path),
                contentDescription = "${date.monthValue}月${date.dayOfMonth}日代表照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(bottomEnd = 8.dp),
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
        }
        if (meals.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = meals.size.toString(),
                    color = MaterialTheme.colorScheme.onTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun YearView(
    year: Int,
    previews: List<CalendarMealPreview>,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byMonth = previews.groupBy { LocalDate.ofEpochDay(it.dateEpochDay).monthValue }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items((1..12).toList()) { month ->
            val monthMeals = byMonth[month].orEmpty()
            MonthCard(
                year = year,
                month = month,
                meals = monthMeals,
                onClick = { onMonthClick(month) },
            )
        }
    }
}

@Composable
private fun MonthCard(
    year: Int,
    month: Int,
    meals: List<CalendarMealPreview>,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f),
        ) {
            meals.firstOrNull { it.coverRelativePath != null }?.coverRelativePath?.let { path ->
                AsyncImage(
                    model = File(context.filesDir, path),
                    contentDescription = "$year 年 $month 月代表照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text("${month}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (meals.isEmpty()) "等待记录" else "${meals.size} 顿饭",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPreviewSheet(
    epochDay: Long,
    meals: List<CalendarMealPreview>,
    onDismiss: () -> Unit,
    onMealClick: (String) -> Unit,
    onOpenDay: () -> Unit,
) {
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(epochDay)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${meals.size} 顿饭",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(meals, key = CalendarMealPreview::mealId) { meal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onMealClick(meal.mealId) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (meal.coverRelativePath != null) {
                            AsyncImage(
                                model = File(context.filesDir, meal.coverRelativePath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(11.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "%02d:%02d · %s".format(
                                    meal.minuteOfDay / 60,
                                    meal.minuteOfDay % 60,
                                    meal.mealType.label,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                meal.title ?: "这一顿",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = onOpenDay,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 10.dp),
            ) {
                Text("查看这一天")
            }
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
