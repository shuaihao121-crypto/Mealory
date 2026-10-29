package com.mealory.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mealory.app.ui.components.MealPhotoImage
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.MealWithDetails
import com.mealory.app.data.repository.MealInputLimits
import com.mealory.app.ui.components.MealoryLogo
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMeal: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhotos: () -> Unit,
    onNoPhoto: () -> Unit,
    onMealClick: (String) -> Unit,
    showAddMealSheet: Boolean,
    onDismissAddMealSheet: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val meals = viewModel.meals.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
    ) {
        Text(
            text = "Mealory",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it.take(MealInputLimits.SEARCH) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("搜索饭名、备注、地点或人物") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        when {
            meals.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("正在翻开回忆…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            meals.loadState.refresh is LoadState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = meals::retry) { Text("没有加载成功，点这里重试") }
                }
            }
            meals.itemCount == 0 -> EmptyHome(query.isNotBlank(), onAddMeal)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(meals.itemCount, key = { index -> meals.peek(index)?.meal?.id ?: index }) { index ->
                        val item = meals[index] ?: return@items
                        val previousDate = if (index > 0) meals.peek(index - 1)?.meal?.dateEpochDay else null
                        if (previousDate != item.meal.dateEpochDay) DateHeader(item.meal.dateEpochDay)
                        MealCard(item = item, onClick = { onMealClick(item.meal.id) })
                    }
                }
            }
        }
    }

    if (showAddMealSheet) {
        ModalBottomSheet(onDismissRequest = onDismissAddMealSheet) {
            Text(
                text = "记录一顿饭",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            MealEntryOption(Icons.Rounded.AddAPhoto, "拍照") {
                onDismissAddMealSheet()
                onTakePhoto()
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            MealEntryOption(Icons.Rounded.Image, "从相册选择") {
                onDismissAddMealSheet()
                onPickPhotos()
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            MealEntryOption(Icons.Rounded.NoPhotography, "无照片补记") {
                onDismissAddMealSheet()
                onNoPhoto()
            }
            TextButton(
                onClick = onDismissAddMealSheet,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 14.dp),
            ) { Text("取消") }
        }
    }
}

@Composable
private fun EmptyHome(searching: Boolean, onAddMeal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!searching) MealoryLogo(size = 104.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (searching) "没有找到这段回忆" else "从这一顿开始",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (searching) "换个饭名、地点或人物试试。" else "一张照片，一点时间。\n未来会替你记得今天。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!searching) {
            Spacer(Modifier.height(28.dp))
            Button(onClick = onAddMeal) { Text("记录第一顿饭") }
        }
    }
}

@Composable
private fun DateHeader(epochDay: Long) {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    val suffix = when (date) {
        today -> " · 今天"
        today.minusDays(1) -> " · 昨天"
        else -> ""
    }
    Text(
        text = date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)) + suffix,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun MealCard(item: MealWithDetails, onClick: () -> Unit) {
    val context = LocalContext.current
    val cover = item.photos.firstOrNull { it.id == item.meal.coverPhotoId } ?: item.photos.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "打开这顿饭" }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
    ) {
        if (cover != null) {
            MealPhotoImage(
                model = File(context.filesDir, cover.relativePath),
                contentDescription = item.meal.title ?: "这一顿的照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
            )
        }
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "${item.meal.mealType.label} · %02d:%02d".format(
                    item.meal.minuteOfDay / 60,
                    item.meal.minuteOfDay % 60,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            item.meal.title?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            item.place?.let { Text("📍 ${it.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (item.people.isNotEmpty()) {
                Text("👥 ${item.people.joinToString("、") { it.name }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MealEntryOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
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
