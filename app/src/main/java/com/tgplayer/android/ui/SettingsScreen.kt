package com.tgplayer.android.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgplayer.android.BuildConfig
import com.tgplayer.android.data.Downloads
import com.tgplayer.android.data.Links
import com.tgplayer.android.data.Settings
import com.tgplayer.android.data.Td
import com.tgplayer.android.ui.common.Thumbnails
import com.tgplayer.android.ui.theme.AppIcons
import com.tgplayer.android.ui.theme.ThemeController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(pagePadding: Dp, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val settings = remember { Settings.get() }
    val scope = rememberCoroutineScope()

    var saveData by remember { mutableStateOf(settings.saveData) }
    var autoplay by remember { mutableStateOf(settings.autoplay) }
    var rememberPosition by remember { mutableStateOf(settings.rememberPosition) }
    var wifiOnly by remember { mutableStateOf(settings.wifiOnlyDownloads) }
    var hdThumbs by remember { mutableStateOf(settings.hdThumbnails) }
    var speed by remember { mutableStateOf(settings.defaultSpeed) }
    var speedDialog by remember { mutableStateOf(false) }
    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    var usedSpace by remember { mutableStateOf(Downloads.usedBytes()) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var dynamicColor by remember { mutableStateOf(settings.dynamicColor) }
    var themeDialog by remember { mutableStateOf(false) }
    var removeAccountId by remember { mutableStateOf<String?>(null) }

    val lastError by Td.lastError.collectAsStateWithLifecycle()
    val me by Td.me.collectAsStateWithLifecycle()
    val accounts = remember { Td.accounts() }
    var diagnostic by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.Back, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = { Text("设置", style = MaterialTheme.typography.titleLarge) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding)
                .padding(bottom = 28.dp),
        ) {
            SectionCard("播放") {
                SwitchRow(AppIcons.Play, "自动续播", autoplay) { autoplay = it; settings.autoplay = it }
                SwitchRow(AppIcons.Clock, "记住播放进度", rememberPosition) { rememberPosition = it; settings.rememberPosition = it }
                SettingRow(AppIcons.Gauge, "默认倍速", speedLabel(speed), onClick = { speedDialog = true })
                SwitchRow(AppIcons.Offline, "节省流量", saveData) { saveData = it; settings.saveData = it }
            }

            SectionCard("下载") {
                SwitchRow(AppIcons.Download, "仅 Wi-Fi 下载", wifiOnly) { wifiOnly = it; settings.wifiOnlyDownloads = it }
                SettingRow(AppIcons.Folder, "已用空间", formatSize(usedSpace))
                SettingRow(AppIcons.Refresh, "后台下载保护", "忽略电池优化", onClick = {
                    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (power?.isIgnoringBatteryOptimizations(context.packageName) == true) {
                        Toast.makeText(context, "已开启后台下载保护", Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + context.packageName),
                                ),
                            )
                        }.onFailure {
                            runCatching {
                                context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }
                    }
                })
                SettingRow(AppIcons.Minus, "清空已完成任务", null, onClick = {
                    Downloads.clearFinished()
                    usedSpace = Downloads.usedBytes()
                })
            }

            SectionCard("显示") {
                SettingRow(AppIcons.Fullscreen, "主题", themeLabel(themeMode), onClick = { themeDialog = true })
                SwitchRow(AppIcons.Sliders, "动态取色（Android 12+）", dynamicColor) {
                    dynamicColor = it
                    ThemeController.setDynamic(it)
                }
                SwitchRow(AppIcons.Tv, "高清封面（720px）", hdThumbs) { hdThumbs = it; settings.hdThumbnails = it }
                SettingRow(AppIcons.Refresh, "清除封面缓存", null, onClick = { Thumbnails.clearDisk(context) })
            }

            SectionCard("账号") {
                accounts.forEach { account ->
                    val current = account.id == Td.currentAccountId()
                    SettingRow(
                        AppIcons.User,
                        account.name + if (current) "（当前）" else "",
                        value = if (current) (me ?: "已登录") else null,
                        onClick = if (!current) ({ Td.switchAccount(account.id) }) else null,
                    )
                }
                SettingRow(AppIcons.Plus, "添加账号", "登录另一个 Telegram 账号", onClick = { Td.addAccount() })
                if (accounts.size > 1) {
                    SettingRow(
                        AppIcons.Minus,
                        "移除当前账号",
                        null,
                        danger = true,
                        onClick = { removeAccountId = Td.currentAccountId() },
                    )
                }
                SettingRow(AppIcons.LogOut, "退出登录", null, danger = true, onClick = { scope.launch { Td.logOut() } })
            }

            SectionCard("诊断") {
                SettingRow(
                    AppIcons.Sliders,
                    if (running) "运行诊断…" else "运行诊断（TDLib 原始返回）",
                    null,
                    onClick = {
                        if (!running) scope.launch {
                            running = true
                            diagnostic = Td.diagnose()
                            running = false
                        }
                    },
                )
                lastError?.let { SettingRow(AppIcons.Close, "最近错误", it) }
            }

            SectionCard("关于") {
                SettingRow(AppIcons.Check, "版本", BuildConfig.VERSION_NAME)
                SettingRow(AppIcons.Globe, "官网", "guying2323.github.io/TGPlayer", onClick = {
                    openUrl(Links.WEBSITE)
                })
                SettingRow(AppIcons.Language, "开源代码", "github.com/guying2323/TGPlayer", onClick = {
                    openUrl(Links.GITHUB)
                })
                SettingRow(AppIcons.Inbox, "开源许可", "GPL-3.0-or-later")
            }

            SectionCard("作者") {
                SettingRow(AppIcons.Star, "作者名称", "孤影")
                SettingRow(AppIcons.Chat, "官方交流群", Links.QQ_GROUP_NAME + " · " + Links.QQ_GROUP_NUMBER, onClick = {
                    openUrl(Links.QQ_GROUP_URL)
                })
                SettingRow(AppIcons.Globe, "官网", "guying2323.github.io/TGPlayer", onClick = {
                    openUrl(Links.WEBSITE)
                })
                SettingRow(AppIcons.Language, "开源代码地址", "github.com/guying2323/TGPlayer", onClick = {
                    openUrl(Links.GITHUB)
                })
            }
        }
    }

    if (speedDialog) {
        AlertDialog(
            onDismissRequest = { speedDialog = false },
            title = { Text("默认倍速") },
            text = {
                Column {
                    listOf(1f, 1.25f, 1.5f, 2f).forEach { option ->
                        Text(
                            speedLabel(option),
                            color = if (option == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    speed = option
                                    settings.defaultSpeed = option
                                    speedDialog = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { speedDialog = false }) { Text("关闭") } },
        )
    }

    removeAccountId?.let { id ->
        AlertDialog(
            onDismissRequest = { removeAccountId = null },
            title = { Text("移除账号") },
            text = { Text("移除当前账号的槽位并退出登录；本地 TDLib 数据会保留，但不再出现在账号列表里。") },
            confirmButton = {
                TextButton(onClick = {
                    removeAccountId = null
                    Td.removeAccount(id)
                }) { Text("移除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removeAccountId = null }) { Text("取消") }
            },
        )
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("主题") },
            text = {
                Column {
                    listOf(0 to "跟随系统", 1 to "深色", 2 to "浅色").forEach { (value, label) ->
                        Text(
                            (if (themeMode == value) "✓ " else "   ") + label,
                            color = if (themeMode == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = value
                                    ThemeController.setMode(value)
                                    themeDialog = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("关闭") } },
        )
    }

    diagnostic?.let { report ->
        AlertDialog(
            onDismissRequest = { diagnostic = null },
            title = { Text("诊断") },
            text = {
                Text(
                    report,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { diagnostic = null }) { Text("关闭") } },
        )
    }
}

private fun themeLabel(mode: Int): String = when (mode) {
    1 -> "深色"
    2 -> "浅色"
    else -> "跟随系统"
}

private fun speedLabel(value: Float): String = when {
    value == 1f -> "1.0×"
    value == 1.25f -> "1.25×"
    value == 1.5f -> "1.5×"
    value == 2f -> "2.0×"
    else -> value.toString() + "×"
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(icon, title, value = null) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String?,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        when {
            trailing != null -> trailing()
            value != null -> Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
