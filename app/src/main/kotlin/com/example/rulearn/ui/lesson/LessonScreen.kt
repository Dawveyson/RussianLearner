package com.example.rulearn.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.Lesson
import com.example.rulearn.data.Segment
import com.example.rulearn.player.AudioPlayer
import com.example.rulearn.ui.common.EmptyHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(nav: NavController, n: Int) {
    val ctx = LocalContext.current
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val lesson = lessons.firstOrNull { it.n == n }

    val player = remember { AudioPlayer(ctx) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var current by remember { mutableIntStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    var continuous by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }

    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f)

    fun playAt(index: Int, seg: Segment) {
        current = index
        playing = true
        player.onComplete = {
            if (continuous && lesson != null && index + 1 < lesson.segments.size) {
                playAt(index + 1, lesson.segments[index + 1])
            } else {
                playing = false
            }
        }
        player.playSegment(seg, lesson?.audio ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson?.title ?: "урок $n") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (lesson != null) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val p = current - 1
                                    if (p >= 0) playAt(p, lesson.segments[p])
                                }
                            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "上一段") }

                            IconButton(
                                onClick = {
                                    if (playing) {
                                        player.pause(); playing = false
                                    } else if (current in lesson.segments.indices) {
                                        player.resume(); playing = true
                                    } else if (lesson.segments.isNotEmpty()) {
                                        playAt(0, lesson.segments[0])
                                    }
                                }
                            ) {
                                Icon(
                                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "播放/暂停"
                                )
                            }

                            IconButton(
                                onClick = {
                                    val nx = current + 1
                                    if (nx < lesson.segments.size) playAt(nx, lesson.segments[nx])
                                }
                            ) { Icon(Icons.Filled.SkipNext, contentDescription = "下一段") }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text("连播", style = MaterialTheme.typography.labelMedium)
                                Switch(checked = continuous, onCheckedChange = { continuous = it })
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            speeds.forEach { s ->
                                FilterChip(
                                    selected = speed == s,
                                    onClick = {
                                        speed = s
                                        player.speed = s
                                    },
                                    label = { Text(if (s == 1f) "1.0x" else "${s}x") }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (lesson == null) {
            EmptyHint("找不到第 $n 课", Modifier.fillMaxSize().padding(pad))
            return@Scaffold
        }
        SegmentList(
            lesson = lesson,
            current = current,
            onPlay = { i -> playAt(i, lesson.segments[i]) },
            modifier = Modifier.fillMaxSize().padding(pad)
        )
    }
}

@Composable
private fun SegmentList(
    lesson: Lesson,
    current: Int,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(lesson.segments, key = { _, s -> s.i }) { idx, seg ->
            val selected = idx == current
            ElevatedCard(
                onClick = { onPlay(idx) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${seg.i}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(seg.ru, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            seg.zh,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
