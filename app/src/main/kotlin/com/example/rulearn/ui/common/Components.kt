package com.example.rulearn.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * [WordArt] 内的文字应该用的颜色：和当前背景同属一个 M3 色对，
 * 保证任何主题（含动态取色）下都有足够对比度，不要写死白色。
 */
val LocalWordArtContentColor = staticCompositionLocalOf { Color.Unspecified }

/**
 * 单词配图：根据词条内容在主题的容器色里挑一组做柔和渐变。
 *
 * 同一个词每次都是同一张「图」，离线可用；但取色只从 Material You 的
 * primaryContainer / secondaryContainer / tertiaryContainer 里选，
 * 所以整体观感会跟着壁纸的主题色走，不再是固定的彩虹色。
 */
@Composable
fun WordArt(
    seed: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val (cols, onColor) = paletteFor(seed)
    CompositionLocalProvider(LocalWordArtContentColor provides onColor) {
        Box(
            modifier.background(
                Brush.linearGradient(
                    colors = cols,
                    start = Offset(0f, 0f),
                    end = Offset(900f, 900f)
                )
            )
        ) {
            content()
        }
    }
}

/** 按 seed 稳定地挑一组容器色，以及与之配对的前景色。 */
@Composable
private fun paletteFor(seed: String): Pair<List<Color>, Color> {
    val s = seed.fold(0) { acc, c -> acc * 31 + c.code }
    val c = MaterialTheme.colorScheme
    return when ((s ushr 2) % 3) {
        0 -> listOf(c.primaryContainer, c.secondaryContainer, c.tertiaryContainer) to c.onPrimaryContainer
        1 -> listOf(c.secondaryContainer, c.tertiaryContainer, c.primaryContainer) to c.onSecondaryContainer
        else -> listOf(c.tertiaryContainer, c.primaryContainer, c.secondaryContainer) to c.onTertiaryContainer
    }
}

/** 学习进度环。 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    label: @Composable (BoxScope.() -> Unit)? = null
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val fill = Brush.sweepGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary
        )
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                brush = fill,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        if (label != null) label()
    }
}

/** 数据小卡。 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** 分区标题。 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) action()
    }
}

/** 空状态提示。 */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
