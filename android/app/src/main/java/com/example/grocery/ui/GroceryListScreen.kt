package com.example.grocery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.grocery.R
import com.example.grocery.data.GroceryRepository
import com.example.grocery.model.GroceryItem
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroceryListScreen(
    repository: GroceryRepository,
    modifier: Modifier = Modifier
) {
    val items by repository.items.collectAsState(initial = emptyList())
    var newItemName by remember { mutableStateOf("") }
    
    // Sort: Unchecked first (by order), then Checked
    var activeItems by remember { mutableStateOf<List<GroceryItem>>(emptyList()) }
    
    val currentItems = items.filter { !it.isCompleted }.sortedBy { it.order }
    
    androidx.compose.runtime.LaunchedEffect(currentItems) {
        activeItems = currentItems
    }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            // FIX: Ensure we stay within bounds of activeItems
            val fromIndex = from.index
            val toIndex = to.index
            
            if (fromIndex in activeItems.indices && toIndex in activeItems.indices) {
                activeItems = activeItems.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
            }
        },
        onDragEnd = { _, _ ->
            repository.updateOrders(activeItems)
        }
    )

    val completedItems = items.filter { it.isCompleted }.sortedBy { it.order }
    
    var isCompletedExpanded by remember { mutableStateOf(true) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = state.listState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .reorderable(state)
        ) {
            // Active Items Section
            items(activeItems, key = { it.id }) { item ->
                ReorderableItem(state, key = item.id) { isDragging ->
                    GroceryItemRow(
                        item = item,
                        onToggle = { repository.toggleCompletion(it) },
                        onDelete = { 
                            repository.deleteItem(it)
                            activeItems = activeItems.filter { item -> item.id != it.id }
                            if (selectedItemId == it.id) selectedItemId = null
                        },
                        onNameChange = { item, newName -> 
                            repository.updateName(item, newName)
                        },
                        isSelected = item.id == selectedItemId,
                        onSelect = { 
                            selectedItemId = if (selectedItemId == item.id) null else item.id 
                        },
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(vertical = if(isDragging) 4.dp else 0.dp), // add minimal spacing during drag
                        dragModifier = Modifier.detectReorderAfterLongPress(state)
                    )
                }
            }

            // Inline Input Row
            item {
                InputRow(
                    text = newItemName,
                    onTextChange = { newItemName = it },
                    onAdd = {
                        if (newItemName.isNotEmpty()) {
                            repository.addItem(newItemName)
                            newItemName = ""
                        }
                    }
                )
            }

            // Completed Items Section header
            if (completedItems.isNotEmpty()) {
                item {
                    val count = completedItems.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCompletedExpanded = !isCompletedExpanded }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$count Checked item${if(count > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Completed Items List
            if (isCompletedExpanded) {
                items(completedItems, key = { it.id }) { item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                         GroceryItemRow(
                            item = item,
                            onToggle = { repository.toggleCompletion(it) },
                            onDelete = { 
                                repository.deleteItem(it)
                                if (selectedItemId == it.id) selectedItemId = null
                            },
                            onNameChange = { item, newName -> 
                                repository.updateName(item, newName)
                            },
                            isSelected = item.id == selectedItemId,
                            onSelect = { selectedItemId = item.id },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputRow(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Plus Icon aligned with Drag Handle/Checkbox area
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Item",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, end = 24.dp) 
        )

        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("List item") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, // No underline
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAdd() }
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
