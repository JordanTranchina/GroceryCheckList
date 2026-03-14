package com.example.grocery.util

import android.util.Log
import com.example.grocery.BuildConfig
import com.example.grocery.model.GroceryItem
import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONObject

class GeminiAisleSorter {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val sectionPriority: Map<String, Int> = mapOf(
        "BREAD" to 0,
        "PRODUCE" to 1,
        "DAIRY_SNACKS" to 2,
        "MEAT" to 3,
        "FROZEN" to 4,
        "CHEESE" to 5,
        "ALCOHOL" to 6,
        "OTHER" to Int.MAX_VALUE
    )

    suspend fun sortItemsWithSections(items: List<GroceryItem>): Pair<List<GroceryItem>, Map<String, String>> {
        if (items.isEmpty()) return Pair(items, emptyMap())
        
        return try {
            val sections = classifyItems(items.map { it.name })
            
            val sorted = items.sortedWith(
                compareBy<GroceryItem> { sectionPriority[sections[it.name] ?: "OTHER"] ?: Int.MAX_VALUE }
                    .thenBy { it.name }
            )
            Pair(sorted, sections)
        } catch (e: Exception) {
            Log.w("GeminiAisleSorter", "Gemini classification failed: ${e.message}", e)
            Pair(items.sortedBy { it.name }, emptyMap())
        }
    }

    suspend fun sortItems(items: List<GroceryItem>): List<GroceryItem> =
        sortItemsWithSections(items).first

    private suspend fun classifyItems(names: List<String>): Map<String, String> {
        val prompt = buildPrompt(names)
        val response = try {
            model.generateContent(prompt)
        } catch (e: Exception) {
            Log.e("GeminiAisleSorter", "Error during generateContent", e)
            return emptyMap()
        }
        
        val text = response.text ?: return emptyMap()
        Log.d("GeminiAisleSorter", "Raw Gemini Response: $text")
        return parseJsonResponse(text, names)
    }

    private fun buildPrompt(names: List<String>): String {
        val itemList = names.joinToString("\n") { it }
        return """
            You are a grocery store organization expert.
            Classify each item into exactly one of these section labels:
            BREAD, PRODUCE, DAIRY_SNACKS, MEAT, FROZEN, CHEESE, ALCOHOL, OTHER

            Section guide:
            - BREAD: bread, bagels, rolls, buns, tortillas, pita, croissants, loaves, sourdough
            - PRODUCE: fresh fruits, fresh vegetables, herbs, salad greens
            - DAIRY_SNACKS: milk (including almond milk, oat milk), yogurt, butter, cream,
              sour cream, chips, crackers, popcorn, nuts, granola, cookies, candy, mayo, condiments
            - MEAT: all meats, poultry, seafood, deli items, bacon, sausage
            - FROZEN: frozen foods, ice cream, popsicles, frozen meals
            - CHEESE: all cheeses (cheddar, mozzarella, brie, cream cheese, etc.)
            - ALCOHOL: beer, wine, spirits, cider, liquor
            - OTHER: anything else

            Respond ONLY with a valid JSON object. 
            The keys must be the EXACT item names provided below, including any leading numbers or units (e.g. "1 orange").
            Do not change the item names. Do not add markdown fences.

            Items to classify:
            $itemList
        """.trimIndent()
    }

    private fun parseJsonResponse(text: String, names: List<String>): Map<String, String> {
        return try {
            val cleaned = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleaned)
            val jsonMap = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                jsonMap[key.lowercase().trim()] = json.getString(key).uppercase().trim()
            }

            Log.d("GeminiAisleSorter", "Keys found in JSON: ${jsonMap.keys}")

            val result = mutableMapOf<String, String>()
            for (originalName in names) {
                val searchName = originalName.lowercase().trim()
                
                // 1. Try exact match
                var section = jsonMap[searchName]
                
                // 2. Try fuzzy match (if Gemini stripped "1 " from "1 orange")
                if (section == null) {
                    val matchingKey = jsonMap.keys.find { key -> 
                        key.isNotEmpty() && (searchName.contains(key) || key.contains(searchName))
                    }
                    if (matchingKey != null) {
                        section = jsonMap[matchingKey]
                        Log.d("GeminiAisleSorter", "Fuzzy match found: '$originalName' matched to key '$matchingKey' -> $section")
                    }
                }
                
                if (section != null) {
                    Log.d("GeminiAisleSorter", "Final assignment: '$originalName' -> $section")
                    result[originalName] = section
                } else {
                    Log.w("GeminiAisleSorter", "No match found for '$originalName', defaulting to OTHER")
                    result[originalName] = "OTHER"
                }
            }
            result
        } catch (e: Exception) {
            Log.e("GeminiAisleSorter", "JSON parse error: ${e.message}")
            emptyMap()
        }
    }
}
