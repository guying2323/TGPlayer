package com.tgplayer.android.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgplayer.android.data.Settings
import com.tgplayer.android.ui.common.pressScale
import com.tgplayer.android.ui.theme.AppIcons
import kotlinx.coroutines.launch

/**
 * First-launch guide, modelled on TMPlayer's setup: one idea per page, a
 * recommended answer, and the practical switches last so the guide leaves the
 * app configured rather than just read.
 */
@Composable
fun OnboardingScreen(settings: Settings, onFinish: () -> Unit) {
    val pages = listOf(0, 1, 2, 3)
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var wifiOnly by remember { mutableStateOf(settings.wifiOnlyDownloads) }
    var saveData by remember { mutableStateOf(settings.saveData) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TGPlayer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                settings.onboardingDone = true
                onFinish()
            }) { Text("跳过") }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) { index ->
            when (index) {
                0 -> GuidePage(
                    icon = AppIcons.ScreenPlay,
                    title = "你的 Telegram 视频，\n边下边播",
                    body = "浏览自己聊天和频道里的视频，起播不必等整段下载完。非官方客户端，消息与账号都只留在本机。",
                )
                1 -> GuidePage(
                    icon = AppIcons.Lock,
                    title = "用你自己的账号登录",
                    body = "支持扫码或手机号 + 验证码，也可以加多个账号。TDLib 会话落在应用私有目录，不上传任何服务器。",
                )
                2 -> GuidePage(
                    icon = AppIcons.ListVideo,
                    title = "像影视 App 一样浏览",
                    body = "频道/群组/私聊筛选、最近与名称排序、#标签筛选、聊天内搜索；视频播完自动接下一个，也能开画中画。",
                )
                else -> GuideSettingsPage(
                    wifiOnly = wifiOnly,
                    onWifiOnly = { wifiOnly = it; settings.wifiOnlyDownloads = it },
                    saveData = saveData,
                    onSaveData = { saveData = it; settings.saveData = it },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pager.currentPage == index) 18.dp else 7.dp, 7.dp)
                        .background(
                            if (pager.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }

        val last = pager.currentPage == pages.lastIndex
        Button(
            onClick = {
                if (last) {
                    settings.onboardingDone = true
                    onFinish()
                } else {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text(if (last) "开始使用" else "下一步", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GuidePage(icon: ImageVector, title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun GuideSettingsPage(
    wifiOnly: Boolean,
    onWifiOnly: (Boolean) -> Unit,
    saveData: Boolean,
    onSaveData: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("先定好下载与省流", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                GuideSwitch("仅 Wi-Fi 下载", "蜂窝数据下等待网络", wifiOnly, onWifiOnly)
                GuideSwitch("节省流量", "按较低优先级下载与播放", saveData, onSaveData)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "之后都可以在 设置 里修改",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun GuideSwitch(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(12.dp))
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
