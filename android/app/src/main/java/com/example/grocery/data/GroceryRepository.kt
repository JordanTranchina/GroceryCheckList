package com.example.grocery.data

import android.util.Log
import com.example.grocery.model.GroceryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

class GroceryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("groceries")

    val items: Flow<List<GroceryItem>> = callbackFlow {
        val listener = collection
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("GroceryRepository", "Listen failed.", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(GroceryItem::class.java)
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    fun addItem(name: String) {
        // Simple default ordering: use current timestamp (seconds) to keep it near end
        val newOrder = (System.currentTimeMillis() / 1000).toInt()
        val newItem = GroceryItem(
            name = name,
            isCompleted = false,
            order = newOrder,
            createdAt = Date()
        )
        collection.add(newItem)
    }

    fun toggleCompletion(item: GroceryItem) {
        Log.d("GroceryRepository", "Toggling item: ${item.id} current status: ${item.isCompleted}")
        if (item.id.isNotEmpty()) {
            collection.document(item.id).update("isCompleted", !item.isCompleted)
                .addOnSuccessListener { Log.d("GroceryRepository", "Update successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Update failed", e) }
        } else {
            Log.e("GroceryRepository", "Cannot toggle item with empty ID")
        }
    }
}
