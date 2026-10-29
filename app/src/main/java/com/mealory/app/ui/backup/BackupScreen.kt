package com.mealory.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::export) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::prepareRestore) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Column {
                    Text("数据与备份", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("完整保存记录、人物、地点和原始照片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "卸载 Mealory 或清除应用数据前，请先导出完整备份。",
                    modifier = Modifier.padding(18.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        exportLauncher.launch("Mealory_Backup_$date.zip")
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    Text(" 导出完整备份")
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    Text(" 从备份恢复")
                }
            }
        }
        if (state.busy) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("  正在校验和处理，请稍候…")
                }
            }
        }
        state.message?.let { message ->
            item { FeedbackCard(message, false, viewModel::clearFeedback) }
        }
        state.error?.let { error ->
            item { FeedbackCard(error, true, viewModel::clearFeedback) }
        }
        item {
            Text(
                "备份文件是可解压的 ZIP，数据采用开放 JSON，照片保持为 JPG。恢复前会验证版本、关系、文件大小和 SHA-256；验证失败不会修改当前数据。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    state.pendingRestore?.let { preview ->
        val time = Instant.ofEpochMilli(preview.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA))
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("恢复这个完整备份？") },
            text = {
                Text(
                    "备份时间：$time\n包含 ${preview.counts.meals} 顿饭、${preview.counts.photos} 张照片。\n\n" +
                        "备份已经完整验证。继续后会替换当前设备中的 Mealory 数据。",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("取消") } },
        )
    }
}

@Composable
private fun FeedbackCard(message: String, error: Boolean, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    }
}
