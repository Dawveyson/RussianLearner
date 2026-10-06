package com.example.rulearn.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.WordBook
import com.example.rulearn.player.AudioPlayer
import com.example.rulearn.player.Speaker
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.theme.GlassCard
import kotlin.random.Random

private fun norm(s: String): String = s.lowercase().replace(Regex("\\s+"), " ").trim()

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
    // 同上：等词书就绪后再套用 Prefs.activeBook，避免被空列表覆盖。
    LaunchedEffect(books) {
        if (bookId.isBlank()) {
            val saved = Prefs.activeBook(ctx)
            bookId = books.firstOrNull { it.id == saved }?.id ?: books.firstOrNull()?.id ?: ""
        }
    }
    val book: WordBook? = books.firstOrNull { it.id == bookId } ?: books.firstOrNull()

    var randomMode by remember { mutableStateOf(false) }
    var idx by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    // 默写时默认直接显示键盘：这是主要输入方式，收起来反而要多点一次
    var showRuKbd by remember { mutableStateOf(true) }
    var shiftOn by remember { mutableStateOf(false) }

    fun appendChar(ch: String) {
        input = input + ch
        result = null
        if (shiftOn) shiftOn = false   // Shift 只生效一次，和真键盘一致
    }
    fun backspace() {
        if (input.isNotEmpty()) {
            input = input.dropLast(1)
            result = null
        }
    }

    val entries = book?.entries ?: emptyList()
    val entry = entries.getOrNull(idx)
    val (correct, total) = Prefs.spelling(ctx)

    fun check() {
        val e = entry ?: return
        val ok = norm(input) == norm(e.ru)
        result = ok
        Prefs.recordSpelling(ctx, ok)
        AppRepository.recordAnswer(e.ru, ok)
    }

    fun next() {
        result = null
        input = ""
        idx = if (entries.isEmpty()) 0
        else if (randomMode) Random.nextInt(entries.size)
        else (idx + 1) % entries.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词默写") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        // 键盘固定在屏幕下半部：放进 bottomBar，不随内容滚动
        bottomBar = {
            if (showRuKbd) {
                RuSoftKeyboard(
                    onChar = ::appendChar,
                    onBackspace = ::backspace,
                    onSpace = { appendChar(" ") },
                    onEnter = { if (input.isNotBlank()) check() },
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
            if (books.isNotEmpty()) {
                BookPicker(
                    books = books,
                    selectedId = book?.id ?: "",
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = {
                        bookId = it
                        Prefs.setActiveBook(ctx, it)
                        idx = 0; result = null; input = ""
                    }
                )
            }

            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !randomMode, onClick = { randomMode = false }, label = { Text("顺序") })
                FilterChip(selected = randomMode, onClick = { randomMode = true }, label = { Text("随机") })
            }

            if (entry == null) {
                EmptyHint("该词书暂无内容，先导入词库吧")
                return@Column
            }

            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.audio.isNotBlank()) {
                            IconButton(onClick = { player.play(entry.audio) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "播放")
                            }
                        }
                        Text(
                            "第 ${idx + 1} / ${entries.size}",
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

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { shiftOn = !shiftOn }) {
                    Text(
                        if (shiftOn) "Shift 已开" else "Shift",
                        fontWeight = if (shiftOn) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
                    "累计正确率：${if (total == 0) 0 else correct * 100 / total}%（$correct/$total）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { Speaker.speak(entry.ru, true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("听发音", Modifier.padding(start = 6.dp))
            }

            Spacer(Modifier.height(if (showRuKbd) 20.dp else 90.dp))
        }
    }
}
