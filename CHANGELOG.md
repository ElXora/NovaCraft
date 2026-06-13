# Changelog

All notable changes to NovaCraft Launcher will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Real LWJGL Android JNI bridge integration
- Modpack installer (Modrinth/CurseForge)
- Screenshot gallery
- Server browser with live SLP ping
- World manager with backup/restore
- Controller button remapping UI
- In-app shader preview

---

## [1.0.0] - 2025-06-13

### Added
- **Full launcher UI** — Home, Play, Versions, Mods, Files, Accounts, Settings screens
- **Microsoft authentication** — full MS → XBL → XSTS → Minecraft auth chain
- **Offline accounts** — username-only with offline UUID generation
- **Version manager** — fetches all Minecraft releases from Mojang, filter by loader and release type
- **Mod manager** — Modrinth API search, install, enable/disable, uninstall
- **Java runtime manager** — auto-download Adoptium JRE 8/17/21 for AArch64
- **Launch engine** — full JVM argument builder with classpath resolution
- **Touch control overlay** — 24+ configurable on-screen buttons with drag-and-drop editor
- **Gyroscope aiming** — device gyro mapped to camera movement
- **Performance overlay** — live FPS, RAM, CPU, and ping HUD
- **Built-in file explorer** — browse NovaCraft game directories
- **Crash reporter** — captures exit codes and crash logs
- **Settings** — RAM slider, JVM args, performance presets, theme, controls
- **Dark Nova / Light Nova / Abyss themes** — glassmorphism Material 3 UI
- **CI/CD** — GitHub Actions for build, test, sign, and GitHub Release

### Architecture
- MVVM + Clean Architecture (Domain / Data / UI layers)
- Hilt dependency injection throughout
- Room database for all persistent state
- Kotlinx Coroutines + Flow for reactive data
- DataStore Preferences for settings
- WorkManager for background tasks
- EncryptedSharedPreferences for token storage

---

[Unreleased]: https://github.com/ElXora/NovaCraftLauncher/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/ElXora/NovaCraftLauncher/releases/tag/v1.0.0
