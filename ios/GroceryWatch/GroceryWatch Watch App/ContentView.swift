import SwiftUI

struct GroceryItemRow: View {
    let item: GroceryItem
    let onToggle: (GroceryItem) -> Void
    
    @State private var isCompleting = false
    
    var body: some View {
        HStack {
            Image(systemName: item.isCompleted ? "checkmark.square.fill" : "square")
                .foregroundStyle(item.isCompleted ? .gray : Color.accentColor)

            Text(item.name)
                .strikethrough(item.isCompleted)
                .foregroundStyle(item.isCompleted ? .gray : .primary)
            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if !item.isCompleted {
                // Trigger visual feedback
                withAnimation {
                    isCompleting = true
                }
                
                // Delay actual toggle to show the green flash
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    onToggle(item)
                    // Reset state for when/if the row is reused or item comes back
                    isCompleting = false 
                }
            } else {
                // Immediate toggle for unchecking (moving back to To Buy)
                 onToggle(item)
            }
        }
        .listRowBackground(
            isCompleting ? Color.green : nil
        )
    }
}

struct ContentView: View {
    @StateObject private var viewModel = GroceryViewModel()
    
    var body: some View {
        Group {
            if #available(watchOS 9.0, *) {
                NavigationStack {
                    contentList
                        .navigationTitle("Groceries")
                }
            } else {
                NavigationView {
                    contentList
                        .navigationTitle("Groceries")
                }
            }
        }
    }
    
    @ViewBuilder
    var contentList: some View {
        List {
            Section(header: Text("To Buy")) {
                ForEach(viewModel.activeItems) { item in
                    GroceryItemRow(item: item, onToggle: viewModel.toggleCompletion)
                }
                .onMove(perform: viewModel.moveItem)
            }
            
            if !viewModel.completedItems.isEmpty {
                Section(header: Text("Completed")) {
                    ForEach(viewModel.completedItems) { item in
                        GroceryItemRow(item: item, onToggle: viewModel.toggleCompletion)
                    }
                }
            }
        }
        .toolbar {
             // ToolbarItem(placement: .primaryAction) {
             //    EditButton()
             // }
        }
    }
}

#Preview {
    ContentView()
}
