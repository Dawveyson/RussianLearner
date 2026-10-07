package com.example.rulearn.ui.alphabet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rulearn.R
import com.example.rulearn.data.CYRILLIC_ALPHABET
import com.example.rulearn.ui.alphabet.dict.CyrillicStrokes
import com.example.rulearn.ui.theme.GlassBox
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

/** 手写体。marckscript 含完整西里尔字形，字母表页与描红页共用。 */
val HandwritingFont = FontFamily(Font(R.font.marckscript))

/** 统计一串字符的总笔画数（用于估算演示时长）。 */
private fun countStrokes(text: String): Int {
    var t = 0
    text.forEach { ch -> if (!ch.isWhitespace()) t += CyrillicStrokes.of(ch).size }
    return t
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HandwritingScreen(nav: NavController) {
    var mode by remember { mutableStateOf("letter") }
    var selected by remember { mutableStateOf(CYRILLIC_ALPHABET.first()) }
    var upper by remember { mutableStateOf(true) }
    val path = remember { Path() }
    var redraw by remember { mutableIntStateOf(0) }

    // 字母模式描单个字母（可切大小写），例词模式描该字母的例词
    val target = if (mode == "letter") {
        if (upper) selected.upper else selected.lower
    } else {
        selected.sampleRu
    }

    // 笔顺演示进度。用 Animatable 而不是手写帧循环，后者会挂起导致动画卡住。
    var demo by remember { mutableStateOf(false) }
    val anim = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    val strokeCount = remember(target) { countStrokes(target) }

    LaunchedEffect(target) {
        path.reset(); redraw++; demo = false; anim.snapTo(0f)
    }

    LaunchedEffect(demo, target) {
        if (!demo) { anim.snapTo(0f); return@LaunchedEffect }
        anim.snapTo(0f)
        // 笔画越多整体越久；单笔时长限在 260~1100ms，4 笔不会一闪而过，20 笔也不会等到烦
        val per = (9000f / max(strokeCount, 1)).toInt().coerceIn(260, 1100)
        anim.animateTo(1f, animationSpec = tween(per * max(strokeCount, 1)))
        delay(700)
        demo = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手写描红 · 笔顺") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = mode == "letter",
                    onClick = { mode = "letter" },
                    label = { Text("字母") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == "word",
                    onClick = { mode = "word" },
                    label = { Text("例词") }
                )
                Spacer(Modifier.weight(1f))
                if (mode == "letter") {
                    FilterChip(
                        selected = upper,
                        onClick = { upper = !upper },
                        label = { Text(if (upper) "大写" else "小写") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (mode == "letter") {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(58.dp),
                    modifier = Modifier.fillMaxWidth().height(148.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CYRILLIC_ALPHABET, key = { it.upper }) { l ->
                        OutlinedButton(
                            onClick = { selected = l },
                            modifier = Modifier.height(48.dp),
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

            // 描红画布：用 weight(1f) 撑满剩余空间，平板上手指跟写更顺手
            GlassBox(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    // 真实笔顺字形：浅灰描红底 + 逐笔动画 + 笔顺编号
                    StrokeGlyph(
                        text = target,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        progress = anim.value,
                        showOrderNumbers = true,
                        textMeasurer = measurer
                    )
                    // 用户笔迹
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
                        if (redraw < 0) return@Canvas
                        drawPath(
                            path,
                            color = primary,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { path.reset(); redraw++; demo = false },
                    modifier = Modifier.weight(1f)
                ) { Text("清除") }
                OutlinedButton(
                    onClick = { path.reset(); redraw++; demo = true },
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
                "跟着浅色底稿用手指描红；点「演示笔顺」会按正确笔顺逐笔写一遍（圆点数字为笔顺号）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }
}
