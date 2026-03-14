package com.example.grocery.util

import com.example.grocery.model.GroceryItem

object AisleSorter {

    enum class Section(val priority: Int) {
        BREAD(0),
        PRODUCE(1),
        DAIRY_SNACKS(2),
        MEAT(3),
        FROZEN(4),
        CHEESE(5),
        ALCOHOL(6),
        OTHER(Int.MAX_VALUE)
    }

    private val breadKeywords = listOf(
        "bread", "bagel", "bun", "roll", "tortilla", "pita", "croissant",
        "muffin", "baguette", "loaf"
    )

    private val produceKeywords = listOf(
        "apple", "banana", "orange", "grape", "strawberry", "blueberry",
        "lemon", "lime", "avocado", "tomato", "lettuce", "spinach", "kale",
        "carrot", "celery", "broccoli", "cucumber", "pepper", "onion", "garlic",
        "potato", "mushroom", "zucchini", "berry", "fruit", "vegetable", "salad",
        "herb", "ginger", "raspberry", "blackberry", "cherry", "peach", "pear",
        "plum", "mango", "melon", "watermelon", "pineapple", "kiwi", "papaya",
        "cauliflower", "cabbage", "corn", "beet", "squash", "yam", "arugula",
        "cilantro", "parsley", "basil", "mint"
    )

    private val cheeseKeywords = listOf(
        "cheese", "cheddar", "mozzarella", "parmesan", "gouda", "brie",
        "feta", "swiss", "provolone", "gruyere", "ricotta"
    )

    private val dairySnacksKeywords = listOf(
        "milk", "almond milk", "oat milk", "yogurt", "butter", "sour cream",
        "chips", "crackers", "pretzels", "popcorn", "nuts",
        "granola", "trail mix", "cookies", "candy", "chocolate", "snack"
    )

    private val meatKeywords = listOf(
        "chicken", "beef", "pork", "turkey", "fish", "salmon", "shrimp",
        "steak", "bacon", "sausage", "ham", "lamb", "crab", "lobster",
        "tuna", "tilapia", "cod", "ground", "meat", "deli", "hot dog"
    )

    private val frozenKeywords = listOf(
        "frozen", "ice cream", "popsicle", "waffle", "freezer"
    )

    private val alcoholKeywords = listOf(
        "beer", "wine", "vodka", "whiskey", "rum", "tequila", "champagne",
        "cider", "gin", "liquor", "bourbon", "scotch", "alcohol", "spirits",
        "lager", "ale", "stout"
    )

    fun getSectionFor(itemName: String): Section {
        val lower = itemName.lowercase()
        return when {
            breadKeywords.any { lower.contains(it) } -> Section.BREAD
            // Check dairy/snacks before produce so "potato chips" hits "chips" before "potato"
            dairySnacksKeywords.any { lower.contains(it) } -> Section.DAIRY_SNACKS
            // Check cheese before produce so "cheese" doesn't fall through
            cheeseKeywords.any { lower.contains(it) } -> Section.CHEESE
            produceKeywords.any { lower.contains(it) } -> Section.PRODUCE
            meatKeywords.any { lower.contains(it) } -> Section.MEAT
            frozenKeywords.any { lower.contains(it) } -> Section.FROZEN
            alcoholKeywords.any { lower.contains(it) } -> Section.ALCOHOL
            else -> Section.OTHER
        }
    }

    /**
     * Sorts items by grocery section priority. Items within the same section
     * preserve their original relative order. Uncategorized items are appended last.
     */
    fun sortItems(items: List<GroceryItem>): List<GroceryItem> {
        return items.sortedWith(
            compareBy({ getSectionFor(it.name).priority }, { items.indexOf(it) })
        )
    }
}
