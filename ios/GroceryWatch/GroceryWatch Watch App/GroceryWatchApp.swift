//
//  GroceryWatchApp.swift
//  GroceryWatch Watch App
//
//  Created by Jordan Tranchina on 1/10/26.
//

import SwiftUI
import FirebaseCore

class ExtensionDelegate: NSObject, WKApplicationDelegate {
    func applicationDidFinishLaunching() {
        FirebaseApp.configure()
    }
}

@main
struct GroceryWatch_Watch_AppApp: App {
    @WKApplicationDelegateAdaptor(ExtensionDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
