package com.mealory.app.ui.addmeal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddMealViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var initialActionLaunched by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
        viewModel::onPhotosPicked,
    )
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
        viewModel::onCameraResult,
    )

    LaunchedEffect(viewModel.initialMode, initialActionLaunched) {
        if (initialActionLaunched) return@LaunchedEffect
        initialActionLaunched = true
        when (viewModel.initialMode) {
            AddMealMode.CAMERA -> camera.launch(viewModel.createCameraCapture())
            AddMealMode.PICKER -> photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
            AddMealMode.NO_PHOTO -> Unit
        }
    }
    LaunchedEffect(state.savedMealId) {
        if (state.savedMealId != null) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录一顿饭") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.isSaving) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                PhotoEditor(
                    photos = state.photos,
                    coverIndex = state.coverIndex,
                    onAdd = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onRemove = viewModel::removePhoto,
                    onMove = viewModel::movePhoto,
                    onSetCover = viewModel::setCover,
                )
            }
            item {
                Text("餐次", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MealType.entries.size) { index ->
                        val type = MealType.entries[index]
                        FilterChip(
                            selected = state.mealType == type,
                            onClick = { viewModel.setMealType(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            item {
                DateAndTimeRow(
                    date = state.date,
                    minuteOfDay = state.minuteOfDay,
                    onDate = viewModel::setDate,
                    onTime = viewModel::setTime,
                )
            }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text("吃了什么") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = state.place,
                    onValueChange = viewModel::setPlace,
                    label = { Text("地点") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = state.people,
                    onValueChange = viewModel::setPeople,
                    label = { Text("和谁一起") },
                    supportingText = { Text("多人可用逗号分隔") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = { Text("一句话备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }

        if (state.isSaving) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PhotoEditor(
    photos: List<SelectedPhoto>,
    coverIndex: Int,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSetCover: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("照片", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("${photos.size}/9", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (photos.isEmpty()) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加照片（可选）")
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = "照片 ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                            IconButton(
                                onClick = { onRemove(index) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "删除照片")
                            }
                        }
                        if (coverIndex == index) {
                            AssistChip(onClick = {}, label = { Text("封面") })
                        } else {
                            TextButton(onClick = { onSetCover(index) }) { Text("设为封面") }
                        }
                        Row {
                            IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "前移")
                            }
                            IconButton(onClick = { onMove(index, 1) }, enabled = index < photos.lastIndex) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "后移")
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = onAdd, enabled = photos.size < 9) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("添加")
                    }
                }
            }
        }
    }
}

@Composable
private fun DateAndTimeRow(
    date: LocalDate,
    minuteOfDay: Int,
    onDate: (LocalDate) -> Unit,
    onTime: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth,
                ).show()
            },
            modifier = Modifier.weight(1f),
        ) { Text(date.toString()) }
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m -> onTime(h, m) }, hour, minute, true).show()
            },
            modifier = Modifier.weight(1f),
        ) { Text("%02d:%02d".format(hour, minute)) }
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
