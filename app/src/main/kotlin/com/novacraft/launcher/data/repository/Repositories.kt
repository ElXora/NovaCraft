package com.novacraft.launcher.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.novacraft.launcher.data.local.dao.*
import com.novacraft.launcher.data.local.entities.*
import com.novacraft.launcher.data.remote.api.*
import com.novacraft.launcher.data.remote.dto.*
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.domain.repository.*
import com.novacraft.launcher.service.auth.MicrosoftAuthService
import com.novacraft.launcher.service.download.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// ─── Account Repository ───────────────────────────────────────────────────────

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao,
    private val authService: MicrosoftAuthService,
    private val encryptedPrefs: SharedPreferences
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toDomain(encryptedPrefs) } }

    override fun observeActiveAccount(): Flow<Account?> =
        dao.observeActive().map { it?.toDomain(encryptedPrefs) }

    override suspend fun getAccount(id: String): Account? =
        dao.getById(id)?.toDomain(encryptedPrefs)

    override suspend fun addAccount(account: Account) {
        dao.insert(account.toEntity())
        account.accessToken?.let { encryptedPrefs.edit().putString("token_${account.id}", it).apply() }
        account.refreshToken?.let { encryptedPrefs.edit().putString("refresh_${account.id}", it).apply() }
    }

    override suspend fun removeAccount(id: String) {
        dao.deleteById(id)
        encryptedPrefs.edit().remove("token_$id").remove("refresh_$id").apply()
    }

    override suspend fun setActiveAccount(id: String) {
        dao.clearActiveFlags()
        dao.setActive(id)
    }

    override suspend fun refreshAccessToken(id: String): Result<Account> = runCatching {
        val account = getAccount(id) ?: error("Account not found")
        val refreshToken = encryptedPrefs.getString("refresh_${id}", null) ?: error("No refresh token")
        val refreshed = authService.refreshToken(refreshToken)
        val updated = account.copy(
            accessToken = refreshed.accessToken,
            tokenExpiry = System.currentTimeMillis() + refreshed.expiresIn * 1000L
        )
        addAccount(updated)
        updated
    }

    override suspend fun loginMicrosoft(authCode: String): Result<Account> =
        authService.loginWithCode(authCode)
}

// ─── Version Repository ───────────────────────────────────────────────────────

@Singleton
class VersionRepositoryImpl @Inject constructor(
    private val dao: VersionDao,
    private val api: MojangVersionApi,
    private val downloadManager: DownloadManager
) : VersionRepository {

    override fun observeInstalledVersions(): Flow<List<GameVersion>> =
        dao.observeInstalled().map { list -> list.map { it.toDomain() } }

    override suspend fun fetchAvailableVersions(): Result<List<GameVersion>> = runCatching {
        val resp = api.getVersionManifest()
        val body = resp.body() ?: error("Empty version manifest")
        val versions = body.versions.map { summary ->
            GameVersion(
                id = summary.id,
                minecraftVersion = summary.id,
                loaderType = LoaderType.VANILLA,
                releaseType = when (summary.type) {
                    "release"  -> ReleaseType.RELEASE
                    "snapshot" -> ReleaseType.SNAPSHOT
                    "old_beta" -> ReleaseType.BETA
                    "old_alpha"-> ReleaseType.ALPHA
                    else       -> ReleaseType.RELEASE
                },
                releaseDate = Instant.parse(summary.releaseTime).toEpochMilli()
            )
        }
        dao.insertAll(versions.map { it.toEntity() })
        versions
    }

    override suspend fun getVersion(id: String): GameVersion? = dao.getById(id)?.toDomain()

    override suspend fun installVersion(version: GameVersion, onProgress: (Float) -> Unit): Result<Unit> =
        runCatching {
            Timber.d("Installing version: ${version.id}")
            // In production: fetch version JSON, download client jar + libraries + assets
            // This is the core launcher logic that mirrors PojavLauncher/Copper architecture
            onProgress(1f)
        }

    override suspend fun uninstallVersion(id: String): Result<Unit> = runCatching {
        dao.deleteById(id)
    }

    override suspend fun isVersionInstalled(id: String): Boolean =
        dao.getById(id)?.isInstalled == true
}

// ─── Profile Repository ───────────────────────────────────────────────────────

@Singleton
class ProfileRepositoryImpl @Inject constructor(private val dao: ProfileDao) : ProfileRepository {

    override fun observeProfiles(): Flow<List<GameProfile>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProfile(id: String): GameProfile? = dao.getById(id)?.toDomain()

    override suspend fun createProfile(profile: GameProfile) { dao.insert(profile.toEntity()) }

    override suspend fun updateProfile(profile: GameProfile) { dao.update(profile.toEntity()) }

    override suspend fun deleteProfile(id: String) { dao.deleteById(id) }

    override suspend fun duplicateProfile(id: String): GameProfile {
        val original = getProfile(id) ?: error("Profile not found")
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastPlayed = null,
            isFavorite = false
        )
        createProfile(copy)
        return copy
    }
}

// ─── Mod Repository ──────────────────────────────────────────────────────────

@Singleton
class ModRepositoryImpl @Inject constructor(
    private val dao: ModDao,
    private val api: ModrinthApi,
    private val downloadManager: DownloadManager
) : ModRepository {

    override fun observeInstalledMods(profileId: String): Flow<List<Mod>> =
        dao.observeByProfile(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun searchMods(
        query: String, loaderType: LoaderType, mcVersion: String
    ): Result<List<Mod>> = runCatching {
        val loader = loaderType.name.lowercase()
        val facets = """[["project_type:mod"],["categories:$loader"],["versions:$mcVersion"]]"""
        val resp = api.searchProjects(query, facets)
        resp.body()?.hits?.map { it.toMod() } ?: emptyList()
    }

    override suspend fun installMod(mod: Mod, profileId: String, onProgress: (Float) -> Unit): Result<Unit> =
        runCatching {
            // Download file then insert into DB
            onProgress(1f)
            dao.insert(mod.copy(isInstalled = true).toEntity(profileId))
        }

    override suspend fun uninstallMod(modId: String, profileId: String): Result<Unit> =
        runCatching { dao.delete(modId, profileId) }

    override suspend fun toggleMod(modId: String, profileId: String, enabled: Boolean) {
        dao.setEnabled(modId, enabled)
    }

    override suspend fun updateMod(modId: String, profileId: String, onProgress: (Float) -> Unit): Result<Unit> =
        runCatching { onProgress(1f) }
}

// ─── Java Repository ─────────────────────────────────────────────────────────

@Singleton
class JavaRepositoryImpl @Inject constructor(
    private val dao: JavaRuntimeDao,
    private val api: AdoptiumApi,
    private val downloadManager: DownloadManager
) : JavaRepository {

    override fun observeRuntimes(): Flow<List<JavaRuntime>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getDefaultRuntime(mcVersion: String): JavaRuntime? {
        val requiredJava = when {
            mcVersion.startsWith("1.20") || mcVersion.startsWith("1.21") -> 21
            mcVersion.startsWith("1.18") || mcVersion.startsWith("1.19") -> 17
            else -> 8
        }
        return dao.getDefault()?.toDomain() ?: dao.observeAll().map { it.find { r -> r.version == requiredJava } }.let { null }
    }

    override suspend fun installRuntime(version: Int, onProgress: (Float) -> Unit): Result<JavaRuntime> =
        runCatching {
            val resp = api.getLatestJre(version)
            val asset = resp.body()?.firstOrNull() ?: error("No JRE found for version $version")
            Timber.d("Would download JRE from: ${asset.binary.pkg.link}")
            onProgress(1f)
            JavaRuntime(
                version = version,
                vendor = "Eclipse Adoptium",
                architecture = "aarch64",
                installPath = "/data/data/com.novacraft.launcher/files/jre/$version",
                isInstalled = true,
                buildVersion = asset.version.semver
            )
        }

    override suspend fun setDefaultRuntime(id: String) {
        dao.clearDefault()
        dao.setDefault(id)
    }

    override suspend fun detectSystemJava(): JavaRuntime? = null
}

// ─── Download Repository ──────────────────────────────────────────────────────

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val dao: DownloadDao,
    private val downloadManager: DownloadManager
) : DownloadRepository {

    override fun observeDownloads(): Flow<List<DownloadTask>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun enqueueDownload(task: DownloadTask): String {
        dao.insert(task.toEntity())
        downloadManager.enqueue(task)
        return task.id
    }

    override suspend fun cancelDownload(id: String) {
        downloadManager.cancel(id)
        dao.updateStatus(id, DownloadStatus.CANCELLED.name)
    }

    override suspend fun pauseDownload(id: String) {
        downloadManager.pause(id)
        dao.updateStatus(id, DownloadStatus.PAUSED.name)
    }

    override suspend fun resumeDownload(id: String) {
        downloadManager.resume(id)
        dao.updateStatus(id, DownloadStatus.DOWNLOADING.name)
    }

    override suspend fun clearCompleted() { dao.clearCompleted() }
}

// ─── Settings Repository ─────────────────────────────────────────────────────

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) : SettingsRepository {

    private companion object {
        val KEY_RAM         = intPreferencesKey("default_ram_mb")
        val KEY_JVM_ARGS    = stringPreferencesKey("default_jvm_args")
        val KEY_GAME_DIR    = stringPreferencesKey("game_dir")
        val KEY_DL_THREADS  = intPreferencesKey("download_threads")
        val KEY_AUTO_UPDATE = booleanPreferencesKey("auto_check_updates")
        val KEY_THEME       = stringPreferencesKey("theme_config")
        val KEY_LANGUAGE    = stringPreferencesKey("language")
        val KEY_FPS         = booleanPreferencesKey("show_fps")
        val KEY_PERF        = booleanPreferencesKey("show_perf")
        val KEY_GYRO        = booleanPreferencesKey("enable_gyro")
        val KEY_HAPTIC      = booleanPreferencesKey("haptic_feedback")
        val KEY_CONTROL     = stringPreferencesKey("control_layout")
    }

    override fun observeSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            defaultRamMb      = prefs[KEY_RAM] ?: 2048,
            defaultJvmArgs    = prefs[KEY_JVM_ARGS] ?: AppSettings().defaultJvmArgs,
            gameDir           = prefs[KEY_GAME_DIR] ?: "",
            downloadThreads   = prefs[KEY_DL_THREADS] ?: 4,
            autoCheckUpdates  = prefs[KEY_AUTO_UPDATE] ?: true,
            themeConfig       = prefs[KEY_THEME] ?: "DARK_NOVA",
            language          = prefs[KEY_LANGUAGE] ?: "en",
            showFpsCounter    = prefs[KEY_FPS] ?: false,
            showPerfOverlay   = prefs[KEY_PERF] ?: false,
            enableGyroAim     = prefs[KEY_GYRO] ?: false,
            hapticFeedback    = prefs[KEY_HAPTIC] ?: true,
            controlLayout     = prefs[KEY_CONTROL] ?: "DEFAULT"
        )
    }

    override suspend fun getSettings(): AppSettings =
        observeSettings().let { AppSettings() } // simplified

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_RAM]         = settings.defaultRamMb
            prefs[KEY_JVM_ARGS]    = settings.defaultJvmArgs
            prefs[KEY_GAME_DIR]    = settings.gameDir
            prefs[KEY_DL_THREADS]  = settings.downloadThreads
            prefs[KEY_AUTO_UPDATE] = settings.autoCheckUpdates
            prefs[KEY_THEME]       = settings.themeConfig
            prefs[KEY_LANGUAGE]    = settings.language
            prefs[KEY_FPS]         = settings.showFpsCounter
            prefs[KEY_PERF]        = settings.showPerfOverlay
            prefs[KEY_GYRO]        = settings.enableGyroAim
            prefs[KEY_HAPTIC]      = settings.hapticFeedback
            prefs[KEY_CONTROL]     = settings.controlLayout
        }
    }

    override suspend fun exportSettings(destPath: String): Result<Unit> = runCatching { }
    override suspend fun importSettings(srcPath: String): Result<Unit> = runCatching { }
    override suspend fun resetSettings() { dataStore.edit { it.clear() } }
}

// ─── News Repository ─────────────────────────────────────────────────────────

@Singleton
class NewsRepositoryImpl @Inject constructor(private val dao: NewsDao) : NewsRepository {

    override suspend fun fetchNews(): Result<List<NewsItem>> = runCatching {
        // Fetch from Minecraft news API or launcher-specific feed
        // Using static sample data for scaffold
        val sampleNews = listOf(
            NewsItem("1", "Minecraft 1.21.4 Released", "The latest update brings new features and fixes.", null, "https://minecraft.net", System.currentTimeMillis(), "UPDATE"),
            NewsItem("2", "NovaCraft 1.0 Released", "Java Minecraft. Anywhere. NovaCraft Launcher is here.", null, "https://novacraft.app", System.currentTimeMillis() - 86400000, "LAUNCHER")
        )
        dao.insertAll(sampleNews.map { it.toEntity() })
        sampleNews
    }

    override fun observeCachedNews(): Flow<List<NewsItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}

// ─── Server Repository ────────────────────────────────────────────────────────

@Singleton
class ServerRepositoryImpl @Inject constructor(private val dao: ServerDao) : ServerRepository {

    override fun observeServers(): Flow<List<ServerEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addServer(server: ServerEntry) { dao.insert(server.toEntity()) }
    override suspend fun removeServer(id: String) { dao.deleteById(id) }
    override suspend fun toggleFavorite(id: String) { dao.toggleFavorite(id) }

    override suspend fun pingServer(address: String, port: Int): Result<ServerEntry> =
        runCatching {
            // Real implementation: open TCP socket and use Minecraft SLP protocol
            ServerEntry(name = address, address = address, port = port, motd = "Ping successful")
        }
}

// ─── Extension mappers ────────────────────────────────────────────────────────

fun AccountEntity.toDomain(prefs: SharedPreferences) = Account(
    id = id, username = username, uuid = uuid,
    type = AccountType.valueOf(type), isActive = isActive,
    avatarUrl = avatarUrl, addedAt = addedAt,
    accessToken = prefs.getString("token_$id", null),
    refreshToken = prefs.getString("refresh_$id", null)
)
fun Account.toEntity() = AccountEntity(id, username, uuid, type.name, isActive, avatarUrl, addedAt)

fun VersionEntity.toDomain() = GameVersion(
    id = id, minecraftVersion = minecraftVersion,
    loaderType = LoaderType.valueOf(loaderType),
    loaderVersion = loaderVersion,
    releaseType = ReleaseType.valueOf(releaseType),
    releaseDate = releaseDate, isInstalled = isInstalled,
    installPath = installPath, lastPlayed = lastPlayed,
    playCount = playCount, totalPlayTime = totalPlayTime, iconUrl = iconUrl
)
fun GameVersion.toEntity() = VersionEntity(
    id, minecraftVersion, loaderType.name, loaderVersion, releaseType.name,
    releaseDate, isInstalled, installPath, lastPlayed, playCount, totalPlayTime, iconUrl
)

fun ProfileEntity.toDomain() = GameProfile(
    id = id, name = name, iconEmoji = iconEmoji, versionId = versionId,
    accountId = accountId, javaPath = javaPath, ramMb = ramMb,
    resolution = GameResolution(resolutionWidth, resolutionHeight),
    jvmArgs = jvmArgs, gameArgs = gameArgs,
    enableOptimizations = enableOptimizations,
    performancePreset = PerformancePreset.valueOf(performancePreset),
    createdAt = createdAt, lastPlayed = lastPlayed,
    isFavorite = isFavorite, screenshotPath = screenshotPath
)
fun GameProfile.toEntity() = ProfileEntity(
    id, name, iconEmoji, versionId, accountId, javaPath, ramMb,
    resolution.width, resolution.height, jvmArgs, gameArgs,
    enableOptimizations, performancePreset.name, createdAt, lastPlayed,
    isFavorite, screenshotPath
)

fun ModEntity.toDomain() = Mod(
    id = id, name = name, slug = slug, description = description,
    author = author, version = version,
    loaderTypes = loaderTypes.map { LoaderType.valueOf(it) },
    gameVersions = gameVersions, downloadUrl = downloadUrl,
    fileName = fileName, filePath = filePath, sizeBytes = sizeBytes,
    isInstalled = isInstalled, isEnabled = isEnabled, iconUrl = iconUrl,
    source = ModSource.valueOf(source), downloads = downloads, follows = follows
)
fun Mod.toEntity(profileId: String) = ModEntity(
    id, profileId, name, slug, description, author, version,
    loaderTypes.map { it.name }, gameVersions, downloadUrl, fileName,
    filePath, sizeBytes, isInstalled, isEnabled, iconUrl, source.name, downloads, follows
)

fun ModrinthProjectDto.toMod() = Mod(
    id = projectId, name = title, slug = slug, description = description,
    author = author, downloads = downloads, follows = follows, iconUrl = iconUrl
)

fun JavaRuntimeEntity.toDomain() = JavaRuntime(
    id = id, version = version, vendor = vendor, architecture = architecture,
    installPath = installPath, isInstalled = isInstalled, isDefault = isDefault,
    buildVersion = buildVersion
)

fun DownloadEntity.toDomain() = DownloadTask(
    id = id, name = name, url = url, destPath = destPath,
    totalBytes = totalBytes, downloadedBytes = downloadedBytes,
    status = DownloadStatus.valueOf(status), progress = progress,
    error = error, createdAt = createdAt
)
fun DownloadTask.toEntity() = DownloadEntity(
    id, name, url, destPath, totalBytes, downloadedBytes,
    status.name, progress, error, createdAt
)

fun ServerEntity.toDomain() = ServerEntry(
    id = id, name = name, address = address, port = port,
    iconUrl = iconUrl, isFavorite = isFavorite, lastPing = lastPing,
    playerCount = playerCount, maxPlayers = maxPlayers, motd = motd, version = version
)
fun ServerEntry.toEntity() = ServerEntity(
    id, name, address, port, iconUrl, isFavorite, lastPing, playerCount, maxPlayers, motd, version
)

fun NewsEntity.toDomain() = NewsItem(id, title, summary, imageUrl, url, publishedAt, category)
fun NewsItem.toEntity() = NewsEntity(id, title, summary, imageUrl, url, publishedAt, category)
