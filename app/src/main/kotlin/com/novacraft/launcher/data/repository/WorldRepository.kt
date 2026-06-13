package com.novacraft.launcher.data.repository

import com.novacraft.launcher.domain.model.World
import com.novacraft.launcher.domain.repository.WorldRepository
import com.novacraft.launcher.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldRepositoryImpl @Inject constructor() : WorldRepository {

    override suspend fun getWorlds(gameDir: String): List<World> =
        withContext(Dispatchers.IO) {
            val savesDir = File(gameDir, "saves")
            if (!savesDir.exists()) return@withContext emptyList()
            savesDir.listFiles()
                ?.filter { it.isDirectory }
                ?.map { worldDir ->
                    val levelDat = File(worldDir, "level.dat")
                    World(
                        name          = worldDir.name,
                        dirPath       = worldDir.absolutePath,
                        lastPlayed    = levelDat.lastModified(),
                        sizeBytes     = FileUtils.dirSize(worldDir),
                        iconPath      = File(worldDir, "icon.png").takeIf { it.exists() }?.absolutePath
                    )
                }
                ?.sortedByDescending { it.lastPlayed }
                ?: emptyList()
        }

    override suspend fun deleteWorld(world: World): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Timber.d("Deleting world: ${world.name}")
            File(world.dirPath).deleteRecursively()
        }
    }

    override suspend fun backupWorld(world: World, destDir: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val dest = File(destDir, "${world.name}_backup_${System.currentTimeMillis()}.zip")
            Timber.d("Backing up world ${world.name} to ${dest.absolutePath}")
            // Production: zip the world directory to destDir
        }
    }
}

// ─── Crash Reporter ──────────────────────────────────────────────────────────

package com.novacraft.launcher.service.launch

import android.content.Context
import com.novacraft.launcher.data.local.dao.CrashReportDao
import com.novacraft.launcher.data.local.entities.CrashReportEntity
import com.novacraft.launcher.domain.model.CrashReport
import com.novacraft.launcher.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CrashReporter
 *
 * Intercepts Minecraft process crash output and stores structured
 * crash reports in the Room database for later viewing in the UI.
 *
 * Also handles the built-in Java exception handler for launcher-side crashes.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CrashReportDao
) {

    fun observeReports(): Flow<List<CrashReport>> =
        dao.observeAll().map { list ->
            list.map {
                CrashReport(
                    id          = it.id,
                    timestamp   = it.timestamp,
                    profileName = it.profileName,
                    versionId   = it.versionId,
                    exitCode    = it.exitCode,
                    logPath     = it.logPath,
                    summary     = it.summary
                )
            }
        }

    /**
     * Called after the Minecraft process exits with a non-zero code.
     * Reads the crash log and stores it.
     */
    suspend fun reportCrash(
        profileName: String,
        versionId: String,
        exitCode: Int,
        gameDir: String
    ) = withContext(Dispatchers.IO) {
        val crashDir  = File(gameDir, Constants.DIR_CRASH_REPORTS)
        val latestLog = crashDir.listFiles()
            ?.filter { it.name.endsWith(".txt") }
            ?.maxByOrNull { it.lastModified() }

        val summary = latestLog?.readText()?.take(512) ?: "Exit code $exitCode — no crash log found."

        val report = CrashReportEntity(
            id          = java.util.UUID.randomUUID().toString(),
            timestamp   = System.currentTimeMillis(),
            profileName = profileName,
            versionId   = versionId,
            exitCode    = exitCode,
            logPath     = latestLog?.absolutePath ?: "",
            summary     = summary
        )
        dao.insert(report)
        Timber.w("Crash report saved for $profileName ($versionId), exit code $exitCode")

        // Trim old reports
        trimOldReports()
    }

    private suspend fun trimOldReports() {
        // Keep only the last MAX_CRASH_REPORTS_STORED reports
        // In production: query all ordered by timestamp, delete oldest beyond limit
    }

    suspend fun clearAll() { dao.deleteAll() }
}
