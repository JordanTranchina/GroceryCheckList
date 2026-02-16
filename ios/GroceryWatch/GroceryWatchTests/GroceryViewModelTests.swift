import XCTest
@testable import GroceryWatch_Watch_App

final class GroceryViewModelTests: XCTestCase {
    
    var viewModel: GroceryViewModel!
    
    override func setUpWithError() throws {
        viewModel = GroceryViewModel()
        // Initialize with some mock data if needed
        viewModel.items = [
            GroceryItem(id: "1", name: "Apple", isCompleted: false, order: 1),
            GroceryItem(id: "2", name: "Banana", isCompleted: true, order: 2),
            GroceryItem(id: "3", name: "Carrot", isCompleted: false, order: 3)
        ]
    }
    
    override func tearDownWithError() throws {
        viewModel = nil
    }
    
    func testToggleCompletion() {
        // Given
        let item = viewModel.items[0] // Apple (not completed)
        XCTAssertFalse(item.isCompleted)
        
        // When
        viewModel.toggleCompletion(item: item)
        
        // Then (Optimistic Update)
        XCTAssertTrue(viewModel.items[0].isCompleted)
        
        // Toggle back
        viewModel.toggleCompletion(item: viewModel.items[0])
        XCTAssertFalse(viewModel.items[0].isCompleted)
    }
    
    func testMoveToBottom() {
        // Given
        let itemToMove = viewModel.items[0] // Apple (Order 1)
        let originalMaxOrder = viewModel.items.map { $0.order }.max() ?? 0
        XCTAssertEqual(originalMaxOrder, 3) 
        
        // When
        viewModel.moveToBottom(item: itemToMove)
        
        // Then (Optimistic Update)
        // The item should now have order = originalMaxOrder + 1 = 4
        if let movedItem = viewModel.items.first(where: { $0.id == "1" }) {
            XCTAssertEqual(movedItem.order, 4)
        } else {
            XCTFail("Item not found after move")
        }
    }
    
    func testActiveItemsFilter() {
        // Given: Apple (false), Banana (true), Carrot (false)
        
        // When
        let active = viewModel.activeItems
        
        // Then
        XCTAssertEqual(active.count, 2)
        XCTAssertEqual(active.first?.name, "Apple")
        XCTAssertEqual(active.last?.name, "Carrot")
    }
    
    func testCompletedItemsFilter() {
        // Given: Apple (false), Banana (true), Carrot (false)
        
        // When
        let completed = viewModel.completedItems
        
        // Then
        XCTAssertEqual(completed.count, 1)
        XCTAssertEqual(completed.first?.name, "Banana")
    }
}
