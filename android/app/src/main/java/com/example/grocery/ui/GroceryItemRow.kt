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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.grocery.model.GroceryItem

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    onToggle: (GroceryItem) -> Unit,
    onDelete: (GroceryItem) -> Unit,
    onNameChange: (GroceryItem, String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddNext: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // Local state for immediate visual feedback
    var isChecked by remember(item.isCompleted) { mutableStateOf(item.isCompleted) }

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
            checked = isChecked,
            onCheckedChange = { checked ->
                isChecked = checked
                // Delay the actual list move/database update
                scope.launch {
                    delay(300) 
                    onToggle(item)
                }
            },
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Editable text field (BasicTextField)
        androidx.compose.foundation.text.BasicTextField(
            value = item.name,
            onValueChange = { onNameChange(item, it) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                       else MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onAddNext() } 
            ),
            modifier = Modifier
                .weight(1f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                        onAddNext()
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                 if (item.name.isEmpty()) {
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
