package com.tgplayer.android.ui.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/** One loading contract for every screen. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Failed(val message: String) : UiState<Nothing>
}

/**
 * Runs [load] whenever [keys] change and exposes a three-state result plus a
 * retry action. Keeps every screen's loading/error handling identical.
 */
@Composable
fun <T> rememberLoadable(vararg keys: Any?, load: suspend () -> T): Pair<UiState<T>, () -> Unit> {
    var state by remember { mutableStateOf<UiState<T>>(UiState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(*keys, retry) {
        state = UiState.Loading
        state = try {
            UiState.Ready(load())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            UiState.Failed(t.message?.takeIf { it.isNotBlank() } ?: "加载失败")
        }
    }
    return state to { retry++ }
}

@Composable
fun LoadingView(message: String = "加载中…", modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp)
        Spacer(Modifier.height(10.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
fun EmptyView(title: String, hint: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (hint != null) {
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        action?.invoke()
    }
}

/** Skeleton rows for list screens; the shared shimmer keeps the look uniform. */
@Composable
fun SkeletonList(rows: Int = 6, avatar: Boolean = true) {
    Column(Modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(if (avatar) 46.dp else 42.dp)
                        .shimmer(if (avatar) androidx.compose.foundation.shape.RoundedCornerShape(15.dp) else CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(12.dp).shimmer(CircleShape))
                    Box(Modifier.fillMaxWidth(0.35f).height(9.dp).shimmer(CircleShape))
                }
            }
        }
    }
}
