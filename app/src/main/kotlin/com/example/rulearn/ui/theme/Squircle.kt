package com.example.rulearn.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Apple 风格的「连续曲率」圆角（squircle）。
 * 用超椭圆采样生成四个角，比普通圆角矩形更接近 iOS 的观感。
 *
 * @param radius 圆角半径
 * @param n 超椭圆指数，越大越接近方角；iOS 玻璃约 4~5
 */
class SquircleShape(private val radius: Dp, private val n: Float = 4.2f) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }.coerceIn(0f, size.minDimension / 2f)
        return Outline.Generic(squirclePath(size.width, size.height, r, n))
    }

    override fun toString(): String = "SquircleShape(radius=$radius, n=$n)"
}

/** 生成 squircle 路径（左上角原点）。 */
fun squirclePath(w: Float, h: Float, r: Float, n: Float): Path {
    if (r <= 0f) {
        return Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h)) }
    }
    val p = Path()
    val steps = 18
    val exp = 2f / n

    // 角参数：θ 从 0→π/2 时 (x,y) 从 (0,r) 走到 (r,0)
    val xs = FloatArray(steps + 1)
    val ys = FloatArray(steps + 1)
    for (i in 0..steps) {
        val t = (Math.PI / 2) * i / steps
        xs[i] = r * (1f - cos(t).toFloat().pow(exp))
        ys[i] = r * (1f - sin(t).toFloat().pow(exp))
    }

    p.moveTo(0f, r)
    // 左上：(0,r) → (r,0)，xs 递增、ys 递减
    for (i in 1..steps) p.lineTo(xs[i], ys[i])
    // 上边
    p.lineTo(w - r, 0f)
    // 右上：(w-r,0) → (w,r)，要从 xs=r 走回 xs=0，必须逆序
    for (i in steps downTo 0) p.lineTo(w - xs[i], ys[i])
    // 右边
    p.lineTo(w, h - r)
    // 右下：(w,h-r) → (w-r,h)，xs 递增、ys 递减
    for (i in 1..steps) p.lineTo(w - xs[i], h - ys[i])
    // 下边
    p.lineTo(r, h)
    // 左下：(r,h) → (0,h-r)，同样需要逆序，否则会斜切出一条三角
    for (i in steps downTo 0) p.lineTo(xs[i], h - ys[i])
    p.close()
    return p
}
