package com.example.rulearn.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rulearn.data.RU_KEYBOARD_ROWS

/**
 * 内置俄文（ЙЦУКЕН）软键盘，避免切换系统输入法。
 *
 * 基础三键（字母 / 退格 / 空格）始终存在；查词页只用这些。
 * 拼写练习可额外传入 [onEnter]（完成）与 [onShiftClick]/[shiftOn]（Shift）
 * 来显示对应的功能键，从而一套实现覆盖两种场景，避免重复键盘。
 */
@Composable
fun RuKeyboard(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: (() -> Unit)? = null,
    onShiftClick: (() -> Unit)? = null,
    shiftOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RU_KEYBOARD_ROWS.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val weight = 1f / row.size
                    row.forEach { ch ->
                        val display = if (shiftOn) ch.uppercase() else ch
                        KeyButton(display, Modifier.weight(weight)) { onChar(display) }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onShiftClick != null) {
                    KeyButton(
                        "Shift",
                        Modifier.weight(1.2f),
                        highlight = shiftOn
                    ) { onShiftClick() }
                }
                KeyButton("⌫", Modifier.weight(1.2f), icon = {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "退格", modifier = Modifier.size(18.dp))
                }) { onBackspace() }
                KeyButton("空格", Modifier.weight(3f)) { onSpace() }
                if (onEnter != null) {
                    KeyButton("完成", Modifier.weight(1.4f), highlight = true) { onEnter() }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) icon()
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
