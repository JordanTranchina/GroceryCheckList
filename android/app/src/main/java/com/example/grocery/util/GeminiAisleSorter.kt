package com.example.grocery.util

import android.util.Log
import com.example.grocery.BuildConfig
import com.example.grocery.model.GroceryItem
import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONObject

class GeminiAisleSorter {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val sectionPriority = mapOf(
        "BREAD" to 0,
        "PRODUCE" to 1,
        "DAIRY_SNACKS" to 2,
        "MEAT" to 3,
        "FROZEN" to 4,
        "CHEESE" to 5,
        "ALCOHOL" to 6,
        "OTHER" to Int.MAX_VALUE
    )

    /**
     * Classifies all items in a single Gemini API call and returns them sorted by
     * section priority. Falls back to keyword-based [AisleSorter] on any error.
     */
    suspend fun sortItems(items: List<GroceryItem>): List<GroceryItem> {
        if (items.isEmpty()) return items
        return try {
            val sections = classifyItems(items.map { it.name })
            items.sortedWith(
                compareBy(
                    { sectionPriority[sections[it.name] ?: "OTHER"] ?: Int.MAX_VALUE },
                    { items.indexOf(it) }
                )
            )
        } catch (e: Exception) {
            Log.w("GeminiAisleSorter", "Gemini classification failed, falling back to keyword matching", e)
            AisleSorter.sortItems(items)
        }
    }

    private suspend fun classifyItems(names: List<String>): Map<String, String> {
        val prompt = buildPrompt(names)
        val response = model.generateContent(prompt)
        val text = response.text ?: return emptyMap()
        return parseJsonResponse(text, names)
    }

    private fun buildPrompt(names: List<String>): String {
        val itemList = names.joinToString("\n") { "- $it" }
        return """
            You are classifying grocery store items into store sections for efficient shopping.
            Classify each item into exactly one of these section labels:
            BREAD, PRODUCE, DAIRY_SNACKS, MEAT, FROZEN, CHEESE, ALCOHOL, OTHER

            Section guide:
            - BREAD: bread, bagels, rolls, buns, tortillas, pita, croissants, loaves
            - PRODUCE: fresh fruits, fresh vegetables, herbs, salad greens
            - DAIRY_SNACKS: milk (including almond milk, oat milk), yogurt, butter, cream,
              sour cream, chips, crackers, popcorn, nuts, granola, cookies, candy
            - MEAT: all meats, poultry, seafood, deli items, bacon, sausage
            - FROZEN: frozen foods, ice cream, popsicles, frozen meals
            - CHEESE: all cheeses (cheddar, mozzarella, brie, cream cheese, etc.)
            - ALCOHOL: beer, wine, spirits, cider, liquor
            - OTHER: cleaning supplies, paper goods, personal care, anything else

            Respond ONLY with a valid JSON object mapping each item name exactly as given to
            its section label. No markdown fences, no explanation, just the raw JSON object.

            Items to classify:
            $itemList
        """.trimIndent()
    }

    private fun parseJsonResponse(text: String, names: List<String>): Map<String, String> {
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)
        return names.associateWith { name ->
            json.optString(name, "OTHER").uppercase()
        }
    }
}
