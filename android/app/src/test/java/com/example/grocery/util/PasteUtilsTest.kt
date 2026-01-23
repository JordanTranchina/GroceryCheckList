package com.example.grocery.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PasteUtilsTest {

    @Test
    fun `processInput returns original text when no newlines`() {
        val current = "Milk"
        val newText = "Milk"
        
        val result = PasteUtils.processInput(current, newText)
        
        assertEquals("Milk", result.updatedCurrentText)
        assertEquals(emptyList<String>(), result.newItems)
    }

    @Test
    fun `processInput splits simple newline paste`() {
        val current = ""
        val newText = "Milk\nEggs"
        
        val result = PasteUtils.processInput(current, newText)
        
        assertEquals("Milk", result.updatedCurrentText)
        assertEquals(listOf("Eggs"), result.newItems)
    }

    @Test
    fun `processInput splits multiple newlines`() {
        val current = ""
        val newText = "Milk\nEggs\nBread"
        
        val result = PasteUtils.processInput(current, newText)
        
        assertEquals("Milk", result.updatedCurrentText)
        assertEquals(listOf("Eggs", "Bread"), result.newItems)
    }

    @Test
    fun `processInput appends to existing text correctly`() {
        // User had "Buy " and pasted "Milk\nEggs"
        val current = "Buy "
        val newText = "Buy Milk\nEggs"
        
        val result = PasteUtils.processInput(current, newText)
        
        assertEquals("Buy Milk", result.updatedCurrentText)
        assertEquals(listOf("Eggs"), result.newItems)
    }

    @Test
    fun `processInput ignores empty lines`() {
        val current = ""
        val newText = "Milk\n\nEggs" // double newline
        
        val result = PasteUtils.processInput(current, newText)
        
        assertEquals("Milk", result.updatedCurrentText)
        assertEquals(listOf("Eggs"), result.newItems)
    }
}
