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

    fun addItem(name: String, order: Int? = null): String {
        val newId = collection.document().id
        val newOrder = order ?: (System.currentTimeMillis() / 1000).toInt()
        val newItem = GroceryItem(
            id = newId,
            name = name,
            isCompleted = false,
            order = newOrder,
            createdAt = Date()
        )
        collection.document(newId).set(newItem)
        return newId
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

    fun deleteItem(item: GroceryItem) {
        if (item.id.isNotEmpty()) {
            collection.document(item.id).delete()
                .addOnSuccessListener { Log.d("GroceryRepository", "Delete successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Delete failed", e) }
        }
    }

    fun updateName(item: GroceryItem, newName: String) {
        if (item.id.isNotEmpty()) {
            collection.document(item.id).update("name", newName)
                .addOnSuccessListener { Log.d("GroceryRepository", "Name update successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Name update failed", e) }
        }
    }

    fun updateOrders(items: List<GroceryItem>) {
        val batch = db.batch()
        items.forEachIndexed { index, item ->
            if (item.id.isNotEmpty()) {
                val ref = collection.document(item.id)
                batch.update(ref, "order", index)
            }
        }
        batch.commit()
            .addOnSuccessListener { Log.d("GroceryRepository", "Batch order update successful") }
            .addOnFailureListener { e -> Log.e("GroceryRepository", "Batch order update failed", e) }
    }
}
