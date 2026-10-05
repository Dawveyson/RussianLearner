package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.data.AppRepository
import com.example.rulearn.nav.Route

@Composable
fun HomeScreen(navigate: (String) -> Unit) {
    val stats by AppRepository.stats.collectAsState()
    val daily = AppRepository.dailyWord()

    ScreenScroll {
        Text("今日概览", style = MaterialTheme.typography.headlineSmall)
        Spacer(8)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("总词数", "${stats.totalWords}", Modifier.weight(1f))
            StatCard("已掌握", "${stats.mastered}", Modifier.weight(1f))
            StatCard("待复习", "${stats.dueToday}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("连续天数", "${stats.streak}", Modifier.weight(1f))
            StatCard("今日已学", "${stats.learnedToday}", Modifier.weight(1f))
            StatCard("拼写正确率", "${stats.spellingAcc}%", Modifier.weight(1f))
        }

        Spacer(16)
        SectionTitle("每日一句")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text(daily.first, style = MaterialTheme.typography.titleLarge)
                Text(daily.second, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(16)
        SectionTitle("开始学习")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton("拼写练习", Route.SPELLING, navigate, Modifier.weight(1f))
            ActionButton("闯关测验", Route.QUIZ, navigate, Modifier.weight(1f))
        }
        Spacer(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton("随身听", Route.LISTEN, navigate, Modifier.weight(1f))
            ActionButton("学习资源", Route.WORDS, navigate, Modifier.weight(1f))
        }

        if (stats.totalWords == 0) {
            Spacer(16)
            Text(
                "还没有词库：进入「学习资源」导入词库或课文，即可开始练习。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionButton(label: String, route: String, navigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = { navigate(route) }, modifier = modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
