package com.mealory.app.ui.deleted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun RecentlyDeletedScreen(
    onBack: () -> Unit,
    viewModel: RecentlyDeletedViewModel = viewModel(),
) {
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    var pendingPermanentDelete by remember { mutableStateOf<MealWithDetails?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Column {
                    Text("最近删除", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("最多保留 30 天，之后会自动永久删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (meals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "这里是空的。删除的饭会暂时出现在这里。",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(meals, key = { it.meal.id }) { meal ->
                DeletedMealCard(
                    meal = meal,
                    onRestore = { viewModel.restore(meal.meal.id) },
                    onPermanentlyDelete = { pendingPermanentDelete = meal },
                )
            }
        }
    }

    pendingPermanentDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("永久删除这顿饭？") },
            text = { Text("记录和照片都会从设备中删除，之后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPermanentDelete = null
                        viewModel.permanentlyDelete(meal.meal.id)
                    },
                ) { Text("永久删除") }
            },
            dismissButton = { TextButton(onClick = { pendingPermanentDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun DeletedMealCard(
    meal: MealWithDetails,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    val context = LocalContext.current
    val cover = meal.photos.firstOrNull { it.id == meal.meal.coverPhotoId } ?: meal.photos.firstOrNull()
    val date = LocalDate.ofEpochDay(meal.meal.dateEpochDay)
        .format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
    val remainingDays = remainingRetentionDays(meal.meal.deletedAt ?: 0, System.currentTimeMillis())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (cover != null) {
            AsyncImage(
                model = File(context.filesDir, cover.relativePath),
                contentDescription = meal.meal.title ?: "已删除的饮食照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(meal.meal.title ?: "$date 的一顿饭", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (remainingDays == 0L) "即将自动永久删除" else "还可恢复 $remainingDays 天",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPermanentlyDelete) { Text("永久删除") }
                TextButton(onClick = onRestore) { Text("恢复") }
            }
        }
    }
}
