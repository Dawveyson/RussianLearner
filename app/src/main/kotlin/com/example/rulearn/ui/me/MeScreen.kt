package com.example.rulearn.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.data.AppRepository
import com.example.rulearn.player.PocketService
import com.example.rulearn.player.PocketState
import com.example.rulearn.ui.Route
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.common.SectionTitle
import com.example.rulearn.ui.common.StatTile
import com.example.rulearn.ui.theme.GlassBox
import com.example.rulearn.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val stats by AppRepository.stats.collectAsStateWithLifecycle()
    val books by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()

    var lessonChoice by remember { mutableIntStateOf(0) }
    var random by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("个人") }) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("学习概览", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatTile("${stats.totalWords}", "总词条")
                            StatTile("${stats.mastered}", "已掌握")
                            StatTile("${stats.streak}", "连续天数")
                            StatTile("${stats.learnedToday}", "今日已学")
                        }
                    }
                }
            }

            item { SectionTitle("随身听") }
            item {
                GlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            PocketState.label.ifBlank { "未在播放" },
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (PocketState.running) {
                            Text(
                                "第 ${PocketState.currentIndex + 1} / ${PocketState.total} 段",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { PocketService.send(ctx, PocketService.ACTION_PREV) },
                                enabled = PocketState.running
                            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "上一段") }

                            IconButton(
                                onClick = {
                                    if (PocketState.isPlaying) PocketService.send(ctx, PocketService.ACTION_PAUSE)
                                    else PocketService.send(ctx, PocketService.ACTION_PLAY)
                                },
                                enabled = PocketState.running
                            ) {
                                Icon(
                                    if (PocketState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "播放/暂停"
                                )
                            }

                            IconButton(
                                onClick = { PocketService.send(ctx, PocketService.ACTION_NEXT) },
                                enabled = PocketState.running
                            ) { Icon(Icons.Filled.SkipNext, contentDescription = "下一段") }
                        }
                        if (PocketState.running) {
                            val dur = maxOf(1, PocketState.durationMs)
                            var dragging by remember { mutableStateOf(false) }
                            var dragPos by remember { mutableStateOf(0f) }
                            val pos = if (dragging) dragPos else PocketState.positionMs.toFloat()
                            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Slider(
                                    value = pos.coerceIn(0f, dur.toFloat()),
                                    onValueChange = { dragging = true; dragPos = it },
                                    onValueChangeFinished = {
                                        dragging = false
                                        PocketState.seekMs(ctx, dragPos.toInt())
                                    },
                                    valueRange = 0f..dur.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "${fmt(PocketState.positionMs)} / ${fmt(dur)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    PocketService.start(ctx, lessonChoice, random)
                                },
                                enabled = lessons.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text(if (PocketState.running) "重新开始" else "开始播放")
                            }
                            OutlinedButton(
                                onClick = { PocketService.send(ctx, PocketService.ACTION_STOP) },
                                enabled = PocketState.running,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("停止")
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.FilterChip(
                                selected = !random,
                                onClick = { random = false },
                                label = { Text("顺序") }
                            )
                            androidx.compose.material3.FilterChip(
                                selected = random,
                                onClick = { random = true },
                                label = { Text("随机") }
                            )
                            Text(
                                if (lessonChoice == 0) "全部课程" else "第 $lessonChoice 课",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        if (lessons.size > 1) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LessonChip(0, "全部", lessonChoice == 0) { lessonChoice = 0 }
                                lessons.forEach { l ->
                                    LessonChip(l.n, "урок ${l.n}", lessonChoice == l.n) { lessonChoice = l.n }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(onClick = { showLyrics = !showLyrics }) {
                                Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text(if (showLyrics) "隐藏歌词" else "歌词")
                            }
                        }
                        AnimatedVisibility(showLyrics) {
                            if (PocketState.lines.isNotEmpty()) PocketTimeline(ctx)
                        }
                        if (lessons.isEmpty()) {
                            EmptyHint("先导入教材才能使用随身听")
                        }
                    }
                }
            }

            item { SectionTitle("资源管理") }
            item {
                GlassCard(onClick = { nav.navigate(Route.IMPORT) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("导入 / 下载资源", fontWeight = FontWeight.SemiBold)
                            Text(
                                "词库 ${books.size} 本 · 教材 ${lessons.size} 课",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
            item {
                GlassCard(onClick = { nav.navigate(Route.SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("设置", Modifier.padding(start = 12.dp).weight(1f), fontWeight = FontWeight.SemiBold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            item { SectionTitle("我的词库（${books.size}）") }
            if (books.isEmpty()) {
                item { EmptyHint("还没有词库，去「导入 / 下载资源」添加") }
            } else {
                items(books.size) { i ->
                    val b = books[i]
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(
                                    "${b.entries.size} 条词条",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { AppRepository.deleteBook(b.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除词库")
                            }
                        }
                    }
                }
            }

            item { SectionTitle("我的教材（${lessons.size}）") }
            if (lessons.isEmpty()) {
                item { EmptyHint("还没有教材") }
            } else {
                items(lessons.size) { i ->
                    val l = lessons[i]
                    ElevatedCard(
                        onClick = { nav.navigate("lesson/${l.n}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("урок ${l.n} · 第${l.n}课", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${l.segments.size} 段 · ${if (l.audio.isNotBlank()) "整课音轨" else "无整课音轨"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { AppRepository.deleteLesson(l.n) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除该课")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 毫秒转 m:ss，给时间轴进度条用。 */
private fun fmt(ms: Int): String {
    val s = max(0, ms) / 1000
    return "${s / 60}:${String.format("%02d", s % 60)}"
}

@Composable
private fun LessonChip(value: Int, text: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

/**
 * 歌词时间轴：显示当前播放列表每一段（歌词行），高亮当前、点击跳转、自动滚动，
 * 下方进度条可在当前段内拖动定位。
 */
@Composable
private fun PocketTimeline(ctx: android.content.Context) {
    GlassBox(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Text(
                "进度 ${fmt(PocketState.positionMs)} / ${fmt(maxOf(1, PocketState.durationMs))}",
                style = MaterialTheme.typography.labelSmall
            )
            PocketState.lines.forEachIndexed { i, line ->
                val active = i == PocketState.currentIndex
                Text(
                    line,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { PocketState.seekIndex(ctx, i) }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    style = if (active) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
