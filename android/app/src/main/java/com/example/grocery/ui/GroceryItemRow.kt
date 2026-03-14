package com.example.grocery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocery.util.PasteUtils
import com.example.grocery.model.GroceryItem

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.delay

@Composable
fun GroceryItemRow(
    item: GroceryItem,
    onToggle: (GroceryItem) -> Unit,
    onDelete: (GroceryItem) -> Unit,
    onNameChange: (GroceryItem, String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddNewItem: () -> Unit = {},
    onAddMultipleItems: (List<String>) -> Unit = {},
    focusRequester: FocusRequester? = null,
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

        // Larger touch target + animated feedback for checking items
        val checkScale by animateFloatAsState(targetValue = if (item.isCompleted) 0.92f else 1f)
        val highlightColor by animateColorAsState(
            targetValue = if (item.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent
        )

        Box(
            modifier = Modifier
                .padding(start = 4.dp, end = 8.dp)
                .size(48.dp)
                .background(highlightColor, shape = CircleShape)
                .scale(checkScale)
                .semantics { contentDescription = "Toggle completion" }
                .clickable { onToggle(item) },
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle(item) },
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    checkmarkColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.size(24.dp)
            )
        }

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

        val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

        androidx.compose.foundation.text.BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val result = PasteUtils.processInput(textFieldValue.text, newValue.text)
                if (result.newItems.isNotEmpty()) {
                    textFieldValue = newValue.copy(text = result.updatedCurrentText)
                    onAddMultipleItems(result.newItems)
                } else {
                    textFieldValue = newValue
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                       else MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { 
                    onAddNewItem() 
                }
            ),
            modifier = Modifier
                .weight(1f)
                .then(focusModifier)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace && textFieldValue.text.isEmpty()) {
                         onDelete(item)
                         true
                    } else {
                         false
                    }
                },
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

        // Section tag — only shown when a section has been assigned (not empty or OTHER)
        val sectionLabel = sectionDisplayLabel(item.section)
        if (sectionLabel != null) {
            Spacer(modifier = Modifier.width(6.dp))
            SectionTag(label = sectionLabel, section = item.section)
        }

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

/** Returns a human-readable label for the section, or null if it shouldn't show a tag. */
private fun sectionDisplayLabel(section: String): String? = when (section.uppercase()) {
    "BREAD"        -> "Bread"
    "PRODUCE"      -> "Produce"
    "DAIRY_SNACKS" -> "Dairy & Snacks"
    "MEAT"         -> "Meat"
    "FROZEN"       -> "Frozen"
    "CHEESE"       -> "Cheese"
    "ALCOHOL"      -> "Alcohol"
    else           -> null  // "OTHER" and empty string show no tag
}

/** Colors associated with each grocery section. */
private fun sectionColors(section: String): Pair<Color, Color> = when (section.uppercase()) {
    "BREAD"        -> Pair(Color(0xFFF5A623), Color.White)
    "PRODUCE"      -> Pair(Color(0xFF43A047), Color.White)
    "DAIRY_SNACKS" -> Pair(Color(0xFF29B6F6), Color.White)
    "MEAT"         -> Pair(Color(0xFFEF5350), Color.White)
    "FROZEN"       -> Pair(Color(0xFF00BCD4), Color.White)
    "CHEESE"       -> Pair(Color(0xFFFDD835), Color(0xFF424242))
    "ALCOHOL"      -> Pair(Color(0xFF7B1FA2), Color.White)
    else           -> Pair(Color(0xFF9E9E9E), Color.White)
}

@Composable
fun SectionTag(label: String, section: String) {
    val (bgColor, textColor) = sectionColors(section)
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
