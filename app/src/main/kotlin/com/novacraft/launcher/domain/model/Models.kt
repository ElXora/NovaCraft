package com.novacraft.launcher.domain.model

import java.time.Instant
import java.util.UUID

// ─── Account ─────────────────────────────────────────────────────────────────

/**
 * Represents a player account (Microsoft/offline).
 */
data class Account(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val uuid: String,
    val type: AccountType,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiry: Long? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    MICROSOFT,  // Full Xbox/Minecraft auth
    OFFLINE     // No auth, username only
}

// ─── GameVersion ─────────────────────────────────────────────────────────────

/**
 * Represents a Minecraft version + optional mod loader.
 */
data class GameVersion(
    val id: String,                         // e.g. "1.21.1-fabric-0.15.11"
    val minecraftVersion: String,           // e.g. "1.21.1"
    val loaderType: LoaderType = LoaderType.VANILLA,
    val loaderVersion: String? = null,      // Fabric/Forge/etc. version
    val releaseType: ReleaseType = ReleaseType.RELEASE,
    val releaseDate: Long = 0L,
    val isInstalled: Boolean = false,
    val installPath: String? = null,
    val lastPlayed: Long? = null,
    val playCount: Int = 0,
    val totalPlayTime: Long = 0L,           // milliseconds
    val iconUrl: String? = null
)

enum class LoaderType {
    VANILLA, FABRIC, FORGE, NEOFORGE, QUILT
}

enum class ReleaseType {
    RELEASE, SNAPSHOT, BETA, ALPHA
}

// ─── GameProfile ─────────────────────────────────────────────────────────────

/**
 * A user-created launch profile linking version, settings, and mods.
 */
data class GameProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconEmoji: String = "⛏️",
    val versionId: String,
    val accountId: String? = null,
    val javaPath: String? = null,           // Override default JRE
    val ramMb: Int = 2048,
    val resolution: GameResolution = GameResolution(0, 0), // 0 = auto
    val jvmArgs: String = "",
    val gameArgs: String = "",
    val enableOptimizations: Boolean = true,
    val performancePreset: PerformancePreset = PerformancePreset.BALANCED,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val isFavorite: Boolean = false,
    val screenshotPath: String? = null
)

data class GameResolution(val width: Int, val height: Int)

enum class PerformancePreset {
    LOW_END,     // Low RAM, minimum settings
    BALANCED,    // Default
    HIGH_END,    // High RAM, performance flags
    CUSTOM
}

// ─── Mod ─────────────────────────────────────────────────────────────────────

data class Mod(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
    val description: String = "",
    val author: String = "",
    val version: String = "",
    val loaderTypes: List<LoaderType> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val downloadUrl: String = "",
    val fileName: String = "",
    val filePath: String? = null,
    val sizeBytes: Long = 0L,
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val iconUrl: String? = null,
    val source: ModSource = ModSource.MODRINTH,
    val downloads: Long = 0L,
    val follows: Long = 0L
)

enum class ModSource { MODRINTH, CURSEFORGE, LOCAL }

// ─── ResourcePack ────────────────────────────────────────────────────────────

data class ResourcePack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val version: String = "",
    val filePath: String,
    val iconPath: String? = null,
    val isEnabled: Boolean = false,
    val packFormat: Int = 0,
    val sizeBytes: Long = 0L
)

// ─── Shader ──────────────────────────────────────────────────────────────────

data class Shader(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val author: String = "",
    val filePath: String,
    val previewUrl: String? = null,
    val isEnabled: Boolean = false,
    val requiredMod: String? = null         // e.g. "Iris Shaders"
)

// ─── Download ─────────────────────────────────────────────────────────────────

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val destPath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,              // 0.0..1.0
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }

// ─── Java Runtime ────────────────────────────────────────────────────────────

data class JavaRuntime(
    val id: String = UUID.randomUUID().toString(),
    val version: Int,                       // 8, 17, 21
    val vendor: String = "Adoptium",
    val architecture: String = "aarch64",
    val installPath: String,
    val isInstalled: Boolean = false,
    val isDefault: Boolean = false,
    val buildVersion: String = ""
)

// ─── World ────────────────────────────────────────────────────────────────────

data class World(
    val name: String,
    val dirPath: String,
    val gameModeId: Int = 0,
    val lastPlayed: Long = 0L,
    val sizeBytes: Long = 0L,
    val iconPath: String? = null,
    val versionId: String? = null
)

// ─── Server ──────────────────────────────────────────────────────────────────

data class ServerEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val port: Int = 25565,
    val iconUrl: String? = null,
    val isFavorite: Boolean = false,
    val lastPing: Long? = null,
    val playerCount: Int = 0,
    val maxPlayers: Int = 0,
    val motd: String = "",
    val version: String = ""
)

// ─── News ────────────────────────────────────────────────────────────────────

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String? = null,
    val url: String,
    val publishedAt: Long,
    val category: String = "UPDATE"
)

// ─── Launch Config ────────────────────────────────────────────────────────────

data class LaunchConfig(
    val profile: GameProfile,
    val account: Account,
    val version: GameVersion,
    val javaRuntime: JavaRuntime,
    val gameDir: String,
    val assetsDir: String,
    val librariesDir: String,
    val nativesDir: String
)

// ─── Settings ────────────────────────────────────────────────────────────────

data class AppSettings(
    val defaultRamMb: Int = 2048,
    val defaultJvmArgs: String = "-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M",
    val gameDir: String = "",
    val downloadThreads: Int = 4,
    val autoCheckUpdates: Boolean = true,
    val enableCrashReports: Boolean = true,
    val themeConfig: String = "DARK_NOVA",
    val language: String = "en",
    val enableAnalytics: Boolean = false,
    val showFpsCounter: Boolean = false,
    val showPerfOverlay: Boolean = false,
    val enableGyroAim: Boolean = false,
    val hapticFeedback: Boolean = true,
    val controlLayout: String = "DEFAULT",
    val cloudSyncEnabled: Boolean = false,
    val cloudSyncToken: String? = null,
    val lastBackupDate: Long? = null
)

// ─── Crash Report ────────────────────────────────────────────────────────────

data class CrashReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val profileName: String,
    val versionId: String,
    val exitCode: Int,
    val logPath: String,
    val summary: String
)
