package com.example.rulearn.ui.me

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
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
import com.example.rulearn.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val stats by AppRepository.stats.collectAsStateWithLifecycle()

    var goalText by remember { mutableStateOf(stats.dailyGoal.toString()) }
    var goalMsg by remember { mutableStateOf("") }

    var key by remember { mutableStateOf(Prefs.apiKey(ctx)) }
    var secret by remember { mutableStateOf(Prefs.apiSecret(ctx)) }
    var keyMsg by remember { mutableStateOf("") }

    var preferNet by remember { mutableStateOf(Prefs.preferNetworkTts(ctx)) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("每日目标", fontWeight = FontWeight.Bold)
                    Text(
                        "首页进度环按这个数字计算今日学习进度（5–200）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        goalText,
                        { goalText = it.filter(Char::isDigit) },
                        label = { Text("每天学多少个词") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = {
                        val v = goalText.toIntOrNull() ?: 20
                        Prefs.setDailyGoal(ctx, v)
                        AppRepository.refreshStats()
                        goalMsg = "已保存：每天 $v 个"
                    }) { Text("保存") }
                    if (goalMsg.isNotBlank()) {
                        Text(goalMsg, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("在线词典（有道智云 · 国内可访问）", fontWeight = FontWeight.Bold)
                    Text(
                        "不填也能用：内置演示接口。填入免费申请的 appKey / appSecret 后更稳定、额度更高。申请：ai.youdao.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(key, { key = it }, label = { Text("appKey") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(secret, { secret = it }, label = { Text("appSecret") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        Prefs.setApiKey(ctx, key)
                        Prefs.setApiSecret(ctx, secret)
                        keyMsg = "已保存"
                    }) { Text("保存密钥") }
                    if (keyMsg.isNotBlank()) Text(keyMsg, color = MaterialTheme.colorScheme.primary)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("发音", fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("优先使用网络发音（有道）", modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferNet,
                            onCheckedChange = { preferNet = it; Prefs.setPreferNetworkTts(ctx, it) }
                        )
                    }
                    Text(
                        "系统未装俄语语音包时会自动改用网络发音。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching { ctx.startActivity(Intent("android.speech.tts.action.INSTALL_TTS_DATA")) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("下载 / 安装俄语语音包")
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("学习数据", fontWeight = FontWeight.Bold)
                    Text(
                        "已掌握 ${stats.mastered} 个词 · 连续 ${stats.streak} 天 · 今日已学 ${stats.learnedToday}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("清除全部掌握度记录") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    "Made by Davey With Heart💗 · VibeCoding",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("清除掌握度？") },
            text = { Text("所有单词的等级与复习时间会被清空，词库本身不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.resetProgress()
                    confirmReset = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            }
        )
    }
}
