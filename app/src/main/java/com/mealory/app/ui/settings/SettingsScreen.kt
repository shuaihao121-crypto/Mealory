package com.mealory.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mealory.app.BuildConfig
import com.mealory.app.data.preferences.PhotoQuality
import com.mealory.app.data.preferences.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                    Text("设置", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("保持简单，也保持可控。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SettingSection("外观") {
                Text("主题跟随", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }
        item {
            SettingSection("照片质量") {
                PhotoQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = state.settings.photoQuality == quality,
                        onClick = { viewModel.setPhotoQuality(quality) },
                        label = { Text(quality.label) },
                    )
                    Text(quality.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SettingSection("照片与存储") {
                Text(
                    "${state.storage.photoCount} 张照片 · ${formatBytes(state.storage.totalBytes)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("照片保存在 App 私有目录，其他应用不能直接读取。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingSection("最近备份") {
                val backupText = state.settings.lastBackupAt?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA))
                } ?: "尚未导出完整备份"
                Text(backupText, style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            SettingSection("隐私") {
                Text("Mealory 不申请网络、定位、通讯录或全相册读取权限。")
                Text("所有记录默认只保存在这台设备中。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingSection("关于 Mealory") {
                Text("Mealory ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text("MEALS TODAY, MEMORIES TOMORROW", color = MaterialTheme.colorScheme.tertiary)
                Text("一款私人、安静、长期的饮食回忆日记。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }

private val PhotoQuality.label: String
    get() = when (this) {
        PhotoQuality.STANDARD -> "标准"
        PhotoQuality.HIGH -> "高质量"
    }

private val PhotoQuality.description: String
    get() = when (this) {
        PhotoQuality.STANDARD -> "最长边约 2560px，适合大多数记录。"
        PhotoQuality.HIGH -> "最长边约 4096px，占用更多设备空间。"
    }

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
