package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.data.AppRepository
import com.example.rulearn.desktop.player.DesktopPlayer
import com.example.rulearn.nav.Route

@Composable
fun ListenScreen(player: DesktopPlayer, navigate: (String) -> Unit) {
    val lessons by AppRepository.lessons.collectAsState()
    var selected by remember { mutableStateOf<Int?>(null) }
    var current by remember { mutableStateOf(-1) }
    var paused by remember { mutableStateOf(false) }
    var continuous by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }

    val lesson = lessons.firstOrNull { it.n == (selected ?: lessons.firstOrNull()?.n) } ?: lessons.firstOrNull()

    ScreenScroll {
        if (lessons.isEmpty()) {
            Text("还没有课文。请到「学习资源」导入课文 JSON 或 ZIP 音频书。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(12)
            androidx.compose.material3.TextButton(onClick = { navigate(Route.WORDS) }) { Text("去导入") }
            return@ScreenScroll
        }

        SectionTitle("选择课文")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lessons.take(8).forEach { l ->
                FilterChip(
                    selected = lesson?.n == l.n,
                    onClick = { selected = l.n; current = -1; paused = false; player.stop() },
                    label = { Text("урок ${l.n}") }
                )
            }
        }

        Spacer(8)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = continuous, onClick = { continuous = !continuous }, label = { Text("连播") })
            listOf(0.5f, 0.75f, 1f, 1.25f).forEach { s ->
                FilterChip(selected = speed == s, onClick = { speed = s }, label = { Text("${s}x") })
            }
        }
        Text("注：桌面端语速调节为预览项，实际生效取决于音频解码（多数 mp3 暂按原速播放）。", style = MaterialTheme.typography.bodySmall)

        Spacer(8)
        SectionTitle("课文逐句")
        if (lesson != null) {
            LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(lesson.segments) { i, seg ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(seg.ru, style = MaterialTheme.typography.titleMedium)
                                Text(seg.zh, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = {
                                val src = seg.audio.ifBlank { lesson.audio }
                                if (src.isNotBlank()) {
                                    when {
                                        current == i && player.isPaused() -> {
                                            player.resume(); paused = false
                                        }
                                        current == i -> {
                                            player.pause(); paused = true
                                        }
                                        else -> {
                                            current = i; paused = false
                                            player.play(src, onComplete = {
                                                if (continuous && i + 1 < lesson.segments.size) {
                                                    val nx = lesson.segments[i + 1]
                                                    val s2 = nx.audio.ifBlank { lesson.audio }
                                                    if (s2.isNotBlank()) {
                                                        current = i + 1
                                                        player.play(s2, onComplete = {})
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    if (current == i && !paused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "播放"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
