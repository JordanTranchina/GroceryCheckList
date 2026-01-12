package com.example.grocery.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.grocery.R
import com.example.grocery.data.GroceryRepository
import com.example.grocery.model.GroceryItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroceryListScreen(
    repository: GroceryRepository,
    modifier: Modifier = Modifier
) {
    val items by repository.items.collectAsState()
    
    // Sort: Unchecked first (by order), then Checked
    val activeItems = items.filter { !it.isCompleted }.sortedBy { it.order }
    val completedItems = items.filter { it.isCompleted }.sortedBy { it.order }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { repository.addItem("New Item ${items.size + 1}") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Items Section
            if (activeItems.isNotEmpty()) {
                stickyHeader {
                    SectionHeader("To Buy")
                }
                items(activeItems, key = { it.id }) { item ->
                    GroceryItemRow(
                        item = item,
                        onToggle = { repository.toggleCompletion(it) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }

            // Completed Items Section ("Below the fold")
            if (completedItems.isNotEmpty()) {
                stickyHeader {
                    SectionHeader("Completed")
                }
                items(completedItems, key = { it.id }) { item ->
                    GroceryItemRow(
                        item = item,
                        onToggle = { repository.toggleCompletion(it) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
