import Foundation
import Combine
import SwiftUI

// Mock Repository Pattern for now until simulated Firebase
class GroceryViewModel: ObservableObject {
    @Published var items: [GroceryItem] = []
    
    init() {
        // Temporary: Seed with some data
        self.items = [
            GroceryItem(name: "Milk", order: 0),
            GroceryItem(name: "Eggs", order: 1),
            GroceryItem(name: "Bread", order: 2),
            GroceryItem(name: "Bananas", isCompleted: true, order: 3)
        ]
    }
    
    var activeItems: [GroceryItem] {
        items.filter { !$0.isCompleted }.sorted { $0.order < $1.order }
    }
    
    var completedItems: [GroceryItem] {
        items.filter { $0.isCompleted }.sorted { $0.order < $1.order }
    }
    
    func toggleCompletion(item: GroceryItem) {
        if let index = items.firstIndex(where: { $0.id == item.id }) {
            withAnimation {
                items[index].isCompleted.toggle()
            }
        }
    }
    
    func moveItem(from source: IndexSet, to destination: Int) {
        // Reordering logic will be tricky with two sections (active/completed)
        // For now, implementing simple reorder within the active list
        // Real implementation will need to update the 'order' field in Firebase
        var active = activeItems
        active.move(fromOffsets: source, toOffset: destination)
        
        // Update the main list based on new order
        for (index, item) in active.enumerated() {
            if let mainIndex = items.firstIndex(where: { $0.id == item.id }) {
                items[mainIndex].order = index
            }
        }
    }
}
