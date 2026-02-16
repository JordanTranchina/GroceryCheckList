package com.example.grocery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.grocery.model.GroceryItem
import com.example.grocery.ui.GroceryItemRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

class GroceryItemRowCheckboxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkbox_hasLargeTouchTarget_and_callsOnToggle() {
        val item = GroceryItem("id1", "Milk", false, 0, Date())
        var toggled = false

        composeTestRule.setContent {
            GroceryItemRow(
                item = item,
                onToggle = { toggled = true },
                onDelete = {},
                onNameChange = { _, _ -> },
                isSelected = false,
                onSelect = {}
            )
        }

        val node = composeTestRule.onNodeWithContentDescription("Toggle completion")
        node.assertIsDisplayed()
        node.assertWidthIsAtLeast(48.dp)
        node.assertHeightIsAtLeast(48.dp)
        node.performClick()

        assertTrue("onToggle should have been invoked", toggled)
    }
}
