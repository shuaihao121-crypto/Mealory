package com.mealory.app.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mealory.app.data.repository.MealInputLimits

data class TagSummaryUi(
    val id: String,
    val name: String,
    val mealCount: Long,
)

@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    onPersonClick: (String) -> Unit,
    viewModel: PeopleViewModel = viewModel(),
) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()
    TagListScreen(
        title = "人物",
        singularLabel = "人物",
        emptyMessage = "还没有人物标签。\n记录一顿饭时，写下同行的人就会出现在这里。",
        tags = people.map { TagSummaryUi(it.id, it.name, it.mealCount) },
        errorMessage = error,
        onBack = onBack,
        onTagClick = onPersonClick,
        onCreate = viewModel::create,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@Composable
fun PlacesScreen(
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    viewModel: PlacesViewModel = viewModel(),
) {
    val places by viewModel.places.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()
    TagListScreen(
        title = "地点",
        singularLabel = "地点",
        emptyMessage = "还没有地点标签。\nMealory 不读取定位，地点始终由你自己写下。",
        tags = places.map { TagSummaryUi(it.id, it.name, it.mealCount) },
        errorMessage = error,
        onBack = onBack,
        onTagClick = onPlaceClick,
        onCreate = viewModel::create,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagListScreen(
    title: String,
    singularLabel: String,
    emptyMessage: String,
    tags: List<TagSummaryUi>,
    errorMessage: String?,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editor by remember { mutableStateOf<TagSummaryUi?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TagSummaryUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加$singularLabel")
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (errorMessage != null) {
                item { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            }
            if (tags.isEmpty()) {
                item {
                    Text(
                        emptyMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 36.dp),
                    )
                }
            } else {
                items(tags, key = TagSummaryUi::id) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagClick(tag.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tag.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${tag.mealCount} 顿", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editor = tag }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "编辑${tag.name}")
                            }
                            IconButton(onClick = { pendingDelete = tag }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除${tag.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding || editor != null) {
        NameEditorDialog(
            title = if (adding) "添加$singularLabel" else "编辑$singularLabel",
            initialValue = editor?.name.orEmpty(),
            onDismiss = { adding = false; editor = null },
            onConfirm = { name ->
                editor?.let { onRename(it.id, name) } ?: onCreate(name)
                adding = false
                editor = null
            },
        )
    }

    pendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除“${tag.name}”？") },
            text = {
                Text(
                    if (singularLabel == "人物") {
                        "只会删除人物标签和关联，不会删除任何一顿饭。"
                    } else {
                        "只会删除地点标签，相关 Meal 会保留并清空地点。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(tag.id); pendingDelete = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun NameEditorDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(MealInputLimits.TAG_NAME) },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
