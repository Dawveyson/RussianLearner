package com.example.rulearn.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Material 3 表面体系。
 *
 * 这一层历史上叫「玻璃」，API 名（GlassCard / GlassBox / GlassToken）保留不变，
 * 以免改动全 App 数十处调用点；但内部实现已从半透明镀膜切换为标准的
 * Material 3 tonal surface：不透明填充 + 按高度取 surfaceContainer 色阶 + 描边。
 *
 * 容器色的取法是 M3 的 tonal elevation 规则，保证同屏多张卡片有层次但不脏：
 *   0dp → surfaceContainerLowest
 *   1dp → surfaceContainerLow
 *   3dp → surfaceContainer
 *   6dp → surfaceContainerHigh
 *   8dp → surfaceContainerHighest
 */

/** 形状令牌：卡片用超大圆角，浮动条用胶囊。 */
object GlassToken {
    val CardShape: Shape = SquircleShape(28.dp)
    val BarShape: Shape = SquircleShape(32.dp)
}

/**
 * 按 M3 tonal elevation 取容器色。
 * 高度越高，表面越亮（深色下）或越暖（浅色下），这是 M3 表达层次的方式。
 */
@Composable
private fun tonalSurface(elevation: androidx.compose.ui.unit.Dp): Color =
    MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)

/**
 * 把任意节点变成一块 Material 3 表面。
 *
 * [tonalElevation] 决定填充色取自哪一级 surfaceContainer；
 * [outline] 为 true 时按 M3 描边规范补一条 outlineVariant 细边，用于在同色背景上分离卡片。
 */
@Composable
fun Modifier.glassMaterial(
    shape: Shape,
    tonalElevation: androidx.compose.ui.unit.Dp = 2.dp,
    containerColor: Color? = null,
    outline: Boolean = false
): Modifier {
    var m = this.clip(shape).background(containerColor ?: tonalSurface(tonalElevation))
    return if (outline) {
        // M3 的描边是一条极低对比度的 outlineVariant，不用渐变
        m.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else m
}

/** M3 卡片：内容纵向排列，可选点击（走 Material ripple）。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = GlassToken.CardShape,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = Modifier
        .glassMaterial(shape, tonalElevation = 2.dp, containerColor = containerColor, outline = true)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Column(modifier.then(base).fillMaxWidth()) { content() }
}

/** M3 容器：自由布局内容。 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = GlassToken.CardShape,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.glassMaterial(shape, tonalElevation = 2.dp, containerColor = containerColor, outline = true),
        content = content
    )
}
