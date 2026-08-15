# GUGA Reading - Android Client

[![License](https://img.shields.io/badge/license-ISC-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.10-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/android-24+-brightgreen.svg)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/jetpack_compose-BOM_2026.02.01-teal.svg)](https://developer.android.com/jetpack/compose)

[中文版](./README.zh-CN.md)

The **Android Native Client** for GUGA Reading, built with Kotlin and Jetpack Compose, delivering a
smooth mobile reading experience.

## 📱 App Overview

Reading Android is the mobile application for the GUGA Reading online platform, providing users with
convenient book reading, bookmarking, and management features. Built with a modern Android
development stack, the app supports category browsing, search, reading progress synchronization, and
more.

### ✨ Core Features

- **📖 Quality Reading Experience**: Smooth page turning animations, customizable font sizes and
  themes
- **🌙 Night Mode**: Eye-friendly dark theme support
- **🔄 Progress Sync**: Real-time reading progress synchronization with the backend
- **🔍 Book Search**: Quick search for books across categories
- **📚 Bookshelf Management**: Personalized bookshelf with favorites management
- **⚡ High Performance**: Jetpack Compose declarative UI with smooth animations

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         Android App (Kotlin)            │
├─────────────────────────────────────────┤
│   UI Layer (Jetpack Compose + Material3)│
│  ┌──────────┐  ┌───────────────────┐    │
│  │  Views   │  │   Components      │    │
│  └──────────┘  └───────────────────┘    │
├─────────────────────────────────────────┤
│   ViewModel Layer                       │
│  ┌─────────────────────────────────┐    │
│  │   StateFlow + ViewModelScope    │    │
│  └─────────────────────────────────┘    │
├─────────────────────────────────────────┤
│   Data Layer                            │
│  ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │  Ktor    │ │DataStore │ │Security│  │
│  │ Client   │ │Preferences│ │ Crypto │  │
│  └──────────┘ └──────────┘ └────────┘  │
├─────────────────────────────────────────┤
│          Network (HTTP/HTTPS)           │
└──────────────────┬──────────────────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Backend API       │
        └─────────────────────┘
```

### Architecture Notes

- **UI Layer**: Built with Jetpack Compose + Material3 for declarative UI
- **ViewModel Layer**: Uses StateFlow for UI state management and viewModelScope for coroutine
  lifecycle
- **Data Layer**: Network requests (Ktor Client), local storage (DataStore Preferences), and
  encryption (Security Crypto)
- **Navigation**: Navigation Compose for page routing with bottom navigation bar and parameter
  passing

## 🛠️ Tech Stack

| Technology          | Version                          | Description                         |
| ------------------- | -------------------------------- | ----------------------------------- |
| **Language**        | Kotlin 2.2.10                    | Modern Android development language |
| **Min SDK**         | API 24 (Android 7.0)             | Compatible with most devices        |
| **Target SDK**      | API 36 (Android 15)              | Latest Android features             |
| **UI Framework**    | Jetpack Compose (BOM 2026.02.01) | Declarative UI toolkit              |
| **Material Design** | Material3                        | Modern design system                |
| **Networking**      | Ktor Client 3.5.0                | Lightweight HTTP client             |
| **Serialization**   | kotlinx.serialization 1.6.2      | JSON serialization                  |
| **Image Loading**   | Coil 2.7.0                       | Efficient image loading library     |
| **Navigation**      | Navigation Compose 2.9.8         | Page navigation                     |
| **Local Storage**   | DataStore Preferences 1.0.0      | Type-safe key-value storage         |
| **Secure Storage**  | Security Crypto 1.1.0-alpha06    | Encrypted sensitive data            |
| **Async**           | Kotlin Coroutines 1.11.0         | Coroutine support                   |
| **Build Tools**     | Gradle Kotlin DSL + AGP 9.2.1    | Type-safe build configuration       |

## 📁 Project Structure

```
reading/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/qianrenni/reading/
│   │   │   │   ├── MainActivity.kt              # Main Activity
│   │   │   │   ├── Router.kt                    # Route configuration
│   │   │   │   ├── common/                      # Common utilities
│   │   │   │   │   └── uiState.kt               # Base UI state
│   │   │   │   ├── components/                  # Reusable UI components
│   │   │   │   │   ├── BookItem.kt              # Book card component
│   │   │   │   │   ├── BottomControlBar.kt      # Reading bottom control bar
│   │   │   │   │   ├── BottomNavigationBar.kt   # Bottom navigation bar
│   │   │   │   │   ├── CaptchaImage.kt          # CAPTCHA image component
│   │   │   │   │   ├── CatalogDrawer.kt         # Chapter catalog drawer
│   │   │   │   │   ├── CommonPage.kt            # Common page layout
│   │   │   │   │   ├── InfinitePager.kt         # Infinite pager component
│   │   │   │   │   └── ReadingSettingsDialog.kt # Reading settings dialog
│   │   │   │   ├── data/                        # Data layer
│   │   │   │   │   ├── api/                     # API interfaces
│   │   │   │   │   │   ├── AuthService.kt       # Authentication API
│   │   │   │   │   │   ├── BookService.kt       # Book API
│   │   │   │   │   │   ├── NetworkClient.kt     # HTTP client (includes BASE_URL)
│   │   │   │   │   │   ├── ReadingProgressService.kt # Reading progress API
│   │   │   │   │   │   ├── ReportService.kt     # Report API
│   │   │   │   │   │   ├── ResponseHandler.kt   # Response handler
│   │   │   │   │   │   ├── ShelfService.kt      # Bookshelf API
│   │   │   │   │   │   └── UserService.kt       # User API
│   │   │   │   │   ├── model/                   # Data models
│   │   │   │   │   │   ├── ApiResponse.kt       # Generic API response
│   │   │   │   │   │   ├── Book.kt              # Book model
│   │   │   │   │   │   ├── ReadSetting.kt       # Reading settings model
│   │   │   │   │   │   ├── ReadingProgress.kt   # Reading progress model
│   │   │   │   │   │   └── User.kt              # User model
│   │   │   │   │   └── store/                   # Local persistence
│   │   │   │   │       ├── AuthStore.kt         # Auth state local storage
│   │   │   │   │       └── SettingRepository.kt # User settings storage
│   │   │   │   ├── ui/theme/                    # Theme system
│   │   │   │   │   ├── Theme.kt                 # Light/Dark theme definition
│   │   │   │   │   └── Type.kt                  # Typography styles
│   │   │   │   ├── util/                        # Utility classes
│   │   │   │   │   ├── SnackBarManager.kt       # SnackBar message manager
│   │   │   │   │   ├── SystemBarUtils.kt        # System bar utility
│   │   │   │   │   └── tool.kt                  # General utility functions
│   │   │   │   ├── viewmodels/                  # ViewModel layer
│   │   │   │   │   ├── auth/                    # Auth ViewModels
│   │   │   │   │   │   ├── AuthViewModel.kt     # Auth state management
│   │   │   │   │   │   ├── LoginViewModel.kt    # Login logic
│   │   │   │   │   │   ├── RegisterViewModel.kt # Registration logic
│   │   │   │   │   │   ├── ForgetPasswordViewModel.kt # Password reset logic
│   │   │   │   │   │   └── UpdatePasswordViewModel.kt  # Password change logic
│   │   │   │   │   └── book/                    # Book ViewModels
│   │   │   │   │       ├── BookInfoViewModel.kt # Book detail logic
│   │   │   │   │       ├── BookReadViewModel.kt # Reading logic
│   │   │   │   │       ├── HistoryViewModel.kt  # Reading history
│   │   │   │   │       ├── HomeViewModel.kt     # Home/Bookstore logic
│   │   │   │   │       └── ShelfViewModel.kt    # Bookshelf management
│   │   │   │   └── views/                       # Screen views
│   │   │   │       ├── HomeView.kt              # Home (Bookstore)
│   │   │   │       ├── auth/                    # Auth screens
│   │   │   │       │   ├── Login.kt             # Login screen
│   │   │   │       │   ├── Register.kt          # Registration screen
│   │   │   │       │   ├── ForgetPassword.kt    # Password reset screen
│   │   │   │       │   └── UpdatePassword.kt    # Password change screen
│   │   │   │       ├── book/                    # Book screens
│   │   │   │       │   ├── BookInfoView.kt      # Book detail screen
│   │   │   │       │   ├── BookReadView.kt      # Reading screen
│   │   │   │       │   ├── BookShelfView.kt     # Bookshelf screen
│   │   │   │       │   └── ReadingHistoryView.kt # Reading history screen
│   │   │   │       └── user/                    # User screens
│   │   │   │           └── ProfileView.kt       # Profile screen
│   │   │   ├── res/                             # Resources
│   │   │   │   ├── drawable/                    # Drawable resources
│   │   │   │   ├── values/                      # Strings, colors, themes
│   │   │   │   ├── xml/                         # XML configuration files
│   │   │   │   └── mipmap-*/                    # App icons
│   │   │   └── AndroidManifest.xml              # App manifest
│   │   ├── test/                                # Unit tests
│   │   └── androidTest/                         # Instrumented tests
│   ├── build.gradle.kts                         # Module build configuration
│   └── proguard-rules.pro                       # ProGuard/R8 rules
├── gradle/
│   ├── wrapper/                                 # Gradle Wrapper
│   └── libs.versions.toml                       # Version catalog
├── build.gradle.kts                             # Project-level build config
├── settings.gradle.kts                          # Project settings
├── gradle.properties                            # Gradle properties
├── local.properties                             # Local config (keystore, etc., not committed)
├── gradlew                                      # Gradle Wrapper (Unix)
├── gradlew.bat                                  # Gradle Wrapper (Windows)
└── README.md                                    # Project README (English)
```

## 🚀 Quick Start

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 11 or higher
- **Android SDK**:
  - Compile SDK: 36
  - Min SDK: 24
  - Target SDK: 36
- **Gradle**: 8.0+
- **Kotlin**: 2.2.10+

### Installation

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd reading
```

#### 2. Configure Backend API URL

Modify the backend API base URL
in [NetworkClient.kt](app/src/main/java/com/qianrenni/reading/data/api/NetworkClient.kt):

```kotlin
// NetworkClient.kt
object NetworkClient {
    private const val BASE_URL =
        "http://49.235.107.221:8000/" // Replace with your actual API base URL
    // ...
}
```

#### 3. Configure Signing (Optional, for Release Builds)

Add signing configuration in the `local.properties` file:

```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

> ⚠️ **Note**: `local.properties` is listed in `.gitignore` and will not be committed to version
> control.

#### 4. Open in Android Studio

1. Launch Android Studio
2. Select **File** → **Open**
3. Choose the `reading` directory
4. Wait for Gradle sync to complete (first sync may take several minutes to download dependencies)

#### 5. Run the App

**Via Android Studio:**

- Connect an Android device or start an emulator
- Click the **Run** button (green triangle) in the toolbar
- Or press `Shift + F10` (Windows/Linux)

**Via Command Line:**

```bash
# macOS/Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### 6. Install on Device

```bash
# Install debug build
./gradlew installDebug

# Or directly via adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📖 Main Features

### 1. User Authentication

- Registration (with CAPTCHA support)
- Login (with CAPTCHA, JWT Token auto-management)
- Password reset (forgot password flow)
- Password change

### 2. Book Browsing (Bookstore)

- Category browsing (multi-category switching)
- Full-text search (debounce support, search results)
- Popular recommendations
- New arrivals
- Paginated loading (infinite scroll)

### 3. Reading Experience

- Chapter list display
- Smooth page turning / swipe reading (InfinitePager component)
- Font size adjustment
- Background theme switching (light/dark mode)
- Auto-save and sync of reading progress
- Resume reading from last position
- Chapter navigation (side drawer catalog)

### 4. Bookshelf Management

- Add / remove favorites
- Favorites list display
- Reading history records
- Recently read books

### 5. User Profile

- User info display
- Profile editing

### 6. Interactive Features (Planned)

- Chapter comments — Planned
- Likes — Planned
- Book sharing — Planned

### 7. Offline Support (Planned)

- Chapter content caching — Planned
- Offline reading mode — Planned
- Cache management — Planned

## 🔧 Development Guide

### Build Variants

The project supports the following build variants:

- **debug**: Debug build with logging and debug tools enabled
- **release**: Release build with performance optimization and code obfuscation

### Useful Gradle Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

### Code Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add necessary comments and documentation
- Keep functions focused on a single responsibility
- Follow Compose best practices

### Debugging Tips

1. **Enable Logging**: Debug builds output detailed logs
2. **Layout Inspector**: Use Android Studio's Layout Inspector to inspect Compose UI
3. **Network Profiler**: Monitor network requests and responses
4. **Logcat**: View runtime logs, filter by the `NetworkClient` tag for network requests

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`

### Instrumented Tests

```bash
# Requires a connected device or emulator
./gradlew connectedAndroidTest
```

### UI Tests

Using the Compose Testing API for UI tests:

```kotlin
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testAppLaunch() {
        // Example instrumented test
    }
}
```

## 📊 Performance Optimization

### Optimizations Applied

- **🖼️ Image Optimization**: Coil automatic caching and image loading optimization
- **📡 Network Optimization**: Ktor connection pooling, timeout control (connect/read timeout 30s)
- **💾 Local Caching**: DataStore persistence for user preferences and reading progress
- **🔋 Battery Optimization**: Structured concurrency with viewModelScope to prevent memory leaks
- **🎨 UI Optimization**: Compose recomposition optimization, lazy loading lists
- **📦 APK Size**: ProGuard/R8 code shrinking
- **🔍 Search Debounce**: 300ms debounce on search input to reduce unnecessary API calls

### Performance Benchmarks

- Cold start time: < 2 seconds
- First paint: < 500ms
- List scrolling: 60 FPS
- Memory usage: < 100MB (typical scenarios)

## 🔒 Security Features

- **🔐 Network Communication**: HTTPS support, HTTP allowed for development (`usesCleartextTraffic`)
- **🔑 Token Management**: JWT token securely stored via AuthStore and automatically attached to
  request headers
- **🛡️ Data Encryption**: Sensitive data encrypted using Security Crypto
- **🚫 Minimal Permissions**: Only INTERNET and ACCESS_NETWORK_STATE permissions requested
- **📝 Input Validation**: Client-side input validation

## 📱 Compatibility

### Supported Android Versions

- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 15 (API 36)
- **Recommended**: Android 10+ (API 29+)

### Screen Adaptation

- Phone and tablet support
- Adaptive layouts
- Portrait and landscape orientation support

### CPU Architectures

- armeabi-v7a
- arm64-v8a
- x86
- x86_64

## 🐛 FAQ

### 1. Gradle sync failed

**Solution**:

- Check your network connection
- Clear Gradle cache: `./gradlew clean`
- Update Android Studio to the latest version
- Verify JDK version is 11 or higher

### 2. Cannot connect to backend API

**Solution**:

- Verify the `BASE_URL` configuration
  in [NetworkClient.kt](app/src/main/java/com/qianrenni/reading/data/api/NetworkClient.kt)
- Ensure the backend service is running
- Check network connectivity and firewall settings
- Check Logcat for network errors under the `NetworkClient` tag

### 3. App crashes

**Solution**:

- Check Logcat for crash logs
- Verify the device meets minimum requirements (API 24+)
- Ensure sufficient storage space
- Try clearing app data and reinstalling

### 4. Image loading fails

**Solution**:

- Check network connectivity
- Verify the image URL is accessible
- Restart the app

## 📦 Release Process

### 1. Generate a Signing Key

```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

### 2. Configure Signing

Add signing information in `local.properties` (see configuration steps above).

### 3. Build Release APK

```bash
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

### 4. Test the Release Build

Thoroughly test on real devices before publishing.

### 5. Upload to App Stores

- Google Play Store
- Huawei AppGallery
- Xiaomi App Store
- Other Chinese app stores

## 🤝 Contributing

Issues and Pull Requests are welcome!

### Contribution Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Standards

- Follow Kotlin official coding conventions
- Use Compose best practices
- Add necessary comments and documentation
- Ensure tests pass
- Keep commit messages clear and descriptive

## 📄 License

ISC License

## 👥 Contact

- **Author**: qianrenni
- **Email**: 2112183503@qq.com
- **Project Homepage**: [GUGA Reading](https://github.com/Qianrenni/guga_reading)

## 🔗 Related Links

- [GUGA Reading Web App](http://49.235.107.221)
- [Author Portal](http://49.235.107.221/author/#)
- [Admin Portal](http://49.235.107.221/admin/#)
- [Backend API Docs](http://49.235.107.221:8000/docs)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

---

**Note**: This project is for educational purposes only. Do not use for commercial purposes.
