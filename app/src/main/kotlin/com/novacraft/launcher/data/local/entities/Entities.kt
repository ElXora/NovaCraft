package com.novacraft.launcher.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.novacraft.launcher.data.local.Converters

// ─── Account Entity ───────────────────────────────────────────────────────────

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val uuid: String,
    val type: String,             // AccountType name
    val isActive: Boolean,
    val avatarUrl: String?,
    val addedAt: Long
    // Tokens stored in EncryptedSharedPreferences, not Room
)

// ─── Version Entity ──────────────────────────────────────────────────────────

@Entity(tableName = "versions")
@TypeConverters(Converters::class)
data class VersionEntity(
    @PrimaryKey val id: String,
    val minecraftVersion: String,
    val loaderType: String,
    val loaderVersion: String?,
    val releaseType: String,
    val releaseDate: Long,
    val isInstalled: Boolean,
    val installPath: String?,
    val lastPlayed: Long?,
    val playCount: Int,
    val totalPlayTime: Long,
    val iconUrl: String?
)

// ─── Profile Entity ──────────────────────────────────────────────────────────

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val versionId: String,
    val accountId: String?,
    val javaPath: String?,
    val ramMb: Int,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val jvmArgs: String,
    val gameArgs: String,
    val enableOptimizations: Boolean,
    val performancePreset: String,
    val createdAt: Long,
    val lastPlayed: Long?,
    val isFavorite: Boolean,
    val screenshotPath: String?
)

// ─── Mod Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "mods")
@TypeConverters(Converters::class)
data class ModEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val slug: String,
    val description: String,
    val author: String,
    val version: String,
    val loaderTypes: List<String>,
    val gameVersions: List<String>,
    val downloadUrl: String,
    val fileName: String,
    val filePath: String?,
    val sizeBytes: Long,
    val isInstalled: Boolean,
    val isEnabled: Boolean,
    val iconUrl: String?,
    val source: String,
    val downloads: Long,
    val follows: Long
)

// ─── Download Entity ─────────────────────────────────────────────────────────

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val destPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val progress: Float,
    val error: String?,
    val createdAt: Long
)

// ─── Java Runtime Entity ─────────────────────────────────────────────────────

@Entity(tableName = "java_runtimes")
data class JavaRuntimeEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val vendor: String,
    val architecture: String,
    val installPath: String,
    val isInstalled: Boolean,
    val isDefault: Boolean,
    val buildVersion: String
)

// ─── Server Entity ───────────────────────────────────────────────────────────

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val iconUrl: String?,
    val isFavorite: Boolean,
    val lastPing: Long?,
    val playerCount: Int,
    val maxPlayers: Int,
    val motd: String,
    val version: String
)

// ─── News Entity ─────────────────────────────────────────────────────────────

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val url: String,
    val publishedAt: Long,
    val category: String
)

// ─── Crash Report Entity ─────────────────────────────────────────────────────

@Entity(tableName = "crash_reports")
data class CrashReportEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val profileName: String,
    val versionId: String,
    val exitCode: Int,
    val logPath: String,
    val summary: String
)
