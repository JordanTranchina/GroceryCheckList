import Foundation
import Combine
import SwiftUI

enum FetchStatus {
    case idle, loading, loaded(count: Int), error(String)
}

class GroceryViewModel: ObservableObject {
    @Published var items: [GroceryItem] = []
    @Published var fetchStatus: FetchStatus = .idle
    
    // Project ID from your GoogleService-Info.plist
    private let projectId = "stable-dogfish-459214-c5"
    private let collectionName = "groceries"
    private let apiKey = "AIzaSyDoGCMFzr4IwOP0pfF_VSnccm1nt-Vmees"
    
    private var isFetching = false

    // Offline support: the last fetched list and unsent changes stay on the watch.
    @Published var lastSynced: Date?
    @Published private(set) var pendingChanges: [PendingChange] = []
    private let storeURL: URL?

    init(storeURL: URL? = GroceryViewModel.defaultStoreURL, autoFetch: Bool = true) {
        self.storeURL = storeURL
        loadCache()
        if autoFetch {
            fetchItems()
        }
    }

    static var defaultStoreURL: URL? {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        return dir?.appendingPathComponent("grocery-cache.json")
    }

    // MARK: - Local cache

    func loadCache() {
        guard let url = storeURL,
              let data = try? Data(contentsOf: url),
              let snapshot = try? JSONDecoder().decode(CacheSnapshot.self, from: data) else { return }
        items = snapshot.items
        lastSynced = snapshot.lastSynced
        pendingChanges = snapshot.pending
    }

    func saveCache() {
        guard let url = storeURL else { return }
        let snapshot = CacheSnapshot(items: items, lastSynced: lastSynced, pending: pendingChanges)
        do {
            try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(snapshot).write(to: url, options: .atomic)
        } catch {
            print("Cache save error: \(error)")
        }
    }

    // MARK: - Pending change queue

    func enqueue(_ change: PendingChange) {
        // Keep only the newest change for each item and field.
        pendingChanges.removeAll { $0.itemId == change.itemId && $0.field == change.field }
        pendingChanges.append(change)
        saveCache()
        flushPending()
    }

    /// Applies unsent changes on top of a fresh server list, so offline edits are not lost.
    func applyPending(to fetched: [GroceryItem]) -> [GroceryItem] {
        var result = fetched
        for change in pendingChanges {
            guard let i = result.firstIndex(where: { $0.id == change.itemId }) else { continue }
            switch change.field {
            case .isCompleted: result[i].isCompleted = change.boolValue ?? result[i].isCompleted
            case .order: result[i].order = change.intValue ?? result[i].order
            }
        }
        return result
    }

    func flushPending() {
        for change in pendingChanges {
            send(change) { [weak self] ok in
                guard ok, let self = self else { return }
                DispatchQueue.main.async {
                    self.pendingChanges.removeAll { $0 == change }
                    self.saveCache()
                }
            }
        }
    }

    private func send(_ change: PendingChange, completion: @escaping (Bool) -> Void) {
        let field = change.field.rawValue
        let urlString = "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/\(collectionName)/\(change.itemId)?updateMask.fieldPaths=\(field)&key=\(apiKey)"
        guard let url = URL(string: urlString) else { return completion(false) }

        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let value: [String: Any]
        switch change.field {
        case .isCompleted: value = ["booleanValue": change.boolValue ?? false]
        case .order: value = ["integerValue": "\(change.intValue ?? 0)"]
        }
        guard let body = try? JSONSerialization.data(withJSONObject: ["fields": [field: value]]) else {
            return completion(false)
        }
        request.httpBody = body

        URLSession.shared.dataTask(with: request) { _, response, error in
            let status = (response as? HTTPURLResponse)?.statusCode ?? -1
            if let error = error { print("Sync error: \(error)") }
            completion(error == nil && (200..<300).contains(status))
        }.resume()
    }

    func fetchItems() {
        guard !isFetching else { return }
        flushPending()
        
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
                    guard let self = self else { return }
                    self.items = self.applyPending(to: decoded)
                    self.lastSynced = Date()
                    self.fetchStatus = .loaded(count: decoded.count)
                    self.saveCache()
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
        
        enqueue(PendingChange(itemId: id, field: .isCompleted, boolValue: !item.isCompleted))
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
        
        // 3. Save on the watch and send to Firestore (queued while offline)
        enqueue(PendingChange(itemId: id, field: .order, intValue: newOrder))
    }
}

struct PendingChange: Codable, Equatable {
    enum Field: String, Codable { case isCompleted, order }
    let itemId: String
    let field: Field
    var boolValue: Bool? = nil
    var intValue: Int? = nil
}

struct CacheSnapshot: Codable {
    let items: [GroceryItem]
    let lastSynced: Date?
    let pending: [PendingChange]
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
