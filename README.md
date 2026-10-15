# BillSplit

BillSplit is an intuitive and modern Android app designed to help you split bills effortlessly with your friends or groups. It calculates individual shares based on various split methods and supports features like multiple currencies, calculation history, and customizable settings.

## Features
- **Split Methods**: Choose between equal split, percentage-based, or exact amounts to cater to any scenario.
- **Currency Support**: Easily select and manage different currencies.
- **History Tracking**: Keeps a history of your past bill splits for quick reference and tracking.
- **Customization & Settings**: Dark and Light themes (Dynamic Colors supported on modern Android versions), preserving last split states, and configurable default currency.
- **Modern UI**: Built entirely with Jetpack Compose following Material Design 3 guidelines for a beautiful edge-to-edge layout.
- **Local Persistence**: Uses preferences or local storage to save your settings and history seamlessly.

## Tech Stack
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Language**: Kotlin
- **Concurrency**: Coroutines and StateFlow
- **Data Persistence**: Local repositories (SharedPreferences/DataStore)

## Getting Started

### Prerequisites
- Android Studio (latest stable version recommended)
- Android SDK 33+

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/BillSplit.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on an emulator or a physical device.

## License
[MIT License](LICENSE) (or configure your preferred license)
