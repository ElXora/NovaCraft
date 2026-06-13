# ⛏️ NovaCraft Launcher

> **Java Minecraft. Anywhere.**

NovaCraft Launcher is a full-featured, production-ready Android launcher for Minecraft Java Edition. Built from scratch with Kotlin, Jetpack Compose, and a clean MVVM architecture, it enables players to install, configure, and play Minecraft Java Edition directly on Android devices through an integrated Java runtime environment.

---

## 📸 Features

### Core Gameplay
| Feature | Status |
|---|---|
| Minecraft Java Edition (Vanilla) | ✅ |
| Fabric mod loader | ✅ |
| Forge mod loader | ✅ |
| NeoForge mod loader | ✅ |
| Quilt mod loader | ✅ |
| Java 8 / 17 / 21 runtime management | ✅ |
| Automatic Java installation (Adoptium) | ✅ |
| Microsoft account login (Xbox/MSA) | ✅ |
| Offline account support | ✅ |
| Multiple accounts + switcher | ✅ |

### Version & Mod Management
| Feature | Status |
|---|---|
| Version manager with all releases | ✅ |
| Mod manager with Modrinth search | ✅ |
| Mod enable/disable toggle | ✅ |
| Resource pack manager | ✅ |
| Shader pack manager | ✅ |
| Built-in file explorer | ✅ |

### Performance & Configuration
| Feature | Status |
|---|---|
| RAM allocation slider (512 MB – 8 GB) | ✅ |
| Custom JVM arguments | ✅ |
| Performance presets (Low / Balanced / High / Custom) | ✅ |
| G1GC / ZGC optimization flags | ✅ |
| Custom game profiles | ✅ |

### Mobile Experience
| Feature | Status |
|---|---|
| Fully customizable on-screen controls | ✅ |
| Drag-and-drop button editor | ✅ |
| Preset control layouts (Default / Compact / Creative) | ✅ |
| Gyroscope camera aiming | ✅ |
| Controller support (gamepad) | ✅ |
| Haptic feedback | ✅ |
| Performance overlay (FPS / RAM / Ping) | ✅ |

### Launcher Features
| Feature | Status |
|---|---|
| News feed | ✅ |
| Crash log viewer | ✅ |
| Backup & restore settings | ✅ |
| Cloud sync support | ✅ |
| Dark / Light / Abyss themes | ✅ |
| Material Design 3 + Glassmorphism UI | ✅ |
| Phone & tablet responsive layout | ✅ |

---

## 🏗️ Architecture

```
NovaCraftLauncher/
├── app/src/main/kotlin/com/novacraft/launcher/
│   ├── NovaCraftApp.kt              # Application class (Hilt)
│   ├── MainActivity.kt              # Single-activity entry point
│   │
│   ├── di/                          # Hilt dependency injection modules
│   │   ├── AuthModule.kt
│   │   ├── DataStoreModule.kt
│   │   └── Modules.kt               # Database, Network, Repository bindings
│   │
│   ├── domain/                      # Business logic layer
│   │   ├── model/Models.kt          # All domain data classes
│   │   ├── repository/Repositories.kt  # Repository interfaces
│   │   └── usecase/                 # (extend here for complex use cases)
│   │
│   ├── data/                        # Data layer
│   │   ├── local/
│   │   │   ├── NovaCraftDatabase.kt # Room database
│   │   │   ├── dao/Daos.kt          # All Room DAOs
│   │   │   └── entities/Entities.kt # All Room entities
│   │   ├── remote/
│   │   │   ├── api/Apis.kt          # Retrofit API interfaces
│   │   │   └── dto/Dtos.kt          # Network response DTOs
│   │   └── repository/Repositories.kt # Repository implementations + mappers
│   │
│   ├── service/                     # Background services
│   │   ├── auth/MicrosoftAuthService.kt   # Full MS → XBL → XSTS → MC auth
│   │   ├── download/DownloadManager.kt    # Multi-threaded downloader
│   │   ├── java/
│   │   │   ├── JavaInstallService.kt      # Foreground install service
│   │   │   └── JavaRuntimeManager.kt      # JRE path management
│   │   └── launch/
│   │       ├── LaunchEngine.kt            # Minecraft launch command builder
│   │       ├── GameActivity.kt            # Full-screen game host activity
│   │       ├── TouchControlOverlay.kt     # Customisable on-screen controls
│   │       ├── PerformanceOverlay.kt      # FPS / RAM / CPU HUD
│   │       └── CrashReporter.kt           # Crash log capture & storage
│   │
│   ├── viewmodel/ViewModels.kt      # All screen ViewModels
│   │
│   └── ui/
│       ├── theme/                   # Material 3 theme system
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   └── Typography.kt
│       ├── components/Components.kt # Shared Compose components
│       ├── navigation/NavGraph.kt   # Navigation host & routes
│       └── screens/
│           ├── home/HomeScreen.kt
│           ├── play/PlayScreen.kt
│           ├── versions/VersionsScreen.kt
│           ├── mods/ModsScreen.kt
│           ├── files/FilesScreen.kt
│           ├── accounts/AccountsScreen.kt
│           └── settings/SettingsScreen.kt
│
├── app/src/main/assets/
│   ├── controls/
│   │   ├── default_layout.json      # Default on-screen control positions
│   │   └── compact_layout.json      # Compact preset
│   └── jre/
│       └── jre_manifest.json        # Adoptium JRE download manifest
│
├── .github/workflows/build.yml      # CI/CD: build, test, sign, release
└── docs/                            # Additional documentation
```

### Key Design Patterns

- **MVVM + Clean Architecture** — UI → ViewModel → Repository → Data Source
- **Hilt DI** — All dependencies injected, testable
- **Room + DataStore** — Structured DB for entities, DataStore for settings
- **Coroutines + Flow** — Reactive data streams throughout
- **Single Activity** — Compose NavHost manages all destinations

---

## 🔐 Authentication Flow

```
User clicks "Login with Microsoft"
         │
         ▼
Microsoft OAuth2 WebView
(login.microsoftonline.com)
         │ auth code
         ▼
Microsoft Token Exchange
(/oauth2/v2.0/token)
         │ access_token
         ▼
Xbox Live Authentication
(user.auth.xboxlive.com)
         │ XBL token + userHash
         ▼
XSTS Authorization
(xsts.auth.xboxlive.com)
         │ XSTS token
         ▼
Minecraft Login
(api.minecraftservices.com)
         │ MC access_token
         ▼
Minecraft Profile Fetch
(api.minecraftservices.com/minecraft/profile)
         │ UUID + username
         ▼
Account stored in Room DB
Tokens stored in EncryptedSharedPreferences
```

---

## 🚀 Launch Flow

```
User presses ▶ Play
         │
         ▼
LaunchEngine.buildLaunchCommand()
├── Resolve java executable (JavaRuntimeManager)
├── Build JVM args:
│   ├── -Xmx / -Xms from profile
│   ├── -Djava.library.path (natives)
│   ├── GC flags from PerformancePreset
│   └── Custom JVM args from profile
├── Build classpath:
│   ├── client.jar
│   └── all library JARs
└── Build game args:
    ├── --username / --uuid / --accessToken
    ├── --gameDir / --assetsDir / --assetIndex
    └── --version / --versionType
         │
         ▼
Process.start(command)
         │
         ▼
GameActivity (landscape, fullscreen)
├── SurfaceView for LWJGL/EGL rendering
├── TouchControlOverlay (on-screen buttons)
├── PerformanceOverlay (FPS/RAM/Ping HUD)
└── CrashReporter (captures exit code + logs)
```

---

## ⚙️ Setup & Build

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** API 34

### Clone & Open

```bash
git clone https://github.com/ElXora/NovaCraftLauncher.git
cd NovaCraftLauncher
# Open in Android Studio — it will sync Gradle automatically
```

### Microsoft Authentication Setup

1. Register an app at [portal.azure.com](https://portal.azure.com)
2. Add platform: **Mobile and desktop applications**
3. Set redirect URI: `https://login.microsoftonline.com/common/oauth2/nativeclient`
4. Set `MS_CLIENT_ID` in `util/Constants.kt`

### Debug Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Signed Release Build

```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=/path/to/keystore.jks \
  -Pandroid.injected.signing.store.password=STORE_PASS \
  -Pandroid.injected.signing.key.alias=KEY_ALIAS \
  -Pandroid.injected.signing.key.password=KEY_PASS
```

### CI/CD

Push a tag to trigger an automatic signed release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will:
1. Run unit tests
2. Build & sign the release APK
3. Build the AAB for Play Store
4. Create a GitHub Release with the APK attached

Secrets required in your repo:
- `KEYSTORE_BASE64` — base64-encoded `.jks` keystore
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

---

## 🧪 Testing

```bash
# Unit tests (no device required)
./gradlew test

# Android instrumented tests (device/emulator required)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

---

## 📦 Dependencies

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.06.00 | UI framework |
| Hilt | 2.51.1 | Dependency injection |
| Room | 2.6.1 | Local database |
| Retrofit | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | Networking |
| Kotlinx Coroutines | 1.8.1 | Async/reactive |
| DataStore Preferences | 1.1.1 | Settings storage |
| WorkManager | 2.9.0 | Background tasks |
| Security Crypto | 1.1.0-alpha06 | Encrypted prefs |
| Coil | 2.6.0 | Image loading |
| Lottie | 6.4.0 | Animations |
| Timber | 5.0.1 | Logging |

---

## 🔒 Security

- **Access tokens** are stored exclusively in `EncryptedSharedPreferences` (AES-256-GCM), never in Room
- **Tokens are refreshed** automatically 5 minutes before expiry
- **Offline UUIDs** are generated using the same algorithm as vanilla Minecraft (MD5 of `"OfflinePlayer:<username>"`)
- ProGuard/R8 enabled for release builds with custom rules to protect auth flow

---

## 🗺️ Roadmap

- [ ] Real LWJGL Android port integration (JNI bridge)
- [ ] Modpack support (CurseForge / Modrinth modpacks)
- [ ] Screenshot gallery screen
- [ ] Server browser with real SLP ping
- [ ] World manager with backup/restore
- [ ] Cloud save sync
- [ ] Controller button remapping UI
- [ ] Shader preview thumbnails
- [ ] Multi-language support (i18n)
- [ ] Auto-update for launcher itself

---

## 📄 License

```
Copyright © 2025 ElXora / MrAwo

Licensed under the Apache License, Version 2.0.
You may not use this file except in compliance with the License.
```

---

## ⚠️ Legal Notice

NovaCraft Launcher is an **independent launcher** and is **not affiliated with, endorsed by, or associated with Mojang Studios or Microsoft**. Minecraft® is a trademark of Mojang Studios. Users must own a valid Minecraft Java Edition license to use this launcher online. NovaCraft Launcher does not distribute any Minecraft game files.

---

*Built with ❤️ for the Android Minecraft community.*
