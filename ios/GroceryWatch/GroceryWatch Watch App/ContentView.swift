import SwiftUI
import WatchKit

struct GroceryItemRow: View {
    let item: GroceryItem
    let onToggle: (GroceryItem) -> Void
    let onMoveToBottom: (GroceryItem) -> Void
    
    @State private var offset: CGFloat = 0
    @State private var isCompleting = false

    static let revealPoint: CGFloat = 30
    static let commitPoint: CGFloat = 70
    static let fadeDistance: CGFloat = 15
    static let iconWidth: CGFloat = 24
    
    var body: some View {
        ZStack {
            // Background Layer for Swipe Actions.
            // The color stays hidden until the row passes revealPoint, then fades in.
            // The icon's outer edge sits at commitPoint, so a fully uncovered icon means release will commit.
            if abs(offset) > Self.revealPoint {
                ZStack(alignment: offset > 0 ? .leading : .trailing) {
                    (offset > 0 ? Color.green : Color.blue)
                    Image(systemName: offset > 0 ? "checkmark" : "arrow.bottom.to.line")
                        .font(.title3)
                        .foregroundColor(.white)
                        .frame(width: Self.iconWidth)
                        .scaleEffect(abs(offset) >= Self.commitPoint ? 1.0 : 0.7)
                        .padding(offset > 0 ? .leading : .trailing, Self.commitPoint - Self.iconWidth)
                }
                .opacity(Double(min(1, (abs(offset) - Self.revealPoint) / Self.fadeDistance)))
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
                        let newOffset = gesture.translation.width
                        if (abs(offset) < Self.commitPoint) != (abs(newOffset) < Self.commitPoint) {
                            WKInterfaceDevice.current().play(.click)
                        }
                        withAnimation(.interactiveSpring()) {
                            offset = newOffset
                        }
                    }
                    .onEnded { gesture in
                        withAnimation(.spring()) {
                            if offset >= Self.commitPoint {
                                // Threshold met: Complete
                                onToggle(item)
                                offset = 0
                            } else if offset <= -Self.commitPoint {
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
    @Environment(\.scenePhase) private var scenePhase
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
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                viewModel.fetchItems()
            }
        }
    }
    
    var statusIcon: String {
        switch viewModel.fetchStatus {
        case .idle:            return "clock"
        case .loading:         return "arrow.trianglehead.clockwise"
        case .loaded:          return "checkmark.circle.fill"
        case .error:           return "exclamationmark.triangle.fill"
        }
    }
    var statusColor: Color {
        switch viewModel.fetchStatus {
        case .idle, .loading:  return .gray
        case .loaded:          return .green
        case .error:           return .red
        }
    }
    var statusText: String {
        switch viewModel.fetchStatus {
        case .idle:            return "Not started"
        case .loading:         return "Loading…"
        case .loaded(let n):   return "✓ \(n) items loaded — tap to refresh"
        case .error(let msg):  return msg
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

            // Debug status row — shows loading/error/count on the Watch screen
            Section {
                Button(action: { viewModel.fetchItems() }) {
                    HStack {
                        Image(systemName: statusIcon)
                            .foregroundColor(statusColor)
                        Text(statusText)
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                            .lineLimit(3)
                    }
                }
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
