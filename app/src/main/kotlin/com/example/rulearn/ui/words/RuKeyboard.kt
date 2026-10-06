package com.example.rulearn.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 内置俄语软键盘（ЙЦУКЕН）。
 *
 * 做成"真键盘"外观：顶部字母区 + 底部 Shift/退格/空格/回车功能行。
 * 调用方应把它放在 Scaffold 的 bottomBar 或固定定位的容器里，
 * 这样它就固定在屏幕下半部，不会随内容滚动。
 */
@Composable
fun RuSoftKeyboard(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: (() -> Unit)? = null,
    shiftOn: Boolean = false
) {
    val rows = listOf(
        "йцукенгшщзхъ".toList(),
        "фывапролдэ".toList(),
        "ячсмитьбюё".toList()
    )
    val keyShape = RoundedCornerShape(7.dp)
    val keyBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val funcBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { ch ->
                    val display = (if (shiftOn) ch.uppercase() else ch).toString()
                    Box(
                        Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(keyShape)
                            .background(keyBg)
                            .clickable { onChar(display) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(display, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                Modifier
                    .weight(1.4f)
                    .height(40.dp)
                    .clip(keyShape)
                    .background(funcBg),
                contentAlignment = Alignment.Center
            ) {
                Text("Shift", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .weight(1.2f)
                    .height(40.dp)
                    .clip(keyShape)
                    .background(funcBg)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "退格",
                    modifier = Modifier.height(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .weight(3f)
                    .height(40.dp)
                    .clip(keyShape)
                    .background(funcBg)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text("空格", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .weight(1.4f)
                    .height(40.dp)
                    .clip(keyShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                    .clickable { onEnter?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Text("完成", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
