package com.example.rulearn.ui.alphabet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rulearn.R
import com.example.rulearn.data.CYRILLIC_ALPHABET
import com.example.rulearn.ui.theme.GlassBox
import kotlin.random.Random

/** 手写体。marckscript 含完整西里尔字形，字母表页与描红页共用。 */
val HandwritingFont = FontFamily(Font(R.font.marckscript))

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HandwritingScreen(nav: NavController) {
    var mode by remember { mutableStateOf("letter") }
    // 例词跟着当前字母走：字母模式描字母，例词模式描这个字母的例词
    var selected by remember { mutableStateOf(CYRILLIC_ALPHABET.first()) }
    var redraw by remember { mutableIntStateOf(0) }
    val path = remember { Path() }
    val target = if (mode == "letter") selected.upper else selected.sampleRu

    // 笔顺演示：progress 从 0 走到 1，字形按书写方向逐步显现 + 笔尖光点跟随
    var demo by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val isSingle = target.length == 1
    val primaryColor = MaterialTheme.colorScheme.primary

    // 模式或字母一变就清空笔迹，否则上一次的手写会串到新字上
    LaunchedEffect(mode, selected) { path.reset(); redraw++; demo = false; progress = 0f }

    LaunchedEffect(demo, target) {
        if (!demo) { progress = 0f; return@LaunchedEffect }
        progress = 0f
        val durationMs = if (isSingle) 1100L else 2600L
        val startNanos = withFrameMillis { it }
        while (true) {
            val nowNanos = withFrameMillis { it }
            progress = ((nowNanos - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            if (progress >= 1f) break
        }
        // 写完停一下再淡出，方便看清最后一笔
        kotlinx.coroutines.delay(600)
        demo = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手写描红") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = mode == "letter",
                    onClick = { mode = "letter" },
                    label = { Text("字母") }
                )
                androidx.compose.material3.FilterChip(
                    selected = mode == "word",
                    onClick = { mode = "word" },
                    label = { Text("例词") }
                )
            }

            Spacer(Modifier.height(10.dp))
            if (mode == "letter") {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CYRILLIC_ALPHABET, key = { it.upper }) { l ->
                        OutlinedButton(
                            onClick = { selected = l },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(l.upper, fontSize = 20.sp, fontFamily = HandwritingFont) }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { selected = CYRILLIC_ALPHABET.random(Random) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎲 随机取一个例词") }
            }

            Spacer(Modifier.height(12.dp))

            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // 描红底：完整浅色字形，始终可见
                    Text(
                        target,
                        fontSize = if (isSingle) 180.sp else 56.sp,
                        fontFamily = HandwritingFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                    )
                    // 笔顺演示层：按书写方向逐步显现，模拟"跟着写"
                    if (progress > 0f) {
                        Text(
                            target,
                            fontSize = if (isSingle) 180.sp else 56.sp,
                            fontFamily = HandwritingFont,
                            color = primaryColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .drawWithContent {
                                    // 对角逐步揭示：左上 -> 右下，模拟"从上到下、从左到右"书写
                                    clipRect(
                                        right = size.width * progress,
                                        bottom = size.height * progress
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                        )
                    }
                    // 笔尖光点：沿书写方向移动
                    if (demo && progress in 0f..1f) {
                        val travel = if (isSingle) 1f else 0.55f
                        val y = (0.12f + progress * travel).coerceAtMost(0.86f)
                        val x = 0.5f + (0.30f - progress * 0.60f)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    drawContent()
                                    val cx = size.width * x.coerceIn(0.04f, 0.96f)
                                    val cy = size.height * y
                                    drawCircle(
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f),
                                        radius = 13f
                                    )
                                    drawCircle(
                                        color = primaryColor,
                                        radius = 8f
                                    )
                                }
                        )
                    }
                    val primary = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { path.moveTo(it.x, it.y) },
                                    onDrag = { change, _ ->
                                        path.lineTo(change.position.x, change.position.y)
                                        redraw++
                                    }
                                )
                            }
                    ) {
                        // 在绘制作用域里订阅 redraw：笔迹变化或清空时才会真正重绘
                        if (redraw < 0) return@Canvas
                        drawPath(
                            path,
                            color = primary,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { path.reset(); redraw++; demo = false; progress = 0f },
                    modifier = Modifier.weight(1f)
                ) { Text("清除") }
                OutlinedButton(
                    onClick = { path.reset(); redraw++; progress = 0f; demo = true },
                    modifier = Modifier.weight(1f)
                ) { Text(if (demo) "书写中…" else "演示笔顺") }
                Button(
                    onClick = {
                        selected = CYRILLIC_ALPHABET.random(Random)
                        path.reset(); redraw++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("下一个", Modifier.padding(start = 4.dp))
                }
            }
            Text(
                "提示：在半透明字上用手指描红即可。笔顺从上到下、从左到右。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }
    }
}
