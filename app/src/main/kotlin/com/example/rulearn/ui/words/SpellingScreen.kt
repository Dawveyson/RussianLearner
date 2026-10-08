package com.example.rulearn.ui.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.WordBook
import com.example.rulearn.data.WordEntry
import com.example.rulearn.player.AudioPlayer
import com.example.rulearn.player.Pronounce
import com.example.rulearn.player.TtsPack
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.theme.GlassCard
import kotlin.random.Random

private fun norm(s: String): String = s.lowercase().replace(Regex("\\s+"), " ").trim()

/** 单次学习/复习可选项。 */
private val SESSION_SIZES = listOf(5, 10, 15, 20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellingScreen(nav: NavController) {
    val ctx = LocalContext.current
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val books = remember(vocab, lessons) { AppRepository.practiceBooks() }

    val player = remember { AudioPlayer(ctx) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var bookId by remember { mutableStateOf("") }
    LaunchedEffect(books) {
        if (bookId.isBlank()) {
            val saved = Prefs.activeBook(ctx)
            bookId = books.firstOrNull { it.id == saved }?.id ?: books.firstOrNull()?.id ?: ""
        }
    }
    val book: WordBook? = books.firstOrNull { it.id == bookId } ?: books.firstOrNull()

    // 设置项（持久化）
    var randomMode by remember { mutableStateOf(false) }
    var sessionSize by remember { mutableStateOf(Prefs.sessionSize(ctx)) }
    var autoPlay by remember { mutableStateOf(Prefs.autoPlayAudio(ctx)) }

    // 学习流程状态
    var phase by remember { mutableStateOf("setup") }      // setup | learn | done
    var queueKind by remember { mutableStateOf("new") }    // old | new | wrong
    var queue by remember { mutableStateOf<List<WordEntry>>(emptyList()) }
    var newList by remember { mutableStateOf<List<WordEntry>>(emptyList()) }
    var qi by remember { mutableIntStateOf(0) }
    val wrongSet = remember { LinkedHashSet<String>() }    // 本轮答错的词（小写原文）

    var showReviewOldPrompt by remember { mutableStateOf(false) }

    // 当前单词的输入/结果
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var showRuKbd by remember { mutableStateOf(true) }
    var shiftOn by remember { mutableStateOf(false) }

    val entry = queue.getOrNull(qi)

    fun appendChar(ch: String) {
        input = input + ch
        result = null
        if (shiftOn) shiftOn = false
    }
    fun backspace() {
        if (input.isNotEmpty()) {
            input = input.dropLast(1)
            result = null
        }
    }

    /** 从词书挑选本轮要学的新词：未学过 → 到期 → 其余，随机则打乱。 */
    fun pickSessionWords(n: Int): List<WordEntry> {
        val all = book?.entries ?: emptyList()
        if (all.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val unseen = all.filter { AppRepository.masteryOf(it.ru).seen == 0 }
        val due = all.filter {
            val m = AppRepository.masteryOf(it.ru)
            m.seen > 0 && m.due <= now
        }
        val rest = all.filter { it !in unseen && it !in due }
        val ordered = if (randomMode) (unseen + due + rest).shuffled(Random) else (unseen + due + rest)
        return ordered.take(n)
    }

    fun startNew(list: List<WordEntry>) {
        newList = list
        queueKind = "new"
        queue = list
        qi = 0
        phase = "learn"
        Prefs.setDailyReviewPromptDay(ctx, Prefs.todayKey())
    }

    fun beginSession() {
        val list = pickSessionWords(sessionSize)
        val due = AppRepository.dueWords(sessionSize)
        // 开启自动下载时，先把本轮新词 + 待复习旧词后台缓存好
        if (Prefs.autoDownloadTts(ctx)) TtsPack.preload(ctx, (list + due).map { it.ru })
        // 每天第一次学习前，先提示复习旧单词
        if (due.isNotEmpty() && Prefs.dailyReviewPromptDay(ctx) != Prefs.todayKey()) {
            newList = list
            showReviewOldPrompt = true
        } else {
            startNew(list)
        }
    }

    fun afterQueue() {
        when (queueKind) {
            "old" -> startNew(newList)                 // 旧词复习完，进入新词
            "new" -> if (wrongSet.isNotEmpty()) {       // 学完新词，自动二次复习错题
                queueKind = "wrong"
                queue = wrongSet.mapNotNull { ru -> book?.entries?.firstOrNull { it.ru.lowercase() == ru } }.distinct()
                qi = 0
            } else {
                phase = "done"
            }
            else -> phase = "done"                      // 错题复习完，本轮结束
        }
    }

    fun check() {
        val e = entry ?: return
        val ok = norm(input) == norm(e.ru)
        result = ok
        Prefs.recordSpelling(ctx, ok)
        AppRepository.recordAnswer(e.ru, ok)
        if (!ok) wrongSet.add(e.ru.lowercase())
    }

    fun next() {
        result = null
        input = ""
        if (qi + 1 >= queue.size) afterQueue() else qi++
    }

    // 切换单词时重置输入，并按需自动朗读（开启自动下载时后台缓存）
    LaunchedEffect(entry?.ru, queueKind) {
        input = ""
        result = null
        if (autoPlay && entry != null) {
            Pronounce.play(ctx, player, entry.ru, entry.audio, Prefs.ttsOfflineFirst(ctx), Prefs.autoDownloadTts(ctx))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (phase) {
                            "done" -> "本轮完成"
                            else -> when (queueKind) {
                                "old" -> "复习旧单词"
                                "wrong" -> "错题巩固"
                                else -> "单词默写"
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (phase == "learn" && showRuKbd) {
                RuSoftKeyboard(
                    onChar = ::appendChar,
                    onBackspace = ::backspace,
                    onSpace = { appendChar(" ") },
                    onEnter = { if (input.isNotBlank()) check() },
                    onShiftClick = { shiftOn = !shiftOn },
                    shiftOn = shiftOn
                )
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (phase) {
                "setup" -> {
                    if (books.isNotEmpty()) {
                        BookPicker(
                            books = books,
                            selectedId = book?.id ?: "",
                            modifier = Modifier.fillMaxWidth(),
                            onSelect = {
                                bookId = it
                                Prefs.setActiveBook(ctx, it)
                            }
                        )
                    } else {
                        EmptyHint("先导入词库或教材才能开始学习")
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("出题方式", fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !randomMode, onClick = { randomMode = false }, label = { Text("顺序") })
                        FilterChip(selected = randomMode, onClick = { randomMode = true }, label = { Text("随机") })
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("一次学/复习多少个词", fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SESSION_SIZES.forEach { sz ->
                            FilterChip(
                                selected = sessionSize == sz,
                                onClick = { sessionSize = sz; Prefs.setSessionSize(ctx, sz) },
                                label = { Text("$sz 个") }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动朗读单词", fontWeight = FontWeight.Bold)
                            Text(
                                "每个单词出现时先播一遍读音",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPlay,
                            onCheckedChange = { autoPlay = it; Prefs.setAutoPlayAudio(ctx, it) }
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { beginSession() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = book != null
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("开始学习（${sessionSize} 个）", Modifier.padding(start = 6.dp))
                    }
                }

                "learn" -> {
                    if (entry == null) {
                        EmptyHint("该词书暂无内容，先导入词库吧")
                    } else {
                        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "进度 ${qi + 1} / ${queue.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { Pronounce.play(ctx, player, entry.ru, entry.audio, Prefs.ttsOfflineFirst(ctx), Prefs.autoDownloadTts(ctx)) }
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "播放发音")
                                    }
                                    Text(
                                        "第 ${qi + 1} / ${queue.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    entry.zh.ifBlank { entry.ru },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    "看中文，写出俄文",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it; result = null },
                            label = { Text("写出俄文") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showRuKbd = !showRuKbd }) {
                                Icon(Icons.Filled.Keyboard, contentDescription = null, Modifier.size(18.dp))
                                Text(if (showRuKbd) "收起键盘" else "俄语键盘", Modifier.padding(start = 6.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { check() }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Text("检查", Modifier.padding(start = 4.dp))
                            }
                            Button(onClick = { next() }, modifier = Modifier.weight(1f)) { Text("下一题") }
                        }

                        Spacer(Modifier.height(14.dp))
                        when (result) {
                            true -> Text("✓ 正确！", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            false -> Text(
                                "✗ 正确拼写：${entry.ru}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            null -> Text(
                                "本轮已错 ${wrongSet.size} 个（学完会再复习）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { Pronounce.play(ctx, player, entry.ru, entry.audio, Prefs.ttsOfflineFirst(ctx), Prefs.autoDownloadTts(ctx)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("听发音", Modifier.padding(start = 6.dp))
                        }

                        Spacer(Modifier.height(if (showRuKbd) 20.dp else 90.dp))
                    }
                }

                "done" -> {
                    val (c, t) = Prefs.spelling(ctx)
                    val acc = if (t == 0) 0 else c * 100 / t
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 本轮完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("新学 ${newList.size} 个词", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "本轮答错 ${wrongSet.size} 个（已自动复习巩固）",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    "累计默写正确率：$acc%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { beginSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text("再来一轮", Modifier.padding(start = 6.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { nav.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("返回首页") }
                    }
                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }

    // 每天第一次学习前，提示先复习旧单词
    if (showReviewOldPrompt) {
        val dueCount = AppRepository.dueWords(sessionSize).size
        AlertDialog(
            onDismissRequest = { /* 必须二选一，不允许误关 */ },
            title = { Text("先复习旧单词？") },
            text = {
                Text(
                    "今天还有 $dueCount 个旧单词到期待复习。建议先巩固它们，再学新词；" +
                        "复习按艾宾浩斯遗忘曲线安排，错过会更易遗忘。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReviewOldPrompt = false
                    val due = AppRepository.dueWords(sessionSize)
                    queueKind = "old"
                    queue = due
                    qi = 0
                    phase = "learn"
                }) { Text("先复习 ($dueCount)") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReviewOldPrompt = false
                    startNew(newList)
                }) { Text("跳过，学新词") }
            }
        )
    }
}
