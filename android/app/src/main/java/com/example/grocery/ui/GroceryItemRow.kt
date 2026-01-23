package com.example.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocery.model.GroceryItem

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    onToggle: (GroceryItem) -> Unit,
    onDelete: (GroceryItem) -> Unit,
    onNameChange: (GroceryItem, String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle
        Icon(
            imageVector = Icons.Default.Menu, 
            contentDescription = "Drag Handle",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = dragModifier.padding(start = 8.dp, end = 8.dp)
        )

        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle(item) },
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Editable text field (BasicTextField) with local state for debouncing
        var textFieldValue by remember(item.id) { mutableStateOf(TextFieldValue(item.name)) }
        var isFocused by remember { mutableStateOf(false) }

        // Sync from upstream if it changes externally AND we are not focused
        LaunchedEffect(item.name) {
            if (!isFocused && textFieldValue.text != item.name) {
                textFieldValue = textFieldValue.copy(text = item.name)
            }
        }

        // Debounced save
        LaunchedEffect(textFieldValue.text) {
             if (textFieldValue.text != item.name) {
                 delay(500) // 500ms debounce
                 onNameChange(item, textFieldValue.text)
             }
        }

        androidx.compose.foundation.text.BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                       else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused },
            decorationBox = { innerTextField ->
                 if (item.name.isEmpty() && !isFocused) {
                     Text(
                         text = "List item",
                         style = MaterialTheme.typography.bodyLarge.copy(
                             fontSize = 16.sp,
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                         )
                     )
                 }
                 innerTextField()
            }
        )

        if (isSelected) {
            androidx.compose.material3.IconButton(onClick = { onDelete(item) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
