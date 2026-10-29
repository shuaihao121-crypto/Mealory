package com.mealory.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(
    onPeople: () -> Unit,
    onPlaces: () -> Unit,
    onRecentlyDeleted: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("我的", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "人物和地点只是你自己的本地标签。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        item { ProfileStatsCard(stats) }
        item { ProfileEntry("人物", "看看和谁一起留下了这些饭", Icons.Rounded.Groups, onPeople) }
        item { ProfileEntry("地点", "看看这些饭曾在哪里发生", Icons.Rounded.Place, onPlaces) }
        item { ProfileEntry("最近删除", "30 天内可以恢复误删的饭", Icons.Rounded.DeleteSweep, onRecentlyDeleted) }
        item { ProfileEntry("数据与备份", "导出或恢复完整的 Mealory 数据", Icons.Rounded.Backup, onBackup) }
        item { ProfileEntry("照片、外观与关于", "存储、照片质量、主题和隐私", Icons.Rounded.Settings, onSettings) }
    }
}

@Composable
private fun ProfileStatsCard(stats: ProfileStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatValue(stats.mealCount.toString(), "顿饭", Modifier.weight(1f))
            StatValue(stats.recordedDayCount.toString(), "个记录日", Modifier.weight(1f))
        }
        val start = stats.firstDateEpochDay?.let {
            LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
        } ?: "还没有开始记录"
        Text("开始记录：$start", Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp))
    }
}

@Composable
private fun StatValue(value: String, label: String, modifier: Modifier) {
    androidx.compose.foundation.layout.Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileEntry(
    title: String,
    supporting: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(supporting) },
            leadingContent = { Icon(icon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
