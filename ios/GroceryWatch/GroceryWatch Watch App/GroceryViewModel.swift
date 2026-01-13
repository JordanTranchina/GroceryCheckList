import Foundation
import Combine
import SwiftUI

// Mock Repository Pattern for now until simulated Firebase
import Foundation
import Combine
import SwiftUI

class GroceryViewModel: ObservableObject {
    @Published var items: [GroceryItem] = []
    
    // Project ID from your GoogleService-Info.plist
    private let projectId = "stable-dogfish-459214-c5"
    private let collectionName = "groceries"
    private var timer: Timer?
    
    init() {
        fetchItems()
        // Poll every 10 seconds for updates
        timer = Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { [weak self] _ in
            self?.fetchItems()
        }
    }
    
    deinit {
        timer?.invalidate()
    }
    
    func fetchItems() {
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)"
        guard let url = URL(string: urlString) else { return }
        
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            guard let data = data, error == nil else {
                print("Error fetching items: \(error?.localizedDescription ?? "Unknown error")")
                return
            }
            
            do {
                let result = try JSONDecoder().decode(FirestoreResponse.self, from: data)
                DispatchQueue.main.async {
                    self?.items = (result.documents ?? []).compactMap { doc in
                        return doc.toGroceryItem()
                    }
                }
            } catch {
                print("Decoding error: \(error)")
            }
        }.resume()
    }
    
    var activeItems: [GroceryItem] {
        items.filter { !$0.isCompleted }.sorted { $0.order < $1.order }
    }
    
    var completedItems: [GroceryItem] {
        items.filter { $0.isCompleted }.sorted { $0.order < $1.order }
    }
    
    func toggleCompletion(item: GroceryItem) {
        guard let id = item.id else { return }
        
        // Optimistic update
        if let index = items.firstIndex(where: { $0.id == id }) {
            items[index].isCompleted.toggle()
        }
        
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)/\(id)?updateMask.fieldPaths=isCompleted"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "fields": [
                "isCompleted": ["booleanValue": !item.isCompleted]
            ]
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        } catch {
            print("Error encoding body: \(error)")
            return
        }
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("Error updating item: \(error)")
            }
            // Ideally revert optimistic update on failure, keeping simple for now
        }.resume()
    }
    
    func moveItem(from source: IndexSet, to destination: Int) {
        print("Reordering via REST not implemented yet")
    }
}

// REST API Helper Models
struct FirestoreResponse: Decodable {
    let documents: [FirestoreDocument]?
}

struct FirestoreDocument: Decodable {
    let name: String // Full path: projects/.../databases/.../documents/groceries/ID
    let fields: FirestoreFields
    
    func toGroceryItem() -> GroceryItem? {
        // Extract ID from the full path name
        let id = name.components(separatedBy: "/").last
        
        // Parse fields
        // Note: Firestore REST returns types like { "stringValue": "Milk" }
        let nameValue = fields.name.stringValue
        let isCompletedValue = fields.isCompleted.booleanValue
        let orderValue = Int(fields.order.integerValue ?? "0") ?? 0
        
        // Use current date for simplicity if createdAt is missing or complex to parse
        return GroceryItem(id: id, name: nameValue, isCompleted: isCompletedValue, order: orderValue, createdAt: Date())
    }
}

struct FirestoreFields: Decodable {
    let name: StringValue
    let isCompleted: BooleanValue
    let order: IntegerValue
    
    struct StringValue: Decodable { let stringValue: String }
    struct BooleanValue: Decodable { let booleanValue: Bool }
    struct IntegerValue: Decodable { let integerValue: String? } // Firestore integers are strings in JSON
}
