package com.example.rulearn.ui.words

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.rulearn.data.WordBook

/** 词书下拉选择器。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPicker(
    books: List<WordBook>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val cur = books.firstOrNull { it.id == selectedId } ?: books.firstOrNull()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = cur?.name ?: "暂无词书",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("选择词书") },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, expanded)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            books.forEach { b ->
                DropdownMenuItem(
                    text = {
                        Text("${b.name} · ${b.entries.size} 条", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = { onSelect(b.id); expanded = false }
                )
            }
        }
    }
}
