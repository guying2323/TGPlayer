package com.tgplayer.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgplayer.android.data.Downloads
import com.tgplayer.android.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(pagePadding: Dp) {
    val context = LocalContext.current
    val tasks by Downloads.tasks.collectAsStateWithLifecycle()
    val active = tasks.filter { it.state != Downloads.State.Done }
    val finished = tasks.filter { it.state == Downloads.State.Done }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("下载", style = MaterialTheme.typography.titleLarge) },
            actions = {
                if (tasks.isNotEmpty()) {
                    IconButton(onClick = { Downloads.clearFinished() }) {
                        Icon(AppIcons.Minus, contentDescription = "清理已完成", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
            Text(
                "已占用 " + formatSize(Downloads.usedBytes()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (tasks.isEmpty()) {
                Text("还没有下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
                Text(
                    "在视频卡片上点下载按钮即可离线保存",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (active.isNotEmpty()) {
                        item(key = "active-header") {
                            Text(
                                "进行中",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(active, key = { it.fileId }, contentType = { "download" }) { task -> DownloadRow(task, context) }
                    }
                    if (finished.isNotEmpty()) {
                        item(key = "done-header") {
                            Text(
                                "已完成",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                        items(finished, key = { it.fileId }, contentType = { "download" }) { task -> DownloadRow(task, context) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(task: Downloads.Task, context: android.content.Context) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable {
                if (task.state == Downloads.State.Done) {
                    Downloads.open(context, task)?.let { context.startActivity(it) }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                when (task.state) {
                    Downloads.State.Queued -> "等待中（仅 Wi-Fi 时开始）"
                    Downloads.State.Downloading -> if (task.speedBytesPerSec > 0) {
                        "下载中 " + (task.progress * 100).toInt() + "% · " + formatSize(task.speedBytesPerSec) + "/s"
                    } else {
                        "下载中 " + (task.progress * 100).toInt() + "%"
                    }
                    Downloads.State.Paused -> "已暂停"
                    Downloads.State.Done -> formatSize(task.totalBytes)
                    Downloads.State.Failed -> task.error ?: "失败"
                },
                color = if (task.state == Downloads.State.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(99.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(task.progress)
                        .height(3.dp)
                        .background(
                            if (task.state == Downloads.State.Done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(99.dp),
                        ),
                )
            }
        }
        when (task.state) {
            Downloads.State.Downloading -> IconButton(onClick = { Downloads.pause(task) }) {
                Icon(AppIcons.Pause, contentDescription = "暂停", tint = MaterialTheme.colorScheme.onSurface)
            }
            Downloads.State.Paused, Downloads.State.Failed -> IconButton(onClick = { Downloads.resume(task) }) {
                Icon(AppIcons.Play, contentDescription = "继续", tint = MaterialTheme.colorScheme.onSurface)
            }
            Downloads.State.Done -> IconButton(onClick = {
                Downloads.open(context, task)?.let { context.startActivity(it) }
            }) {
                Icon(AppIcons.Play, contentDescription = "播放", tint = MaterialTheme.colorScheme.primary)
            }
            Downloads.State.Queued -> Box(Modifier.size(40.dp))
        }
        IconButton(onClick = { Downloads.remove(task) }) {
            Icon(AppIcons.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
