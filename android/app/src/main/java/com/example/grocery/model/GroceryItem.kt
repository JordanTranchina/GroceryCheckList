package com.example.grocery.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class GroceryItem(
    @get:DocumentId
    var id: String = "",
    
    var name: String = "",
    
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    
    var order: Int = 0,
    
    var createdAt: Date = Date()
)
