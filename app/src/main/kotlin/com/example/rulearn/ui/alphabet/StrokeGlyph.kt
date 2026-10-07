package com.example.rulearn.ui.alphabet

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.rulearn.ui.alphabet.dict.CyrillicStrokes
import com.example.rulearn.ui.alphabet.dict.GlyphStroke
import com.example.rulearn.ui.alphabet.dict.StrokeKind
import kotlin.math.max

/** 笔顺配色，与 tools/render_glyphs.py 的证明图保持一致。 */
private val STROKE_COLORS = listOf(
    Color(0xFF2F6FD0), Color(0xFFD0452F), Color(0xFF2F9D55), Color(0xFFA345CF),
    Color(0xFFD08A2F), Color(0xFF2FA8A8), Color(0xFFC0407A), Color(0xFF5A7D2F)
)

private val GUIDE_COLOR = Color(0xFFDDDDDD)
private val INK_COLOR = Color(0xFF2F6FD0)

/** 字形里的一笔，带它属于第几个字符、以及在整体时间轴上的区间。 */
private class Slot(
    val charIndex: Int,
    val stroke: GlyphStroke,
    val startFrac: Float,
    val endFrac: Float
)

/** 收集整串字符的所有笔画，并按顺序均分时间轴。 */
private fun layout(chars: List<Char>): List<Slot> {
    val flat = ArrayList<Pair<Int, GlyphStroke>>()
    chars.forEachIndexed { ci, ch ->
        CyrillicStrokes.of(ch).forEach { flat.add(ci to it) }
    }
    val total = max(flat.size, 1)
    return flat.mapIndexed { i, (ci, s) ->
        Slot(ci, s, i.toFloat() / total, (i + 1).toFloat() / total)
    }
}

/** 二次贝塞尔细分（把曲线打散成密集点，便于按弧长截断）。 */
private fun quad(p0: Offset, c: Offset, p1: Offset, steps: Int = 14): List<Offset> {
    val out = ArrayList<Offset>(steps)
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val mt = 1 - t
        val x = mt * mt * p0.x + 2 * mt * t * c.x + t * t * p1.x
        val y = mt * mt * p0.y + 2 * mt * t * c.y + t * t * p1.y
        out.add(Offset(x, y))
    }
    return out
}

/** 把一笔展开成画布坐标下的密集点序列（已按顺序）。 */
private fun flatten(stroke: GlyphStroke, ox: Float, oy: Float, w: Float, h: Float): List<Offset> {
    if (stroke.pts.isEmpty()) return emptyList()
    fun pt(x: Float, y: Float) = Offset(ox + x * w, oy + y * h)
    val out = ArrayList<Offset>()
    out.add(pt(stroke.pts[0].first, stroke.pts[0].second))
    when (stroke.kind) {
        StrokeKind.LINE -> {
            for (i in 1 until stroke.pts.size) {
                out.add(pt(stroke.pts[i].first, stroke.pts[i].second))
            }
        }
        else -> {
            val seq = if (stroke.kind == StrokeKind.LOOP) stroke.pts + stroke.pts[0]
            else stroke.pts
            for (i in 0 until seq.size - 1) {
                val a = pt(seq[i].first, seq[i].second)
                val b = pt(seq[i + 1].first, seq[i + 1].second)
                val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                out.addAll(quad(a, a, mid))
                out.add(mid)
            }
        }
    }
    return out
}

/** 密集点 -> Path。 */
private fun pathOf(pts: List<Offset>): Path {
    val p = Path()
    if (pts.isEmpty()) return p
    p.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) p.lineTo(pts[i].x, pts[i].y)
    return p
}

/** 按弧长比例截取前一段，做出"这一笔正在写"的效果。 */
private fun partialOf(
    pts: List<Offset>, ratio: Float
): Path {
    if (ratio >= 1f) return pathOf(pts)
    val out = Path()
    if (pts.isEmpty() || ratio <= 0f) return out
    out.moveTo(pts[0].x, pts[0].y)
    var acc = 0f
    var total = 0f
    for (i in 0 until pts.size - 1) {
        total += hypot(pts[i].x - pts[i + 1].x, pts[i].y - pts[i + 1].y)
    }
    if (total <= 0f) return out
    val want = total * ratio
    for (i in 0 until pts.size - 1) {
        val seg = hypot(pts[i].x - pts[i + 1].x, pts[i].y - pts[i + 1].y)
        if (acc + seg >= want) {
            val t = if (seg > 0f) (want - acc) / seg else 0f
            val x = pts[i].x + (pts[i + 1].x - pts[i].x) * t
            val y = pts[i].y + (pts[i + 1].y - pts[i].y) * t
            out.lineTo(x, y)
            return out
        }
        acc += seg
        out.lineTo(pts[i + 1].x, pts[i + 1].y)
    }
    return out
}

private fun hypot(x: Float, y: Float): Float = kotlin.math.sqrt(x * x + y * y)

/**
 * 笔顺字形（支持单个字母与单词）。
 *
 * @param text 单个字母，或一个单词（单词会按字母并排、依次书写）
 * @param progress 0..1 全局书写进度，驱动"演示笔顺"逐笔动画
 * @param showOrderNumbers 是否在每笔起点标出笔顺号
 *
 * 无论 progress 多大，都会先画一层浅灰完整字形作为描红底；
 * 字形随容器等比放大，平板上可以用手指跟着描。
 */
@Composable
fun StrokeGlyph(
    text: String,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    showOrderNumbers: Boolean = true,
    textMeasurer: TextMeasurer? = null
) {
    val chars = remember(text) { text.filter { !it.isWhitespace() }.toList() }
    val slots = remember(text) { layout(chars) }

    Canvas(modifier) {
        if (chars.isEmpty()) return@Canvas

        val n = chars.size
        // 单词按宽度摊分，单个字母尽量占满高度（平板上更好跟着手写）
        val gap = if (n > 1) 0.06f else 0f
        val slotW = size.width / (n + (n - 1) * gap)
        val glyphH: Float
        val glyphW: Float
        if (n > 1) {
            glyphW = slotW * 0.94f
            glyphH = minOf(glyphW * 1.12f, size.height * 0.94f)
        } else {
            glyphH = size.height * 0.94f
            glyphW = minOf(glyphH / 1.12f, size.width * 0.94f)
        }
        val totalW = n * glyphW + (n - 1) * slotW * gap
        val ox0 = (size.width - totalW) / 2f
        val oy0 = (size.height - glyphH) / 2f

        fun originOf(ci: Int) = ox0 + ci * (glyphW + slotW * gap)
        fun strokeW() = max(glyphW * 0.05f, 4f)
        fun guideW() = max(glyphW * 0.075f, 6f)

        // ---- 描红底：完整浅灰字形 ----
        slots.forEach { s ->
            drawPath(
                pathOf(flatten(s.stroke, originOf(s.charIndex), oy0, glyphW, glyphH)),
                color = GUIDE_COLOR,
                style = Stroke(width = guideW(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // ---- 按笔顺逐笔书写 ----
        slots.forEachIndexed { i, s ->
            val local = when {
                progress <= s.startFrac -> 0f
                progress >= s.endFrac -> 1f
                else -> (progress - s.startFrac) / (s.endFrac - s.startFrac)
            }
            if (local <= 0f) return@forEachIndexed
            val color = if (progress > 0f) STROKE_COLORS[i % STROKE_COLORS.size] else INK_COLOR
            val flat = flatten(s.stroke, originOf(s.charIndex), oy0, glyphW, glyphH)
            drawPath(
                partialOf(flat, local),
                color = color,
                style = Stroke(width = strokeW(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // ---- 笔顺编号 ----
        if (showOrderNumbers && progress > 0f && textMeasurer != null) {
            val r = max(glyphW * 0.055f, 9f)
            slots.forEachIndexed { i, s ->
                if (progress < s.startFrac) return@forEachIndexed
                val p0 = s.stroke.pts.firstOrNull() ?: return@forEachIndexed
                val cx = originOf(s.charIndex) + p0.first * glyphW
                val cy = oy0 + p0.second * glyphH
                drawCircle(STROKE_COLORS[i % STROKE_COLORS.size], r, Offset(cx, cy))
                val layout = textMeasurer.measure(
                    "${i + 1}",
                    TextStyle(fontSize = (r * 1.3f).toSp(), color = Color.White, fontWeight = FontWeight.Bold)
                )
                drawText(
                    layout,
                    topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
                )
            }
        }
    }
}
