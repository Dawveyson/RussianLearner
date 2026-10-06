package com.example.rulearn.ui.words

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rulearn.data.WordBook
import com.example.rulearn.ui.theme.GlassCard

/**
 * 词书选择器。
 *
 * 之前用 ExposedDropdownMenuBox + 普通 DropdownMenu，菜单锚点没对齐，点击后弹不出来
 * （"选择词书"点不开）。这里改为 Box 锚定 + 自绘玻璃卡片：锚点明确、点击区域更大，
 * 也不会被滚动容器裁掉。
 */
@Composable
fun BookPicker(
    books: List<WordBook>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val cur = books.firstOrNull { it.id == selectedId } ?: books.firstOrNull()

    Box(modifier) {
        GlassCard(
            onClick = { if (books.isNotEmpty()) expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "选择词书",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        cur?.name ?: "暂无词书",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (cur != null) {
                        Text(
                            "${cur.entries.size} 条词条",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "展开",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            books.forEach { b ->
                val active = b.id == cur?.id
                DropdownMenuItem(
                    text = {
                        Text(
                            "${b.name} · ${b.entries.size} 条",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (active) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "已选择",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onClick = {
                        onSelect(b.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
