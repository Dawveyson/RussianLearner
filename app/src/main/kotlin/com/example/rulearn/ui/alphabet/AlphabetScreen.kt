package com.example.rulearn.ui.alphabet

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rulearn.data.CYRILLIC_ALPHABET
import com.example.rulearn.data.LetterInfo
import com.example.rulearn.data.letterAsset
import com.example.rulearn.data.wordAsset
import com.example.rulearn.player.Speaker
import com.example.rulearn.ui.Route
import com.example.rulearn.ui.common.WordArt
import com.example.rulearn.ui.theme.GlassBox
import com.example.rulearn.ui.theme.GlassCard
import kotlin.random.Random

@Composable
fun AlphabetScreen(nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("字母表", "字母测验")

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
        }
        when (tab) {
            0 -> AlphabetTable(nav)
            else -> AlphabetQuiz()
        }
    }
}

@Composable
private fun AlphabetTable(nav: NavController) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<LetterInfo?>(CYRILLIC_ALPHABET.first()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 当前字母大卡
        selected?.let { l ->
            GlassBox(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WordArt(l.upper, Modifier.size(96.dp)) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "${l.upper}${l.lower}",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                // 大字用手写体（marckscript 含西里尔字形），之前用的是默认字体
                                fontFamily = HandwritingFont
                            )
                        }
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text("发音 ${l.sound}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${l.sampleRu} · ${l.sampleZh}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(Modifier.padding(top = 8.dp)) {
                            IconButton(onClick = { Speaker.playAsset(ctx, wordAsset(l), l.sampleRu) }) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "朗读例词", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { Speaker.playAsset(ctx, letterAsset(l), l.lower) }) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = "朗读字母", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { nav.navigate(Route.HANDWRITING) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("描红练习", Modifier.padding(start = 6.dp))
                }
                Button(
                    onClick = { Speaker.playAsset(ctx, wordAsset(l), l.sampleRu) },
                    modifier = Modifier.weight(1f)
                ) { Text("听例词") }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("全部 ${CYRILLIC_ALPHABET.size} 个字母", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CYRILLIC_ALPHABET, key = { it.upper }) { l ->
                val active = l.upper == selected?.upper
                GlassCard(
                    onClick = { selected = l },
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${l.upper} ${l.lower}",
                            fontSize = 20.sp,
                            fontFamily = HandwritingFont,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlphabetQuiz() {
    val ctx = LocalContext.current
    var question by remember { mutableStateOf(newQuestion()) }
    var picked by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(question.prompt, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    question.ask,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
                IconButton(onClick = { Speaker.playAsset(ctx, question.wordAsset, question.speak) }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "再听一次", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { opt ->
                        val showCorrect = answered && opt == question.answer
                        val showWrong = answered && picked == opt && opt != question.answer
                        Surface(
                            onClick = {
                                if (!answered) {
                                    picked = opt
                                    answered = true
                                    total++
                                    if (opt == question.answer) score++
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = when {
                                showCorrect -> Color(0xFF2E7D32).copy(alpha = 0.18f)
                                showWrong -> Color(0xFFC62828).copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(opt, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("本轮 $score / $total", color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (answered) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    question = newQuestion()
                    picked = null
                    answered = false
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("下一题") }
        }

        Spacer(Modifier.height(100.dp))
    }
}

private data class LetterQuestion(
    val prompt: String,
    val ask: String,
    val speak: String,
    val wordAsset: String,
    val options: List<String>,
    val answer: String
)

private fun newQuestion(): LetterQuestion {
    val correct = CYRILLIC_ALPHABET[Random.nextInt(CYRILLIC_ALPHABET.size)]
    val others = CYRILLIC_ALPHABET.filter { it.upper != correct.upper }.shuffled().take(3)
    val options = (others + correct).shuffled().map { "${it.upper} ${it.lower}" }
    return LetterQuestion(
        prompt = "「${correct.sound}」是哪个字母？",
        ask = correct.sampleRu,
        speak = correct.sampleRu,
        wordAsset = wordAsset(correct),
        options = options,
        answer = "${correct.upper} ${correct.lower}"
    )
}
