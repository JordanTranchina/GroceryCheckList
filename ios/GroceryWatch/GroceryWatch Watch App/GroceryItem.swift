import Foundation

struct GroceryItem: Identifiable, Codable {
    var id: String?
    var name: String
    var isCompleted: Bool
    var order: Int
    var createdAt: Date
    
    init(id: String? = nil, name: String, isCompleted: Bool = false, order: Int = 0, createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.isCompleted = isCompleted
        self.order = order
        self.createdAt = createdAt
    }
}

