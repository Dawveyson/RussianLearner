package com.example.rulearn.ui.alphabet

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * 描红底稿：用真实手写体（marckscript）把字符渲染成浅灰模板，供手指描红。
 * 不做任何笔顺/书写动画——纯字形描红。
 */
@Composable
fun StrokeGlyph(
    text: String,
    modifier: Modifier = Modifier
) {
    val chars = remember(text) { text.filter { !it.isWhitespace() } }
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        if (chars.isEmpty()) return@Canvas
        val n = chars.length
        val cellW = size.width / n
        val cellH = size.height
        chars.forEachIndexed { ci, ch ->
            val left = ci * cellW
            val fs = ((if (cellW < cellH) cellW else cellH) * 0.82f) / density
            val tl = measurer.measure(
                ch.toString(),
                TextStyle(fontSize = fs.sp, fontFamily = HandwritingFont, color = GUIDE_COLOR)
            )
            val tx = left + (cellW - tl.size.width) / 2f
            val ty = (cellH - tl.size.height) / 2f
            drawText(tl, topLeft = Offset(tx, ty), color = GUIDE_COLOR)
        }
    }
}

/** 描红底稿颜色（浅灰，手写体字形）。 */
val GUIDE_COLOR = Color(0xFFC9C9C9)
