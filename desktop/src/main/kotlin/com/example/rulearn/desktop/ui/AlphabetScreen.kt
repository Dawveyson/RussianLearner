package com.example.rulearn.desktop.ui
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rulearn.CYRILLIC_ALPHABET
import com.example.rulearn.desktop.player.DesktopPlayer
import com.example.rulearn.letterAsset
import com.example.rulearn.wordAsset

@Composable
fun AlphabetScreen(player: DesktopPlayer) {
    var selected by remember { mutableStateOf(CYRILLIC_ALPHABET.first()) }

    ScreenScroll {
        SectionTitle("西里尔字母表")
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(CYRILLIC_ALPHABET) { l ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { selected = l }) {
                    Column(Modifier.padding(8.dp), Arrangement.Center) {
                        Text(l.upper, style = MaterialTheme.typography.titleLarge)
                        Text(l.lower, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(16)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${selected.upper} ${selected.lower}", style = MaterialTheme.typography.headlineSmall)
                        Text("读音：${selected.sound}", style = MaterialTheme.typography.bodyLarge)
                        Text("例词：${selected.sampleRu} · ${selected.sampleZh}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row {
                        IconButton(onClick = { player.play(letterAsset(selected)) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "读字母")
                        }
                        IconButton(onClick = { player.play(wordAsset(selected)) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "读例词")
                        }
                    }
                }
            }
        }
        Text("注：字母/例词发音为内置离线音频，首次播放需要对应资源已打入安装包。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Spacer(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.height(dp.dp))
