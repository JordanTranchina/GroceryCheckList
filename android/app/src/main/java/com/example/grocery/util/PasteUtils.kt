package com.example.grocery.util

data class PasteResult(
    val updatedCurrentText: String,
    val newItems: List<String>
)

object PasteUtils {
    /**
     * Processes input text to check for newlines.
     * If newlines are present, splits the text into the updated current item and a list of new items.
     *
     * @param currentText The text currently in the field (unused in this specific logic but good for context if needed later).
     * @param newText The new text after the user input (paste).
     * @return A PasteResult containing the first line as the updated text for the current field
     *         and the rest as new items.
     */
    fun processInput(currentText: String, newText: String): PasteResult {
        if (!newText.contains("\n")) {
            return PasteResult(newText, emptyList())
        }

        val lines = newText.split("\n")
        val updatedCurrent = lines.firstOrNull() ?: ""
        val newItems = lines.drop(1).filter { it.isNotEmpty() }

        return PasteResult(updatedCurrent, newItems)
    }
}
