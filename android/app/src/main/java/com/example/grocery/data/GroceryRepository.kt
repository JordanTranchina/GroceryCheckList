package com.example.grocery.data

import com.example.grocery.model.GroceryItem

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.UUID

// Mock Repository until Firebase is connected
class GroceryRepository {
    private val _items = MutableStateFlow<List<GroceryItem>>(emptyList())
    val items: StateFlow<List<GroceryItem>> = _items.asStateFlow()

    init {
        // Seed mock data
        _items.value = listOf(
            GroceryItem(id = UUID.randomUUID().toString(), name = "Milk", order = 0),
            GroceryItem(id = UUID.randomUUID().toString(), name = "Eggs", order = 1),
            GroceryItem(id = UUID.randomUUID().toString(), name = "Coffee", order = 2),
            GroceryItem(id = UUID.randomUUID().toString(), name = "Apples", isCompleted = true, order = 3)
        )
    }

    fun addItem(name: String) {
        val currentList = _items.value.toMutableList()
        val newItem = GroceryItem(
            id = UUID.randomUUID().toString(),
            name = name,
            order = currentList.size // Append to end
        )
        currentList.add(newItem)
        _items.value = currentList
    }

    fun toggleCompletion(item: GroceryItem) {
        val currentList = _items.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentList[index] = item.copy(isCompleted = !item.isCompleted)
            _items.value = currentList
        }
    }
}
