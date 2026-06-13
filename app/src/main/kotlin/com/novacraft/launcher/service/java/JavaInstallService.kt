package com.novacraft.launcher.service.java

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.novacraft.launcher.domain.repository.JavaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * JavaInstallService
 *
 * Foreground service that manages downloading and extracting the
 * Java Runtime Environment (JRE) for Minecraft. Uses Adoptium API
 * to fetch the latest AArch64 JRE for Android.
 *
 * Supported JRE versions:
 * - Java 8  (MC ≤ 1.17.1)
 * - Java 17 (MC 1.18 - 1.20.4)
 * - Java 21 (MC ≥ 1.20.5)
 */
@AndroidEntryPoint
class JavaInstallService : Service() {

    @Inject lateinit var javaRepository: JavaRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_INSTALL = "com.novacraft.launcher.INSTALL_JAVA"
        const val EXTRA_JAVA_VERSION = "java_version"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "java_install_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val javaVersion = intent?.getIntExtra(EXTRA_JAVA_VERSION, 17) ?: 17
        startForeground(NOTIFICATION_ID, buildNotification("Installing Java $javaVersion..."))
        serviceScope.launch {
            try {
                javaRepository.installRuntime(javaVersion) { progress ->
                    updateNotification("Installing Java $javaVersion... ${(progress * 100).toInt()}%")
                }
                Timber.d("Java $javaVersion installation complete")
            } catch (e: Exception) {
                Timber.e(e, "Java installation failed")
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Java Installation",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows Java runtime installation progress" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(message: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NovaCraft Launcher")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

    private fun updateNotification(message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(message))
    }
}

// ─── JavaRuntimeManager ───────────────────────────────────────────────────────

package com.novacraft.launcher.service.java

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JavaRuntimeManager
 *
 * Manages JRE installations on-device. Handles path resolution,
 * extraction from downloaded archives, and version selection for launch.
 */
@Singleton
class JavaRuntimeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val jreBaseDir = File(context.filesDir, "jre")

    /** Returns the path to java executable for the given major version */
    fun getJavaExecutable(majorVersion: Int): String? {
        val jreDir = File(jreBaseDir, majorVersion.toString())
        val javaExe = File(jreDir, "bin/java")
        return if (javaExe.exists() && javaExe.canExecute()) javaExe.absolutePath else null
    }

    /** Returns whether a specific JRE version is installed */
    fun isInstalled(majorVersion: Int): Boolean {
        return getJavaExecutable(majorVersion) != null
    }

    /** Returns the recommended Java version for a given MC version string */
    fun requiredJavaVersion(mcVersion: String): Int = when {
        compareVersions(mcVersion, "1.20.5") >= 0 -> 21
        compareVersions(mcVersion, "1.18.0") >= 0 -> 17
        else -> 8
    }

    /** All installed JRE directories */
    fun listInstalledVersions(): List<Int> {
        return jreBaseDir.listFiles()?.mapNotNull { it.name.toIntOrNull() } ?: emptyList()
    }

    /** Extract a downloaded JRE tar.gz into the jre directory */
    suspend fun extractJre(archivePath: String, targetVersion: Int) {
        Timber.d("Extracting JRE $targetVersion from $archivePath")
        val destDir = File(jreBaseDir, targetVersion.toString())
        destDir.mkdirs()
        // In production: use Apache Commons Compress or custom tar extraction
        // Runtime.getRuntime().exec("tar -xzf $archivePath -C ${destDir.absolutePath} --strip-components=1")
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0..2) {
            val diff = (parts1.getOrNull(i) ?: 0).compareTo(parts2.getOrNull(i) ?: 0)
            if (diff != 0) return diff
        }
        return 0
    }
}
