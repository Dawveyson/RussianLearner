package com.example.rulearn.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.Lesson
import com.example.rulearn.data.WordEntry
import com.example.rulearn.player.AudioPlayer
import com.example.rulearn.player.Pronounce
import com.example.rulearn.ui.Route
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.common.LocalWordArtContentColor
import com.example.rulearn.ui.common.ProgressRing
import com.example.rulearn.ui.common.SectionTitle
import com.example.rulearn.ui.common.StatTile
import com.example.rulearn.ui.common.WordArt
import com.example.rulearn.ui.theme.GlassBox
import com.example.rulearn.ui.theme.GlassCard
import com.example.rulearn.ui.theme.GlassToken

@Composable
fun HomeScreen(nav: NavController) {
    val stats by AppRepository.stats.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val progress by AppRepository.progress.collectAsStateWithLifecycle()

    // 依赖词书与掌握度：答完题或导入词库后，复习队列会跟着刷新。
    val queue = remember(vocab, lessons, progress) { AppRepository.reviewQueue() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GreetingHeader(streak = stats.streak, total = stats.totalWords) }

        item {
            TodayCard(
                learned = stats.learnedToday,
                goal = stats.dailyGoal,
                due = stats.dueToday,
                onStart = { nav.navigate(Route.SPELLING) },
                onQuiz = { nav.navigate(Route.QUIZ) }
            )
        }

        item {
            SectionTitle("今日单词")
            if (queue.isEmpty()) {
                EmptyHint("还没有可学的词，先去「个人 → 导入资源」添加词库或教材吧")
            } else {
                WordHeroCard(
                    entry = queue.first(),
                    onAnswer = { correct ->
                        // recordAnswer 会更新 progress，queue 随之上游重算，无需手动刷新
                        AppRepository.recordAnswer(queue.first().ru, correct)
                    }
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatTile("${stats.totalWords}", "总词条")
                    StatTile("${stats.mastered}", "已掌握")
                    StatTile("${stats.dueToday}", "待复习")
                    StatTile("${stats.spellingAcc}%", "默写正确率")
                }
            }
        }

        item {
            SectionTitle(
                "课文跟读",
                action = {
                    if (lessons.isNotEmpty()) {
                        Text(
                            "全部 ${lessons.size} 课",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            if (lessons.isEmpty()) {
                GlassCard(onClick = { nav.navigate(Route.IMPORT) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📖 还没有课本", fontWeight = FontWeight.Bold)
                        Text(
                            "导入课程 JSON 后即可逐句跟读、后台随身听。点这里去导入。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LessonRow(lessons) { nav.navigate("lesson/${it.n}") }
            }
        }

        item {
            SectionTitle("快捷入口")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(
                    icon = Icons.Filled.Headphones,
                    title = "随身听",
                    sub = "后台连播",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Route.ME) }
                QuickAction(
                    icon = Icons.Filled.GraphicEq,
                    title = "字母",
                    sub = "字母表 / 描红",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Route.ALPHABET) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(
                    icon = Icons.Filled.Download,
                    title = "导入资源",
                    sub = "词库 / 教材",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Route.IMPORT) }
                QuickAction(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    title = "查单词",
                    sub = "离线 + 在线",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Route.WORDS) }
            }
        }
    }
}

@Composable
private fun GreetingHeader(streak: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Привет!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (total == 0) "先导入词库，马上开始" else "今天也要加油哦",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (streak > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "连续 $streak 天",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayCard(
    learned: Int,
    goal: Int,
    due: Int,
    onStart: () -> Unit,
    onQuiz: () -> Unit
) {
    val progress = if (goal == 0) 0f else learned.toFloat() / goal
    GlassBox(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.size(84.dp),
                stroke = 9.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$learned",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "/ $goal",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(Modifier.padding(start = 18.dp).weight(1f)) {
                Text("今日学习进度", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (due > 0) "待复习 $due 个词" else "暂无需复习的词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("开始学习", Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(onClick = onQuiz) { Text("闯关") }
                }
            }
        }
    }
}

@Composable
private fun WordHeroCard(entry: WordEntry, onAnswer: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val player = remember { AudioPlayer(ctx) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var revealed by remember(entry.ru) { mutableStateOf(false) }
    val mastery = AppRepository.masteryOf(entry.ru)

    // 点击查看释义后自动播放单词读音（优先离线包 → 网络发音），并后台自动缓存。
    // 用 try/catch 兜住，避免发音路径里的任何异常（网络/媒体/IO）冒泡到 LaunchedEffect
    // 导致协程崩溃、整页 100% 闪退。
    LaunchedEffect(revealed, entry.ru) {
        if (revealed) {
            try {
                Pronounce.play(
                    ctx, player, entry.ru, entry.audio,
                    Prefs.ttsOfflineFirst(ctx), Prefs.autoDownloadTts(ctx)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp),
        shape = GlassToken.CardShape
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .clickable { revealed = !revealed }
            ) {
                WordArt(entry.ru, Modifier.matchParentSize()) {
                    if (entry.img.isNotBlank()) {
                        AsyncImage(
                            model = entry.img,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            entry.ru,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalWordArtContentColor.current,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (revealed) entry.zh else "点击查看释义",
                            fontSize = 16.sp,
                            color = LocalWordArtContentColor.current.copy(
                                alpha = if (revealed) 0.92f else 0.7f
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (mastery.level > 0) {
                        Text(
                            "掌握度 Lv.${mastery.level}",
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.30f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f)
                ) { Text("不认识") }
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f)
                ) { Text("认识") }
            }
        }
    }
}

@Composable
private fun LessonRow(lessons: List<Lesson>, onOpen: (Lesson) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(lessons, key = { it.n }) { lesson ->
            GlassCard(
                onClick = { onOpen(lesson) },
                modifier = Modifier.width(190.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "урок ${lesson.n}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "第 ${lesson.n} 课 · ${lesson.segments.size} 段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "开始跟读",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
