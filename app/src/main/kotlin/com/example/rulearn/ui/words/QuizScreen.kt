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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rulearn.core.Prefs
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.WordBook
import com.example.rulearn.player.AudioPlayer
import com.example.rulearn.ui.common.EmptyHint
import com.example.rulearn.ui.theme.GlassCard

private val GREEN = Color(0xFF2E7D32)
private val RED = Color(0xFFC62828)

private data class QuizQ(
    val prompt: String,
    val answer: String,
    val options: List<String>,
    val audio: String,
    /** 被考查的原始俄语词条，掌握度必须以此为键（不能是中文释义）。 */
    val word: String
)

private fun buildQuestions(book: WordBook, mode: String, n: Int): List<QuizQ> {
    val pool = book.entries.shuffled()
    val out = mutableListOf<QuizQ>()
    for (e in pool) {
        if (out.size >= n) break
        when (mode) {
            "zh2ru" -> {
                val others = book.entries.filter { it.ru != e.ru }.shuffled().take(3).map { it.ru }
                if (others.size < 3) continue
                out.add(QuizQ(e.zh.ifEmpty { e.ru }, e.ru, (others + e.ru).shuffled(), e.audio, e.ru))
            }
            "ru2zh" -> {
                if (e.zh.isBlank()) continue
                val others = book.entries.filter { it.zh.isNotBlank() && it.zh != e.zh }.shuffled().take(3).map { it.zh }
                if (others.size < 3) continue
                out.add(QuizQ(e.ru, e.zh, (others + e.zh).shuffled(), e.audio, e.ru))
            }
            else -> {
                if (e.zh.isBlank()) continue
                out.add(QuizQ(e.zh, e.ru, emptyList(), e.audio, e.ru))
            }
        }
    }
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(nav: NavController) {
    val ctx = LocalContext.current
    val vocab by AppRepository.vocabBooks.collectAsStateWithLifecycle()
    val lessons by AppRepository.lessons.collectAsStateWithLifecycle()
    val books = remember(vocab, lessons) { AppRepository.practiceBooks() }

    val player = remember { AudioPlayer(ctx) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var bookId by remember { mutableStateOf("") }
    // 词书在首帧可能尚未从磁盘加载完成，等 books 就绪后再定初值，
    // 否则 Prefs.activeBook 里记住的词书会被空列表覆盖掉。
    LaunchedEffect(books) {
        if (bookId.isBlank()) {
            val saved = Prefs.activeBook(ctx)
            bookId = books.firstOrNull { it.id == saved }?.id ?: books.firstOrNull()?.id ?: ""
        }
    }
    val book: WordBook? = books.firstOrNull { it.id == bookId } ?: books.firstOrNull()

    var phase by remember { mutableStateOf("setup") }
    var mode by remember { mutableStateOf("zh2ru") }
    var questions by remember { mutableStateOf<List<QuizQ>>(emptyList()) }
    var qi by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(5) }
    var picked by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    var spellInput by remember { mutableStateOf("") }

    fun start() {
        val b = book ?: return
        questions = buildQuestions(b, mode, 10)
        qi = 0; score = 0; combo = 0; bestCombo = 0; hearts = 5
        picked = null; answered = false; spellInput = ""
        phase = if (questions.isEmpty()) "setup" else "play"
    }

    fun answer(choice: String) {
        if (answered) return
        picked = choice; answered = true
        val ok = choice == questions[qi].answer
        if (ok) { score++; combo++; if (combo > bestCombo) bestCombo = combo }
        else { hearts--; combo = 0 }
        AppRepository.recordAnswer(questions[qi].word, ok)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("刷题闯关") },
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
                            onSelect = { bookId = it; Prefs.setActiveBook(ctx, it) }
                        )
                    } else {
                        EmptyHint("先导入词库或教材才能闯关")
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("出题方式", fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "zh2ru", onClick = { mode = "zh2ru" }, label = { Text("中→俄") })
                        FilterChip(selected = mode == "ru2zh", onClick = { mode = "ru2zh" }, label = { Text("俄→中") })
                        FilterChip(selected = mode == "spell", onClick = { mode = "spell" }, label = { Text("拼写") })
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("开始闯关（10 题）", Modifier.padding(start = 6.dp))
                    }
                    val best = Prefs.quizBest(ctx)
                    if (best > 0) {
                        Text(
                            "历史最佳：$best / 10",
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "play" -> {
                    val q = questions[qi]
                    LinearProgressIndicator(
                        progress = { qi.toFloat() / questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    if (i < hearts) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = RED,
                                    modifier = Modifier.size(18.dp).padding(end = 2.dp)
                                )
                            }
                        }
                        Text("连对 $combo 🔥 · $score/${questions.size}", fontWeight = FontWeight.Bold)
                    }

                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (q.audio.isNotBlank()) {
                                IconButton(onClick = { player.play(q.audio) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "播放发音", modifier = Modifier.size(32.dp))
                                }
                            }
                            Text(
                                q.prompt,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                if (mode == "spell") "请写出俄文" else "选择正确的释义 / 单词",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    if (mode == "spell") {
                        OutlinedTextField(
                            value = spellInput,
                            onValueChange = { spellInput = it; answered = false; picked = null },
                            label = { Text("输入俄文") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val ok = spellInput.lowercase().trim() == q.answer.lowercase().trim()
                                answered = true; picked = spellInput
                                if (ok) { score++; combo++; if (combo > bestCombo) bestCombo = combo }
                                else { hearts--; combo = 0 }
                                AppRepository.recordAnswer(q.answer, ok)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("检查") }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            q.options.forEach { opt ->
                                val isPicked = picked == opt
                                val showCorrect = answered && opt == q.answer
                                val showWrong = answered && isPicked && opt != q.answer
                                Surface(
                                    onClick = { answer(opt) },
                                    enabled = !answered,
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        showCorrect -> GREEN.copy(alpha = 0.15f)
                                        showWrong -> RED.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(opt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        if (showCorrect || showWrong) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = if (showCorrect) GREEN else RED,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (answered) {
                        Spacer(Modifier.height(12.dp))
                        val ok = if (mode == "spell") spellInput.lowercase().trim() == q.answer.lowercase().trim()
                        else picked == q.answer
                        Text(
                            if (ok) "✓ 正确！" else "✗ 正确答案：${q.answer}",
                            color = if (ok) GREEN else RED,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (hearts <= 0 || qi + 1 >= questions.size) {
                                    Prefs.setQuizBest(ctx, score)
                                    AppRepository.refreshStats()
                                    phase = "result"
                                } else {
                                    qi++; picked = null; answered = false; spellInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hearts <= 0 || qi + 1 >= questions.size) "查看成绩" else "继续")
                        }
                    }
                }

                "result" -> {
                    val total = questions.size
                    val acc = if (total == 0) 0 else score * 100 / total
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (score >= total * 0.8) "🎉 太棒了！" else if (score >= total / 2) "👍 不错！" else "💪 继续加油！",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("得分 $score / $total", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "正确率 $acc% · 最高连对 $bestCombo 🔥",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    "历史最佳 ${Prefs.quizBest(ctx)} / $total",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Text("再来一局", Modifier.padding(start = 6.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { phase = "setup" }, modifier = Modifier.fillMaxWidth()) {
                            Text("换词书 / 方式")
                        }
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
}
