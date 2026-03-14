package com.example.grocery.util

import com.example.grocery.model.GroceryItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class AisleSorterTest {

    private fun item(id: String, name: String, order: Int = 0) =
        GroceryItem(id, name, false, order, Date())

    @Test
    fun `getSectionFor maps bread items correctly`() {
        assertEquals(AisleSorter.Section.BREAD, AisleSorter.getSectionFor("Sourdough bread"))
        assertEquals(AisleSorter.Section.BREAD, AisleSorter.getSectionFor("Bagels"))
        assertEquals(AisleSorter.Section.BREAD, AisleSorter.getSectionFor("Flour tortillas"))
    }

    @Test
    fun `getSectionFor maps produce items correctly`() {
        assertEquals(AisleSorter.Section.PRODUCE, AisleSorter.getSectionFor("Bananas"))
        assertEquals(AisleSorter.Section.PRODUCE, AisleSorter.getSectionFor("Spinach"))
        assertEquals(AisleSorter.Section.PRODUCE, AisleSorter.getSectionFor("Red pepper"))
    }

    @Test
    fun `getSectionFor maps dairy and snack items correctly`() {
        assertEquals(AisleSorter.Section.DAIRY_SNACKS, AisleSorter.getSectionFor("Whole milk"))
        assertEquals(AisleSorter.Section.DAIRY_SNACKS, AisleSorter.getSectionFor("Almond milk"))
        assertEquals(AisleSorter.Section.DAIRY_SNACKS, AisleSorter.getSectionFor("Potato chips"))
        assertEquals(AisleSorter.Section.DAIRY_SNACKS, AisleSorter.getSectionFor("Greek yogurt"))
    }

    @Test
    fun `getSectionFor maps meat items correctly`() {
        assertEquals(AisleSorter.Section.MEAT, AisleSorter.getSectionFor("Chicken breast"))
        assertEquals(AisleSorter.Section.MEAT, AisleSorter.getSectionFor("Ground beef"))
        assertEquals(AisleSorter.Section.MEAT, AisleSorter.getSectionFor("Bacon"))
    }

    @Test
    fun `getSectionFor maps frozen items correctly`() {
        assertEquals(AisleSorter.Section.FROZEN, AisleSorter.getSectionFor("Frozen peas"))
        assertEquals(AisleSorter.Section.FROZEN, AisleSorter.getSectionFor("Ice cream"))
    }

    @Test
    fun `getSectionFor maps cheese items correctly`() {
        assertEquals(AisleSorter.Section.CHEESE, AisleSorter.getSectionFor("Cheddar"))
        assertEquals(AisleSorter.Section.CHEESE, AisleSorter.getSectionFor("Mozzarella"))
        assertEquals(AisleSorter.Section.CHEESE, AisleSorter.getSectionFor("Cream cheese"))
    }

    @Test
    fun `getSectionFor maps alcohol items correctly`() {
        assertEquals(AisleSorter.Section.ALCOHOL, AisleSorter.getSectionFor("Red wine"))
        assertEquals(AisleSorter.Section.ALCOHOL, AisleSorter.getSectionFor("Beer"))
        assertEquals(AisleSorter.Section.ALCOHOL, AisleSorter.getSectionFor("Vodka"))
    }

    @Test
    fun `getSectionFor returns OTHER for unrecognized items`() {
        assertEquals(AisleSorter.Section.OTHER, AisleSorter.getSectionFor("Detergent"))
        assertEquals(AisleSorter.Section.OTHER, AisleSorter.getSectionFor("Napkins"))
    }

    @Test
    fun `sortItems orders items by section priority`() {
        val items = listOf(
            item("1", "Beer"),
            item("2", "Bananas"),
            item("3", "Sourdough bread"),
            item("4", "Chicken breast"),
            item("5", "Whole milk")
        )

        val sorted = AisleSorter.sortItems(items)

        assertEquals("Sourdough bread", sorted[0].name) // Bread = 0
        assertEquals("Bananas", sorted[1].name)          // Produce = 1
        assertEquals("Whole milk", sorted[2].name)       // Dairy/Snacks = 2
        assertEquals("Chicken breast", sorted[3].name)   // Meat = 3
        assertEquals("Beer", sorted[4].name)             // Alcohol = 6
    }

    @Test
    fun `sortItems appends uncategorized items at the bottom preserving relative order`() {
        val items = listOf(
            item("1", "Detergent"),
            item("2", "Napkins"),
            item("3", "Bananas")
        )

        val sorted = AisleSorter.sortItems(items)

        assertEquals("Bananas", sorted[0].name)
        assertEquals("Detergent", sorted[1].name)
        assertEquals("Napkins", sorted[2].name)
    }

    @Test
    fun `sortItems preserves relative order within the same section`() {
        val items = listOf(
            item("1", "Green grapes"),
            item("2", "Spinach"),
            item("3", "Avocado")
        )

        val sorted = AisleSorter.sortItems(items)

        // All produce — original order preserved
        assertEquals("Green grapes", sorted[0].name)
        assertEquals("Spinach", sorted[1].name)
        assertEquals("Avocado", sorted[2].name)
    }

    @Test
    fun `cheese maps to CHEESE section not DAIRY_SNACKS`() {
        // "Cream cheese" contains "cream" (dairy keyword) and "cheese" (cheese keyword)
        // cheese check must win
        assertEquals(AisleSorter.Section.CHEESE, AisleSorter.getSectionFor("Cream cheese"))
    }
}
