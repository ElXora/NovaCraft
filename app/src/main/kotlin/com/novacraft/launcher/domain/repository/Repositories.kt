package com.novacraft.launcher.domain.repository

import com.novacraft.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow

// ─── Account Repository ───────────────────────────────────────────────────────

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeActiveAccount(): Flow<Account?>
    suspend fun getAccount(id: String): Account?
    suspend fun addAccount(account: Account)
    suspend fun removeAccount(id: String)
    suspend fun setActiveAccount(id: String)
    suspend fun refreshAccessToken(id: String): Result<Account>
    suspend fun loginMicrosoft(authCode: String): Result<Account>
}

// ─── Version Repository ───────────────────────────────────────────────────────

interface VersionRepository {
    fun observeInstalledVersions(): Flow<List<GameVersion>>
    suspend fun fetchAvailableVersions(): Result<List<GameVersion>>
    suspend fun getVersion(id: String): GameVersion?
    suspend fun installVersion(version: GameVersion, onProgress: (Float) -> Unit): Result<Unit>
    suspend fun uninstallVersion(id: String): Result<Unit>
    suspend fun isVersionInstalled(id: String): Boolean
}

// ─── Profile Repository ───────────────────────────────────────────────────────

interface ProfileRepository {
    fun observeProfiles(): Flow<List<GameProfile>>
    suspend fun getProfile(id: String): GameProfile?
    suspend fun createProfile(profile: GameProfile)
    suspend fun updateProfile(profile: GameProfile)
    suspend fun deleteProfile(id: String)
    suspend fun duplicateProfile(id: String): GameProfile
}

// ─── Mod Repository ──────────────────────────────────────────────────────────

interface ModRepository {
    fun observeInstalledMods(profileId: String): Flow<List<Mod>>
    suspend fun searchMods(query: String, loaderType: LoaderType, mcVersion: String): Result<List<Mod>>
    suspend fun installMod(mod: Mod, profileId: String, onProgress: (Float) -> Unit): Result<Unit>
    suspend fun uninstallMod(modId: String, profileId: String): Result<Unit>
    suspend fun toggleMod(modId: String, profileId: String, enabled: Boolean)
    suspend fun updateMod(modId: String, profileId: String, onProgress: (Float) -> Unit): Result<Unit>
}

// ─── Java Repository ─────────────────────────────────────────────────────────

interface JavaRepository {
    fun observeRuntimes(): Flow<List<JavaRuntime>>
    suspend fun getDefaultRuntime(mcVersion: String): JavaRuntime?
    suspend fun installRuntime(version: Int, onProgress: (Float) -> Unit): Result<JavaRuntime>
    suspend fun setDefaultRuntime(id: String)
    suspend fun detectSystemJava(): JavaRuntime?
}

// ─── Download Repository ──────────────────────────────────────────────────────

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadTask>>
    suspend fun enqueueDownload(task: DownloadTask): String
    suspend fun cancelDownload(id: String)
    suspend fun pauseDownload(id: String)
    suspend fun resumeDownload(id: String)
    suspend fun clearCompleted()
}

// ─── Settings Repository ─────────────────────────────────────────────────────

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
    suspend fun exportSettings(destPath: String): Result<Unit>
    suspend fun importSettings(srcPath: String): Result<Unit>
    suspend fun resetSettings()
}

// ─── News Repository ─────────────────────────────────────────────────────────

interface NewsRepository {
    suspend fun fetchNews(): Result<List<NewsItem>>
    fun observeCachedNews(): Flow<List<NewsItem>>
}

// ─── Server Repository ────────────────────────────────────────────────────────

interface ServerRepository {
    fun observeServers(): Flow<List<ServerEntry>>
    suspend fun addServer(server: ServerEntry)
    suspend fun removeServer(id: String)
    suspend fun pingServer(address: String, port: Int): Result<ServerEntry>
    suspend fun toggleFavorite(id: String)
}

// ─── World Repository ────────────────────────────────────────────────────────

interface WorldRepository {
    suspend fun getWorlds(gameDir: String): List<World>
    suspend fun deleteWorld(world: World): Result<Unit>
    suspend fun backupWorld(world: World, destDir: String): Result<Unit>
}
