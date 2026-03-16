import Foundation
import Combine
import SwiftUI

enum FetchStatus {
    case idle, loading, loaded(count: Int), error(String)
}

class GroceryViewModel: ObservableObject {
    @Published var items: [GroceryItem] = []
    @Published var fetchStatus: FetchStatus = .idle
    
    private var projectId: String {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path),
              let projectId = dict["PROJECT_ID"] as? String else {
            return ""
        }
        return projectId
    }
    
    private let collectionName = "groceries"
    
    private var apiKey: String {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path),
              let apiKey = dict["API_KEY"] as? String else {
            return ""
        }
        return apiKey
    }
    
    private var isFetching = false
    
    init() {
        fetchItems()
    }
    
    func fetchItems() {
        guard !isFetching else { return }
        
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)?key=\(apiKey)"
        guard let url = URL(string: urlString) else { return }
        
        isFetching = true
        DispatchQueue.main.async { self.fetchStatus = .loading }
        
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            defer { self?.isFetching = false }
            
            if let error = error {
                let msg = error.localizedDescription
                print("Network error: \(msg)")
                DispatchQueue.main.async {
                    self?.fetchStatus = .error("Network: \(msg)")
                }
                return
            }
            
            let httpStatus = (response as? HTTPURLResponse)?.statusCode ?? -1
            guard let data = data, httpStatus == 200 else {
                let body = data.flatMap { String(data: $0, encoding: .utf8) } ?? "(no body)"
                let msg = "HTTP \(httpStatus): \(body.prefix(120))"
                print("Bad response: \(msg)")
                // Don't show confusing 429 parsing errors if it still happens, just show rate limit message
                let errorMsg = httpStatus == 429 ? "Rate limit reached. Trying again soon." : msg
                DispatchQueue.main.async {
                    self?.fetchStatus = .error(errorMsg)
                }
                return
            }
            
            do {
                let result = try JSONDecoder().decode(FirestoreResponse.self, from: data)
                let decoded = (result.documents ?? []).compactMap { $0.toGroceryItem() }
                DispatchQueue.main.async {
                    self?.items = decoded
                    self?.fetchStatus = .loaded(count: decoded.count)
                }
            } catch {
                let msg = "Decode: \(error)"
                print(msg)
                DispatchQueue.main.async {
                    self?.fetchStatus = .error(msg)
                }
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
        
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)/\(id)?updateMask.fieldPaths=isCompleted&key=\(apiKey)"
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
        }.resume()
    }
    
    func moveItem(from source: IndexSet, to destination: Int) {
        print("Reordering via REST not implemented yet")
    }
    
    func moveToBottom(item: GroceryItem) {
        // 1. Calculate new max order
        let currentMaxOrder = items.map { $0.order }.max() ?? 0
        let newOrder = currentMaxOrder + 1
        
        guard let id = item.id, let index = items.firstIndex(where: { $0.id == id }) else { return }
        
        // 2. Optimistic update
        items[index].order = newOrder
        
        // 3. Persist to Firestore
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)/\(id)?updateMask.fieldPaths=order&key=\(apiKey)"
        guard let url = URL(string: urlString) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "fields": [
                "order": ["integerValue": "\(newOrder)"]
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
                print("Error moving item to bottom: \(error)")
            }
        }.resume()
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
