# GroceryCheckList

A real-time, cross-platform grocery list application designed for **Android** (Phone) and **watchOS** (Standalone Apple Watch).

This app allows you to manage your grocery list on your phone and check items off directly from your wrist while at the store, even without your phone nearby.

## Architecture

This project uses a unique **Hybrid Architecture** to support true standalone functionality on Apple Watch while maintaining a robust Android experience.

### Backend

- **Firebase Firestore**: Central database acting as the single source of truth.
- **Data Model**:
  - `groceries` collection.
  - Fields: `name` (string), `isCompleted` (boolean), `order` (int), `createdAt` (timestamp).

### Android App

- **Tech Stack**: Kotlin, Jetpack Compose, Firebase Android SDK.
- **Sync**: Uses standard real-time listeners (`SnapshotListener`) from the Firebase SDK for instant updates.
- **Features**: Add, delete, reorder, and complete items.

### Apple Watch App

- **Tech Stack**: Swift, SwiftUI.
- **Sync**: Uses **Firestore REST API** via `URLSession`.
  - _Why?_ Official Firebase SDK support for standalone watchOS is limited. Using REST ensures the app can run independently over Wi-Fi/LTE without complex linking errors.
- **Sync Strategy**:
  - Fetches data on launch.
  - Polls for updates every 10 seconds while in the foreground.
  - Optimistic UI updates for checking off items.

## Setup Instructions

### Prerequisites

1.  **Android Studio** (for Android app).
2.  **Xcode 15+** (for Watch app).
3.  **Java 17** (required for Android build).

### Firebase Configuration

Since configuration files are git-ignored for security, you must add your own:

1.  Create a Firebase Project.
2.  **Android**:
    - Register an Android app (`com.example.grocery`).
    - Download `google-services.json` and place it in `android/app/`.
3.  **watchOS**:
    - Register an iOS app (`JordanTranchina.GroceryWatch.watchkitapp`).
    - The Watch app talks to Firestore over REST. The project ID and API key are set in `GroceryViewModel.swift`, so no plist is needed to build it.
4.  **Firestore**:
    - Enable Firestore in the Firebase Console.
    - Start in **Test Mode** or configure rules to allow read/write.

### Building

**Android**:

```bash
cd android
./gradlew assembleDebug
```

**watchOS**: Open `ios/GroceryWatch/GroceryWatch.xcodeproj` in Xcode and build for the "GroceryWatch Watch App" scheme.

### Install on your Apple Watch (testing)

You need a Mac with Xcode, your iPhone, and the Apple Watch that is paired to that iPhone.

1. On the iPhone, turn on Developer Mode: **Settings > Privacy & Security > Developer Mode**. The iPhone restarts.
2. Connect the iPhone to the Mac with a cable. Unlock it and tap **Trust**.
3. On the watch, turn on Developer Mode: **Settings > Privacy & Security > Developer Mode**. The watch restarts. (If you do not see the option, do step 6 first, then look again.)
4. Open `ios/GroceryWatch/GroceryWatch.xcodeproj` in Xcode.
5. Select the project, then each target (**GroceryWatch**, **GroceryWatch Watch App**). On the **Signing & Capabilities** tab, set **Team** to your Apple ID team. If Xcode shows a bundle ID error, change the bundle ID prefix to something unique.
6. Open **Window > Devices and Simulators**. Wait until Xcode shows the watch under the iPhone and finishes "Preparing". Keep the watch on its charger and near the iPhone.
7. In the toolbar, select the **GroceryWatch Watch App** scheme and your watch as the run destination.
8. Click **Run** (⌘R). The first install can take several minutes.
9. If the watch says "Untrusted Developer", on the iPhone go to **Settings > General > VPN & Device Management**, select your Apple ID, and tap **Trust**.

With a free Apple ID, the app stops opening after 7 days. Run it from Xcode again to renew it.

The row under "To Buy" shows the sync status. A green check means the list loaded. A red triangle shows the error text.

To run the tests: select the **GroceryWatch Watch App** scheme and a watch simulator, then press ⌘U.

## Features

- ✅ **Real-time Sync**: Updates propagate between devices in seconds.
- ✅ **Standalone Mode**: Watch app works via Wi-Fi/LTE without the phone.
- ✅ **Smart Sorting**: Unchecked items float to the top; completed items sink to the bottom!
