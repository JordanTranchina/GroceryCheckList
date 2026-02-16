import SwiftUI

struct GroceryItemRow: View {
    let item: GroceryItem
    let onToggle: (GroceryItem) -> Void
    let onMoveToBottom: (GroceryItem) -> Void
    
    @State private var offset: CGFloat = 0
    @State private var isCompleting = false
    
    var body: some View {
        ZStack {
            // Background Layer for Swipe Actions
            if offset != 0 {
                GeometryReader { geometry in
                    HStack {
                        if offset > 0 {
                            // Swiping Right -> Complete (Green)
                            ZStack(alignment: .leading) {
                                Color.green
                                Image(systemName: "checkmark")
                                    .font(.title3)
                                    .padding(.leading, 12)
                                    .foregroundColor(.white)
                            }
                        } else {
                            // Swiping Left -> Bottom (Blue)
                            ZStack(alignment: .trailing) {
                                Color.blue
                                Image(systemName: "arrow.bottom.to.line")
                                    .font(.title3)
                                    .padding(.trailing, 12)
                                    .foregroundColor(.white)
                            }
                        }
                    }
                }
            }
            
            // Content Layer
            HStack {
                Image(systemName: item.isCompleted ? "checkmark.square.fill" : "square")
                    .foregroundStyle(item.isCompleted ? .gray : Color.accentColor)

                Text(item.name)
                    .strikethrough(item.isCompleted)
                    .foregroundStyle(item.isCompleted ? .gray : .primary)
                Spacer()
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
            // Independent Tap Gesture
            .onTapGesture {
                if !item.isCompleted {
                    // Trigger visual feedback
                    withAnimation {
                        isCompleting = true
                    }
                    
                    // Delay actual toggle to show the green flash
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        onToggle(item)
                        // Reset state for when/if the row is reused or item comes back
                        isCompleting = false
                    }
                } else {
                    // Immediate toggle for unchecking (moving back to To Buy)
                     onToggle(item)
                }
            }
            // Drag Gesture for Swipe Actions
            .offset(x: offset)
            .gesture(
                DragGesture()
                    .onChanged { gesture in
                        // Add some resistance or limit
                        withAnimation(.interactiveSpring()) {
                            offset = gesture.translation.width
                        }
                    }
                    .onEnded { gesture in
                        withAnimation(.spring()) {
                            if offset > 70 {
                                // Threshold met: Complete
                                onToggle(item)
                                offset = 0
                            } else if offset < -70 {
                                // Threshold met: Move to Bottom
                                onMoveToBottom(item)
                                offset = 0
                            } else {
                                // Reset
                                offset = 0
                            }
                        }
                    }
            )
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
                    GroceryItemRow(
                        item: item,
                        onToggle: viewModel.toggleCompletion,
                        onMoveToBottom: viewModel.moveToBottom
                    )
                }
                .onMove(perform: viewModel.moveItem)
            }
            
            if !viewModel.completedItems.isEmpty {
                Section(header: Text("Completed")) {
                    ForEach(viewModel.completedItems) { item in
                        GroceryItemRow(
                            item: item,
                            onToggle: viewModel.toggleCompletion,
                            onMoveToBottom: viewModel.moveToBottom
                        )
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
