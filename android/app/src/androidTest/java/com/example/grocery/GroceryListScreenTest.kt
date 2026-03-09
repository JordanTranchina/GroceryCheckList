package com.example.grocery

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.grocery.data.GroceryRepository
import com.example.grocery.model.GroceryItem
import com.example.grocery.ui.GroceryListScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.util.Date

class GroceryListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deleteAllCheckedItems_callsRepositoryDeleteItems() {
        // Arrange
        val repository = mockk<GroceryRepository>(relaxed = true)

        val activeItem = GroceryItem("id1", "Milk", false, 0, Date())
        val completedItem1 = GroceryItem("id2", "Eggs", true, 1, Date())
        val completedItem2 = GroceryItem("id3", "Bread", true, 2, Date())

        val mockItems = listOf(activeItem, completedItem1, completedItem2)

        every { repository.items } returns flowOf(mockItems)

        composeTestRule.setContent {
            GroceryListScreen(repository = repository)
        }

        // Act
        // 1. Click the 3-dot menu icon
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // 2. Click the "Delete all checked items" dropdown item
        composeTestRule.onNodeWithText("Delete all checked items").performClick()

        // Assert
        // Verify that deleteItems was called with only the completed items
        verify {
            repository.deleteItems(match { items ->
                items.size == 2 && items.containsAll(listOf(completedItem1, completedItem2))
            })
        }
    }
}
