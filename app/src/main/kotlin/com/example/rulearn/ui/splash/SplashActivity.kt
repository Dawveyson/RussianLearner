package com.example.rulearn.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rulearn.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 开机动画（demo）：Gemini 风格 —— 中心四角星流光旋转 + 「RuLearner」文字渐显上浮。
 * 动画约 2.6s 后进入主界面。后续可继续打磨时长 / 路径动画 / 配色。
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SplashContent {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SplashContent(onFinished: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "spin")
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing))
    )
    val scaleA = remember { Animatable(0.5f) }
    val alphaA = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        launch { scaleA.animateTo(1f, tween(900, easing = EaseOutBack)) }
        launch { alphaA.animateTo(1f, tween(700)) }
        launch { textAlpha.animateTo(1f, tween(900, delayMillis = 450)) }
        launch { textOffset.animateTo(0f, tween(900, delayMillis = 450)) }
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1B1D2E), Color(0xFF07080F)),
                    center = Offset(0.5f, 0.42f),
                    radius = 1100f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(128.dp)
                    .alpha(alphaA.value)
                    .scale(scaleA.value, scaleA.value)
                    .rotate(rotation)
            ) {
                val c = Offset(size.width / 2, size.height / 2)
                val rOuter = size.minDimension / 2f
                val rInner = rOuter * 0.36f
                val steps = 8 // 四角星：4 个外尖 + 4 个内凹
                val path = Path()
                for (i in 0 until steps) {
                    val ang = (i * (360f / steps)) * (PI / 180.0).toFloat()
                    val rad = if (i % 2 == 0) rOuter else rInner
                    val x = c.x + cos(ang) * rad
                    val y = c.y + sin(ang) * rad
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4285F4),
                            Color(0xFF9B72CB),
                            Color(0xFFD96570)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(
                text = "RuLearner",
                fontSize = 34.sp,
                color = Color.White.copy(alpha = textAlpha.value),
                modifier = Modifier.alpha(textAlpha.value).offset(y = textOffset.value.dp)
            )
        }
    }
}
