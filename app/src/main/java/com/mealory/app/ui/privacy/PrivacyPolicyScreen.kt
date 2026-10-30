package com.mealory.app.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mealory.app.BuildConfig

const val CurrentPrivacyPolicyVersion = 1

private data class PolicySection(
    val title: String,
    val paragraphs: List<String>,
)

private val policySections = listOf(
    PolicySection(
        title = "一、适用范围",
        paragraphs = listOf(
            "本政策适用于 Mealory Android 应用。Mealory 是一款离线饮食记录工具，不提供账号、社交、广告、支付或云同步服务。",
            "运营者：${BuildConfig.DEVELOPER_NAME}",
            "版本生效日期：2026年10月30日。",
        ),
    ),
    PolicySection(
        title = "二、我们如何处理数据",
        paragraphs = listOf(
            "你主动记录的餐次、日期、时间、饭名、备注、人物和地点标签，仅在本机用于展示、搜索、日历、统计和回忆功能。",
            "你拍摄或选择的照片会被复制到 Mealory 的应用私有目录。应用可在你确认前读取所选照片中的拍摄日期、时间和方向信息，以便预填记录时间；这些信息不会上传。",
            "Mealory 不建立用户账号，不向开发者服务器上传数据，不出售数据，也不与第三方共享你的记录和照片。",
        ),
    ),
    PolicySection(
        title = "三、系统能力与权限",
        paragraphs = listOf(
            "拍照时，Mealory 通过 Android 系统相机完成拍摄，不申请相机权限。",
            "从相册选择照片时，Mealory 使用 Android 系统 Photo Picker，仅能读取你主动选择的照片，不申请读取全部相册的权限。",
            "导出或恢复备份时，Mealory 使用 Android 系统文件选择器，仅访问你主动选择的目标文件，不申请广泛存储权限。",
            "Mealory 不申请网络、定位、通讯录、麦克风、短信、通话记录或广告标识符权限。",
        ),
    ),
    PolicySection(
        title = "四、存储、备份与删除",
        paragraphs = listOf(
            "记录和照片默认保存在应用私有目录。卸载应用或在系统设置中清除应用数据，会删除尚未另行导出的本地数据。",
            "你可以在应用中编辑、导出、删除或恢复记录。普通删除的记录会在最近删除中保留最多 30 天；你也可以选择永久删除。",
            "完整备份由你主动导出到所选位置。备份文件由你自行保管，恢复操作会在确认后替换当前 Mealory 数据。",
        ),
    ),
    PolicySection(
        title = "五、第三方服务",
        paragraphs = listOf(
            "Mealory 当前不接入广告、统计、推送、账号登录、在线支付或云存储 SDK。应用使用的开源组件仅在设备本地支持界面、数据库、图片处理和后台清理。",
        ),
    ),
    PolicySection(
        title = "六、你的权利",
        paragraphs = listOf(
            "你可以随时查看、更正、删除和导出自己的数据，也可以停止使用并卸载 Mealory。由于数据不上传到开发者服务器，我们无法远程读取、恢复或删除你设备中的记录。",
        ),
    ),
    PolicySection(
        title = "七、未成年人保护",
        paragraphs = listOf(
            "Mealory 不面向未成年人提供定向内容或营销，也不会主动识别年龄。未满 14 周岁的用户应在监护人阅读并同意本政策后使用。",
        ),
    ),
    PolicySection(
        title = "八、政策更新与联系",
        paragraphs = listOf(
            "如果数据处理方式发生实质变化，我们会更新本政策并再次请求你的确认。",
            "如对隐私或应用使用有疑问，请联系：${BuildConfig.SUPPORT_EMAIL}",
        ),
    ),
)

@Composable
fun PrivacyConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Text(
            text = "隐私保护说明",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "请阅读后选择是否继续使用 Mealory",
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PolicyContent(modifier = Modifier.weight(1f))
        Text(
            text = "点击“同意并继续”表示你已阅读并同意本隐私政策。",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                Text("不同意并退出")
            }
            Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                Text("同意并继续")
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 8.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "Mealory 隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (BuildConfig.PRIVACY_POLICY_URL.startsWith("https://") &&
                !BuildConfig.PRIVACY_POLICY_URL.contains("example.invalid")
            ) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                        )
                    },
                ) {
                    Text("在线版本")
                }
            }
        }
        PolicyContent(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PolicyContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Mealory 尊重并保护你的隐私。应用不建立账号、不连接开发者服务器，记录和照片默认只保存在你的设备中。",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(policySections) { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                section.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
