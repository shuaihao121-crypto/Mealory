package com.mealory.app.ui.mealdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mealory.app.data.database.entity.MealType
import com.mealory.app.data.database.entity.PersonEntity
import com.mealory.app.data.database.entity.PlaceEntity
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: MealDetailViewModel = viewModel(),
) {
    val details by viewModel.meal.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val availablePeople by viewModel.people.collectAsStateWithLifecycle()
    val availablePlaces by viewModel.places.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("这一顿") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "完成" else "编辑") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除")
                    }
                },
            )
        },
    ) { padding ->
        val meal = details
        if (meal == null) {
            Text("正在打开…", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            DetailContent(
                details = meal,
                editing = editing,
                availablePeople = availablePeople,
                availablePlaces = availablePlaces,
                onSave = { date, minute, type, title, note, place, people ->
                    viewModel.saveEdits(date, minute, type, title, note, place, people)
                    editing = false
                },
                onSetCover = viewModel::setCover,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("移到最近删除？") },
            text = { Text("这顿饭会保留 30 天，期间可以恢复。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteMeal() }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun DetailContent(
    details: com.mealory.app.data.database.entity.MealWithDetails,
    editing: Boolean,
    availablePeople: List<PersonEntity>,
    availablePlaces: List<PlaceEntity>,
    onSave: (LocalDate, Int, MealType, String, String, String, String) -> Unit,
    onSetCover: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val meal = details.meal
    var dateText by remember(meal.id, meal.updatedAt) { mutableStateOf(LocalDate.ofEpochDay(meal.dateEpochDay).toString()) }
    var timeText by remember(meal.id, meal.updatedAt) {
        mutableStateOf("%02d:%02d".format(meal.minuteOfDay / 60, meal.minuteOfDay % 60))
    }
    var type by remember(meal.id, meal.updatedAt) { mutableStateOf(meal.mealType) }
    var title by remember(meal.id, meal.updatedAt) { mutableStateOf(meal.title.orEmpty()) }
    var note by remember(meal.id, meal.updatedAt) { mutableStateOf(meal.note.orEmpty()) }
    var placeName by remember(meal.id, meal.updatedAt) { mutableStateOf(details.place?.name.orEmpty()) }
    var peopleNames by remember(meal.id, meal.updatedAt) {
        mutableStateOf(details.people.joinToString("，") { it.name })
    }
    val pagerState = rememberPagerState(pageCount = { details.photos.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (details.photos.isNotEmpty()) {
            HorizontalPager(state = pagerState) { page ->
                val photo = details.photos[page]
                Column {
                    AsyncImage(
                        model = File(context.filesDir, photo.relativePath),
                        contentDescription = "照片 ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                    )
                    if (photo.id != meal.coverPhotoId) {
                        TextButton(onClick = { onSetCover(photo.id) }) { Text("设为封面") }
                    } else {
                        Text("封面", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        if (editing) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MealType.entries) { candidate ->
                    FilterChip(
                        selected = type == candidate,
                        onClick = { type = candidate },
                        label = { Text(candidate.label) },
                    )
                }
            }
            Row {
                OutlinedTextField(dateText, { dateText = it }, label = { Text("日期") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(timeText, { timeText = it }, label = { Text("时间") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(title, { title = it }, label = { Text("吃了什么") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(placeName, { placeName = it }, label = { Text("地点") }, modifier = Modifier.fillMaxWidth())
            if (availablePlaces.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availablePlaces) { place ->
                        FilterChip(
                            selected = placeName.equals(place.name, ignoreCase = true),
                            onClick = { placeName = place.name },
                            label = { Text(place.name) },
                        )
                    }
                }
            }
            OutlinedTextField(
                peopleNames,
                { peopleNames = it },
                label = { Text("和谁一起") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (availablePeople.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availablePeople) { person ->
                        val selected = peopleNames.split('，', ',')
                            .any { it.trim().equals(person.name, ignoreCase = true) }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val names = peopleNames.split('，', ',')
                                    .map(String::trim)
                                    .filter(String::isNotEmpty)
                                    .toMutableList()
                                val index = names.indexOfFirst { it.equals(person.name, ignoreCase = true) }
                                if (index >= 0) names.removeAt(index) else names += person.name
                                peopleNames = names.joinToString("，")
                            },
                            label = { Text(person.name) },
                        )
                    }
                }
            }
            OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            TextButton(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.ofEpochDay(meal.dateEpochDay))
                    val parts = timeText.split(':')
                    val minute = parts.getOrNull(0)?.toIntOrNull()?.times(60)
                        ?.plus(parts.getOrNull(1)?.toIntOrNull() ?: 0)
                        ?.takeIf { it in 0..1_439 }
                        ?: meal.minuteOfDay
                    onSave(date, minute, type, title, note, placeName, peopleNames)
                },
            ) { Text("保存修改") }
        } else {
            Text("${meal.mealType.label} · $dateText · $timeText", style = MaterialTheme.typography.titleMedium)
            meal.title?.let { Text(it, style = MaterialTheme.typography.headlineMedium) }
            details.place?.let { Text("📍 ${it.name}") }
            if (details.people.isNotEmpty()) Text("👥 ${details.people.joinToString("、") { it.name }}")
            meal.note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
