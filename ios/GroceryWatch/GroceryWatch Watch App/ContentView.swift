import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GroceryViewModel()
    
    var body: some View {
        Group {
            if #available(watchOS 9.0, *) {
                NavigationStack {
                    List {
                        Section(header: Text("To Buy")) {
                            ForEach(viewModel.activeItems) { item in
                                HStack {
                                    Image(systemName: "square")
                                        .foregroundStyle(.tint)
                                        .onTapGesture {
                                            viewModel.toggleCompletion(item: item)
                                        }
                                    Text(item.name)
                                }
                            }
                            .onMove(perform: viewModel.moveItem)
                        }
                        
                        if !viewModel.completedItems.isEmpty {
                            Section(header: Text("Completed")) {
                                ForEach(viewModel.completedItems) { item in
                                    HStack {
                                        Image(systemName: "checkmark.square.fill")
                                            .foregroundStyle(.gray)
                                            .onTapGesture {
                                                viewModel.toggleCompletion(item: item)
                                            }
                                        Text(item.name)
                                            .strikethrough()
                                            .foregroundStyle(.gray)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle("Groceries")
                    .toolbar {
                        // Edit button allows reordering
                        // ToolbarItem(placement: .primaryAction) {
                        //    EditButton()
                        // }
                    }
                }
            } else {
                NavigationView {
                    List {
                        Section(header: Text("To Buy")) {
                            ForEach(viewModel.activeItems) { item in
                                HStack {
                                    Image(systemName: "square")
                                        .foregroundStyle(.tint)
                                        .onTapGesture {
                                            viewModel.toggleCompletion(item: item)
                                        }
                                    Text(item.name)
                                }
                            }
                            .onMove(perform: viewModel.moveItem)
                        }
                        
                        if !viewModel.completedItems.isEmpty {
                            Section(header: Text("Completed")) {
                                ForEach(viewModel.completedItems) { item in
                                    HStack {
                                        Image(systemName: "checkmark.square.fill")
                                            .foregroundStyle(.gray)
                                            .onTapGesture {
                                                viewModel.toggleCompletion(item: item)
                                            }
                                        Text(item.name)
                                            .strikethrough()
                                            .foregroundStyle(.gray)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle("Groceries")
                    .toolbar {
                        // Edit button allows reordering
                        // ToolbarItem(placement: .primaryAction) {
                        //    EditButton()
                        // }
                    }
                }
            }
        }
    }
}

#Preview {
    ContentView()
}
