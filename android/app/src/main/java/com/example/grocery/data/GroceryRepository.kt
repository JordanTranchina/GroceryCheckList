package com.example.grocery.data

import android.util.Log
import com.example.grocery.model.GroceryItem
import com.example.grocery.util.GeminiAisleSorter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

class GroceryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("groceries")
    private val geminiAisleSorter = GeminiAisleSorter()

    sealed class GroceryAction {
        data class DeleteItem(val item: GroceryItem) : GroceryAction()
        data class ToggleCompletion(val item: GroceryItem, val previousState: Boolean) : GroceryAction()
        data class DeleteItems(val items: List<GroceryItem>) : GroceryAction()
        data class SortBySection(val previousOrders: Map<String, Int>) : GroceryAction()
    }

    var lastAction: GroceryAction? = null
        private set

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

    fun addItem(name: String, id: String? = null): String {
        // Simple default ordering: use current timestamp (seconds) to keep it near end
        val newOrder = (System.currentTimeMillis() / 1000).toInt()
        
        val docRef = if (id != null) collection.document(id) else collection.document()
        val finalId = docRef.id
        
        val newItem = GroceryItem(
            id = finalId,
            name = name,
            isCompleted = false,
            order = newOrder,
            createdAt = Date()
        )
        docRef.set(newItem)
        return finalId
    }

    fun addItems(names: List<String>) {
        if (names.isEmpty()) return
        
        val batch = db.batch()
        val baseOrder = (System.currentTimeMillis() / 1000).toInt()
        
        names.forEachIndexed { index, name ->
            val docRef = collection.document()
            val newItem = GroceryItem(
                id = docRef.id,
                name = name,
                isCompleted = false,
                order = baseOrder + index,
                createdAt = Date()
            )
            batch.set(docRef, newItem)
        }
        
        batch.commit()
            .addOnSuccessListener { Log.d("GroceryRepository", "Batch add successful") }
            .addOnFailureListener { e -> Log.e("GroceryRepository", "Batch add failed", e) }
    }

    fun toggleCompletion(item: GroceryItem) {
        Log.d("GroceryRepository", "Toggling item: ${item.id} current status: ${item.isCompleted}")
        if (item.id.isNotEmpty()) {
            lastAction = GroceryAction.ToggleCompletion(item, item.isCompleted)
            collection.document(item.id).update("isCompleted", !item.isCompleted)
                .addOnSuccessListener { Log.d("GroceryRepository", "Update successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Update failed", e) }
        } else {
            Log.e("GroceryRepository", "Cannot toggle item with empty ID")
        }
    }

    fun deleteItem(item: GroceryItem) {
        if (item.id.isNotEmpty()) {
            lastAction = GroceryAction.DeleteItem(item)
            collection.document(item.id).delete()
                .addOnSuccessListener { Log.d("GroceryRepository", "Delete successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Delete failed", e) }
        }
    }

    fun deleteItems(itemsToDelete: List<GroceryItem>) {
        if (itemsToDelete.isEmpty()) return
        
        lastAction = GroceryAction.DeleteItems(itemsToDelete)
        
        val batch = db.batch()
        itemsToDelete.forEach { item ->
            if (item.id.isNotEmpty()) {
                batch.delete(collection.document(item.id))
            }
        }
        
        batch.commit()
            .addOnSuccessListener { Log.d("GroceryRepository", "Batch delete successful") }
            .addOnFailureListener { e -> Log.e("GroceryRepository", "Batch delete failed", e) }
    }

    fun updateName(item: GroceryItem, newName: String) {
        if (item.id.isNotEmpty()) {
            collection.document(item.id).update("name", newName)
                .addOnSuccessListener { Log.d("GroceryRepository", "Name update successful") }
                .addOnFailureListener { e -> Log.e("GroceryRepository", "Name update failed", e) }
        }
    }

    suspend fun sortBySection(items: List<GroceryItem>) {
        if (items.isEmpty()) return
        lastAction = GroceryAction.SortBySection(items.associate { it.id to it.order })
        val (sortedItems, sections) = geminiAisleSorter.sortItemsWithSections(items)
        updateOrdersAndSections(sortedItems, sections)
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

    fun updateOrdersAndSections(items: List<GroceryItem>, sections: Map<String, String>) {
        val batch = db.batch()
        items.forEachIndexed { index, item ->
            if (item.id.isNotEmpty()) {
                val ref = collection.document(item.id)
                val section = sections[item.name] ?: "OTHER"
                batch.update(ref, mapOf("order" to index, "section" to section))
            }
        }
        batch.commit()
            .addOnSuccessListener { Log.d("GroceryRepository", "Batch order+section update successful") }
            .addOnFailureListener { e -> Log.e("GroceryRepository", "Batch order+section update failed", e) }
    }

    fun undoLastAction() {
        when (val action = lastAction) {
            is GroceryAction.DeleteItem -> {
                collection.document(action.item.id).set(action.item)
            }
            is GroceryAction.DeleteItems -> {
                val batch = db.batch()
                action.items.forEach { item ->
                    batch.set(collection.document(item.id), item)
                }
                batch.commit()
            }
            is GroceryAction.ToggleCompletion -> {
                collection.document(action.item.id).update("isCompleted", action.previousState)
            }
            is GroceryAction.SortBySection -> {
                val batch = db.batch()
                action.previousOrders.forEach { (id, order) ->
                    batch.update(collection.document(id), "order", order)
                }
                batch.commit()
                    .addOnSuccessListener { Log.d("GroceryRepository", "Undo sort successful") }
                    .addOnFailureListener { e -> Log.e("GroceryRepository", "Undo sort failed", e) }
            }
            null -> {
                Log.d("GroceryRepository", "No action to undo")
            }
        }
        lastAction = null
    }
}
