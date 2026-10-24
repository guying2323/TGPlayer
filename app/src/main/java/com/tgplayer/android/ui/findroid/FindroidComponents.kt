package com.tgplayer.android.ui.findroid

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.ui.common.Thumbnail
import com.tgplayer.android.ui.common.pressScale
import com.tgplayer.android.ui.formatDateShort
import com.tgplayer.android.ui.formatDuration
import com.tgplayer.android.ui.resolutionLabel
import com.tgplayer.android.ui.theme.AppIcons

/**
 * Components adapted from Findroid's presentation/film/components
 * (ItemCard, ItemPoster, ProgressBar, HomeSection; GPL-3.0,
 * https://github.com/jarnedemeulemeester/findroid).
 *
 * The model is this app's MediaItem and artwork comes from the Telegram
 * thumbnail cache instead of Coil; the card geometry (260/150 dp, 1.77f poster,
 * 4dp progress bar, 42dp section header) follows the original.
 */
object FindroidSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val default = 12.dp
    val medium = 16.dp
    val large = 24.dp
}

enum class Direction { HORIZONTAL, VERTICAL }

@Composable
fun ItemPoster(
    thumbnailFileId: Int,
    direction: Direction,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    Thumbnail(
        thumbnailFileId,
        modifier
            .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        seed = seed,
    )
}

@Composable
fun FindroidProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    width: Dp? = null,
) {
    val safe = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .height(4.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.primary)
            .then(if (width != null) Modifier.width((width - 16.dp) * safe) else Modifier.fillMaxWidth(safe)),
    )
}

@Composable
fun FindroidItemCard(
    item: MediaItem,
    direction: Direction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = if (direction == Direction.HORIZONTAL) 260.dp else 150.dp,
    progressFraction: Float = 0f,
    onDownload: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
) {
    val meta = listOf(
        resolutionLabel(item.width, item.height),
        formatDateShort(item.date),
    ).filter { it.isNotBlank() }.joinToString(" · ")

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clip(MaterialTheme.shapes.small),
    ) {
        Surface(shape = MaterialTheme.shapes.small) {
            Box(
                Modifier
                    .pressScale(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
            ) {
                ItemPoster(item.thumbnailFileId, direction, item.fileId)
                if (item.durationSec > 0) {
                    Text(
                        formatDuration(item.durationSec),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(FindroidSpacing.small)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                if (direction == Direction.HORIZONTAL) {
                    FindroidProgressBar(
                        progressFraction,
                        modifier = Modifier.align(Alignment.BottomStart).padding(FindroidSpacing.small),
                    )
                }
            }
        }
        Spacer(Modifier.height(FindroidSpacing.extraSmall))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (meta.isBlank()) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onClick),
        )
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onDownload != null) {
                TextButton(
                    onClick = onDownload,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(32.dp).minimumInteractiveComponentSize(),
                ) {
                    Icon(AppIcons.Download, contentDescription = "下载", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(" 下载", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (onFavorite != null) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(34.dp).minimumInteractiveComponentSize()) {
                    Icon(
                        AppIcons.Star,
                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FindroidRecentCard(
    thumbnailFileId: Int,
    seed: Int,
    title: String,
    subtitle: String,
    progressFraction: Float,
    width: Dp = 260.dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(width)
            .pressScale(interactionSource)
            .clip(MaterialTheme.shapes.small)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
    ) {
        Surface(shape = MaterialTheme.shapes.small) {
            Box {
                ItemPoster(thumbnailFileId, Direction.HORIZONTAL, seed)
                FindroidProgressBar(
                    progressFraction,
                    modifier = Modifier.align(Alignment.BottomStart).padding(FindroidSpacing.small),
                )
            }
        }
        Spacer(Modifier.height(FindroidSpacing.extraSmall))
        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
fun HomeSection(
    title: String,
    itemsPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(42.dp)) {
            Text(
                title,
                modifier = Modifier.align(Alignment.CenterStart).padding(itemsPadding),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(FindroidSpacing.extraSmall))
        LazyRow(
            contentPadding = itemsPadding,
            horizontalArrangement = Arrangement.spacedBy(FindroidSpacing.default),
            content = content,
        )
    }
}
