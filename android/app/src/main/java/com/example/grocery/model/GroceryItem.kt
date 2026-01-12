package com.example.grocery.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class GroceryItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val isCompleted: Boolean = false,
    val order: Int = 0,
    val createdAt: Date = Date()
)
