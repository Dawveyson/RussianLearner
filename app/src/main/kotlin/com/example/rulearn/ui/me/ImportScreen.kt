package com.example.rulearn.ui.me

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rulearn.data.AppRepository
import com.example.rulearn.data.ImportSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Mono = FontFamily.Monospace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var msg by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var bookName by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }

    val vocabPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val name = AppRepository.importBook(it)
            // 回调捕获的 vocab 是导入前的旧列表，这里必须读 reload 之后的实时值
            msg = if (name != null) {
                "已导入词库「$name」，现有 ${AppRepository.vocabBooks.value.size} 本"
            } else "导入失败：文件为空或格式不符"
        }
    }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val n = AppRepository.importLessonZip(it)
            msg = if (n > 0) {
                "已导入 $n 课，现有 ${AppRepository.lessons.value.size} 课"
            } else "导入失败：包里没有找到清单 JSON（book.json / lessons.json / manifest.json / index.json）"
        }
    }
    val lessonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val n = AppRepository.importLessons(it)
            msg = if (n > 0) {
                "已导入 $n 课，现有 ${AppRepository.lessons.value.size} 课"
            } else "导入失败：不是有效的课程 JSON"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入 / 下载资源") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (msg.isNotBlank()) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 词库导入
            ImportBlock(
                title = "导入词库（本地文件）",
                desc = "选择 .json / .tsv / .txt / .csv 文件，会成为一本新词书。",
                buttonText = "选择词库文件"
            ) {
                vocabPicker.launch(
                    arrayOf("application/json", "text/plain", "text/tab-separated-values", "text/comma-separated-values", "*/*")
                )
            }

            // 词库下载
            com.example.rulearn.ui.theme.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("下载词库（按 URL）", fontWeight = FontWeight.Bold)
                    Text(
                        "填入放在 Gitee / 任意国内可访问托管上的词库文件地址。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(url, { url = it }, label = { Text("词库文件 URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(bookName, { bookName = it }, label = { Text("词书名（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(
                        enabled = !downloading && url.isNotBlank(),
                        onClick = {
                            downloading = true
                            msg = "下载中…"
                            scope.launch {
                                val name = withContext(Dispatchers.IO) {
                                    AppRepository.downloadBook(url.trim(), bookName.ifBlank { null })
                                }
                                downloading = false
                                msg = if (name != null) "已添加词书：$name" else "下载失败（地址不可访问或格式不符）"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Text("下载词库", Modifier.padding(start = 6.dp))
                    }
                }
            }

            // zip 音频书（推荐）
            ImportBlock(
                title = "导入 zip 音频书（推荐）",
                desc = "清单 + 音频打成一个包，解压后自动接好音频路径。",
                buttonText = "选择 zip 包"
            ) {
                zipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }

            // 单独的课程 JSON（不带音频包）
            ImportBlock(
                title = "导入课程 JSON（不带音频包）",
                desc = "只有课文和可选外链音频时用这个，本地音频请打包成 zip。",
                buttonText = "选择课程 JSON"
            ) {
                lessonPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
            }

            // AI 转换指南
            com.example.rulearn.ui.theme.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("用 AI 转换你的材料", fontWeight = FontWeight.Bold)
                    Text(
                        "把课本照片 / PDF 复制文本 / 单词表，连同下面任一指令发给 ChatGPT、Claude、Gemini、文心、通义、Kimi、DeepSeek 等任意 AI，把返回结果存成 .json 即可导入。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CopyableBlock("① 词库 → JSON（最常用）", ImportSpec.AI_VOCAB_JSON, ctx)
                    CopyableBlock("② 词库 → TSV 纯文本", ImportSpec.AI_VOCAB_TSV, ctx)
                    CopyableBlock("③ 课文 + 音频时间轴 → 课程 JSON", ImportSpec.AI_LESSON, ctx)
                    CopyableBlock("④ 怎么把清单和音频打成 zip", ImportSpec.AI_ZIP_TIP, ctx)
                }
            }

            // 格式规范
            SpecBlock("词库格式规范", ImportSpec.VOCAB_SPEC)
            SpecBlock("zip 音频书打包规范（推荐）", ImportSpec.ZIP_SPEC)
            SpecBlock("教材 JSON 格式规范", ImportSpec.LESSON_SPEC)
            SpecBlock("音频同步怎么做", ImportSpec.AI_AUDIO_TIP)

            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 80.dp))
        }
    }
}

@Composable
private fun ImportBlock(
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit
) {
    com.example.rulearn.ui.theme.GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Text(
                buttonText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CopyableBlock(title: String, text: String, ctx: Context) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
                Toast.makeText(ctx, "已复制指令", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { open = !open }) {
                Icon(
                    if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (open) "收起" else "展开"
                )
            }
        }
        if (open) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text.trimIndent(),
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecBlock(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    com.example.rulearn.ui.theme.GlassCard(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            if (open) {
                Text(
                    text.trimIndent(),
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}
