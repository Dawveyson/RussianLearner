package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.ImportSpec
import com.example.rulearn.data.AppRepository

@Composable
fun MeScreen() {
    val stats by AppRepository.stats.collectAsState()
    val prefs = AppRepository.prefs
    var goal by remember { mutableStateOf(prefs.dailyGoal().toString()) }
    var netTts by remember { mutableStateOf(prefs.preferNetworkTts()) }
    var apiKey by remember { mutableStateOf(prefs.apiKey()) }
    var apiSecret by remember { mutableStateOf(prefs.apiSecret()) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("学习统计")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("总词数", "${stats.totalWords}")
            StatCard("已掌握", "${stats.mastered}")
            StatTitle("连续天数", "${stats.streak}")
        }

        SectionTitle("设置")
        OutlinedTextField(value = goal, onValueChange = {
            goal = it
            it.toIntOrNull()?.let { v -> prefs.setDailyGoal(v) }
        }, label = { Text("每日目标（词）") }, modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("优先网络发音（离线音频缺失时）", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = netTts, onCheckedChange = { netTts = it; prefs.setPreferNetworkTts(it) })
        }

        OutlinedTextField(value = apiKey, onValueChange = {
            apiKey = it; prefs.setApiKey(it)
        }, label = { Text("有道 appKey（可选）") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = apiSecret, onValueChange = {
            apiSecret = it; prefs.setApiSecret(it)
        }, label = { Text("有道 appSecret（可选）") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        Button(onClick = { AppRepository.resetProgress() }, modifier = Modifier.padding(top = 12.dp)) { Text("重置学习进度") }

        SectionTitle("导入格式说明")
        Card(Modifier.fillMaxWidth()) {
            Text(ImportSpec.VOCAB_SPEC, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(8)
        Card(Modifier.fillMaxWidth()) {
            Text(ImportSpec.ZIP_SPEC, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth(1f / 3f), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(12.dp), Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatTitle(label: String, value: String) {
    Card(Modifier.fillMaxWidth(1f / 3f)) {
        Column(Modifier.padding(12.dp), Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
