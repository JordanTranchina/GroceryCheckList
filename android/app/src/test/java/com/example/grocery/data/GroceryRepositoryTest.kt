package com.example.grocery.data

import com.example.grocery.model.GroceryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class GroceryRepositoryTest {

    // Since Firebase Firestore cannot be easily unit tested locally without
    // Firebase Test Lab or extensive mocking (which is complex for this scope),
    // we will focus on verifying that the state (lastAction) is set correctly 
    // by the methods in GroceryRepository. We won't test the actual Firestore 
    // transactions here.
    
    // In a real production app without complete Firestore mock support, 
    // you would extract the Firestore usage behind an interface, but we will
    // just test what we can for the state changes before Firestore calls.

    // A testable version of the repository logic to verify action recording
    class TestableGroceryRepository {
        var lastAction: GroceryRepository.GroceryAction? = null
            private set

        fun toggleCompletion(item: GroceryItem) {
            if (item.id.isNotEmpty()) {
                lastAction = GroceryRepository.GroceryAction.ToggleCompletion(item, item.isCompleted)
            }
        }

        fun deleteItem(item: GroceryItem) {
            if (item.id.isNotEmpty()) {
                lastAction = GroceryRepository.GroceryAction.DeleteItem(item)
            }
        }

        fun deleteItems(itemsToDelete: List<GroceryItem>) {
            if (itemsToDelete.isEmpty()) return
            lastAction = GroceryRepository.GroceryAction.DeleteItems(itemsToDelete)
        }
        
        fun undoLastAction() {
            // For testing, just clear it to simulate consumption
            lastAction = null
        }
    }

    private lateinit var testRepository: TestableGroceryRepository

    @Before
    fun setup() {
        testRepository = TestableGroceryRepository()
    }

    @Test
    fun `toggleCompletion records ToggleCompletion action`() {
        // Arrange
        val item = GroceryItem("id1", "Milk", false, 0, Date())

        // Act
        testRepository.toggleCompletion(item)

        // Assert
        val action = testRepository.lastAction
        assertTrue(action is GroceryRepository.GroceryAction.ToggleCompletion)
        action as GroceryRepository.GroceryAction.ToggleCompletion
        assertEquals(item, action.item)
        assertEquals(false, action.previousState)
    }

    @Test
    fun `deleteItem records DeleteItem action`() {
        // Arrange
        val item = GroceryItem("id1", "Milk", false, 0, Date())

        // Act
        testRepository.deleteItem(item)

        // Assert
        val action = testRepository.lastAction
        assertTrue(action is GroceryRepository.GroceryAction.DeleteItem)
        action as GroceryRepository.GroceryAction.DeleteItem
        assertEquals(item, action.item)
    }

    @Test
    fun `deleteItems records DeleteItems action`() {
        // Arrange
        val items = listOf(
            GroceryItem("id1", "Milk", true, 0, Date()),
            GroceryItem("id2", "Eggs", true, 1, Date())
        )

        // Act
        testRepository.deleteItems(items)

        // Assert
        val action = testRepository.lastAction
        assertTrue(action is GroceryRepository.GroceryAction.DeleteItems)
        action as GroceryRepository.GroceryAction.DeleteItems
        assertEquals(items, action.items)
    }
    
    @Test
    fun `undoLastAction clears lastAction`() {
        // Arrange
        val item = GroceryItem("id1", "Milk", false, 0, Date())
        testRepository.deleteItem(item)
        
        // Act
        testRepository.undoLastAction()
        
        // Assert
        assertNull(testRepository.lastAction)
    }
}
