package com.example.grocery.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PasteUtilsTest {

    @Test
    fun processInput_noNewlines_returnsSameTextAndEmptyList() {
        val currentText = "Milk"
        val newText = "Milk"
        
        val result = PasteUtils.processInput(currentText, newText)
        
        assertEquals("Milk", result.updatedCurrentText)
        assertEquals(emptyList<String>(), result.newItems)
    }
    
    @Test
    fun processInput_withNewlines_splitsText() {
        // Simulating user pasting "Bread\nButter" into a field that might have had text, 
        // but the 'newText' passed to processInput is the final text in the field after paste.
        // If the implementation assumes 'newText' is what's in the box:
        val newText = "Bread\nButter"
        
        val result = PasteUtils.processInput("", newText)
        
        assertEquals("Bread", result.updatedCurrentText)
        assertEquals(listOf("Butter"), result.newItems)
    }

    @Test
    fun processInput_multipleNewlines_filtersEmptyLines() {
        val newText = "One\nTwo\n\nThree"
        
        val result = PasteUtils.processInput("", newText)
        
        assertEquals("One", result.updatedCurrentText)
        assertEquals(listOf("Two", "Three"), result.newItems)
    }
}
