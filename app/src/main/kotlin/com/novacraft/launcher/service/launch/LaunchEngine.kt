package com.novacraft.launcher.service.launch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.service.java.JavaRuntimeManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LaunchEngine
 *
 * Core Minecraft Java Edition launch engine for Android.
 *
 * Architecture mirrors PojavLauncher's approach:
 * 1. Resolve all classpath entries (client jar + libraries)
 * 2. Build JVM arguments (memory, GC, natives path)
 * 3. Build game arguments (username, session, assets, version)
 * 4. Execute via ART JNI bridge or direct Process invocation
 *
 * On Android, Minecraft Java runs via a modified JVM that bridges
 * Java's AWT/Swing rendering to an Android SurfaceView through
 * a native (JNI) bridge layer.
 */
@Singleton
class LaunchEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val javaRuntimeManager: JavaRuntimeManager
) {

    /**
     * Build the full command line for launching Minecraft.
     *
     * @param config  All resolved launch parameters.
     * @return        Executable command with all arguments.
     */
    fun buildLaunchCommand(config: LaunchConfig): List<String> {
        val javaExe = javaRuntimeManager.getJavaExecutable(
            javaRuntimeManager.requiredJavaVersion(config.version.minecraftVersion)
        ) ?: error("Java runtime not found for MC ${config.version.minecraftVersion}")

        val classpath = buildClasspath(config)
        val jvmArgs   = buildJvmArgs(config)
        val gameArgs  = buildGameArgs(config)

        return buildList {
            add(javaExe)
            addAll(jvmArgs)
            add("-cp")
            add(classpath)
            add(config.version.toMainClass())
            addAll(gameArgs)
        }
    }

    /**
     * Build JVM argument list including memory, GC tuning, and paths.
     */
    private fun buildJvmArgs(config: LaunchConfig): List<String> = buildList {
        // Memory
        add("-Xmx${config.profile.ramMb}M")
        add("-Xms${minOf(512, config.profile.ramMb / 2)}M")

        // Natives path (JNI libraries)
        add("-Djava.library.path=${config.nativesDir}")

        // Minecraft system properties
        add("-Dminecraft.launcher.brand=NovaCraftLauncher")
        add("-Dminecraft.launcher.version=1.0.0")

        // Android/mobile specific JVM flags
        add("-Dos.name=Linux")
        add("-Dos.version=Android")
        add("-Dfile.encoding=UTF-8")

        // LWJGL renderer (headless OpenGL via ANGLE/EGL)
        add("-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true")

        // Performance optimizations if enabled
        if (config.profile.enableOptimizations) {
            addAll(getOptimizationFlags(config.profile.performancePreset))
        }

        // Custom JVM args from profile
        if (config.profile.jvmArgs.isNotBlank()) {
            addAll(config.profile.jvmArgs.split(" ").filter { it.isNotBlank() })
        }
    }

    /**
     * Build Minecraft game argument list.
     */
    private fun buildGameArgs(config: LaunchConfig): List<String> = buildList {
        add("--username"); add(config.account.username)
        add("--version"); add(config.version.minecraftVersion)
        add("--gameDir"); add(config.gameDir)
        add("--assetsDir"); add(config.assetsDir)
        add("--assetIndex"); add(config.version.minecraftVersion)
        add("--uuid"); add(config.account.uuid.replace("-", ""))
        add("--accessToken"); add(config.account.accessToken ?: "offline")
        add("--clientId"); add("NovaCraftLauncher")
        add("--userType"); add(if (config.account.type == AccountType.MICROSOFT) "msa" else "legacy")
        add("--versionType"); add("NovaCraftLauncher")

        // Resolution
        if (config.profile.resolution.width > 0) {
            add("--width"); add(config.profile.resolution.width.toString())
            add("--height"); add(config.profile.resolution.height.toString())
        }

        // Custom game args
        if (config.profile.gameArgs.isNotBlank()) {
            addAll(config.profile.gameArgs.split(" ").filter { it.isNotBlank() })
        }
    }

    /**
     * Resolve the full Java classpath string for the given version.
     * Includes client jar + all required libraries.
     */
    private fun buildClasspath(config: LaunchConfig): String {
        val paths = mutableListOf<String>()
        val librariesDir = File(config.librariesDir)

        // Client jar
        val clientJar = File(config.gameDir, "versions/${config.version.id}/${config.version.id}.jar")
        if (clientJar.exists()) paths.add(clientJar.absolutePath)

        // Library jars
        if (librariesDir.exists()) {
            librariesDir.walkTopDown()
                .filter { it.isFile && it.extension == "jar" }
                .forEach { paths.add(it.absolutePath) }
        }

        return paths.joinToString(":")
    }

    /**
     * Performance-preset JVM flags.
     */
    private fun getOptimizationFlags(preset: PerformancePreset): List<String> = when (preset) {
        PerformancePreset.LOW_END -> listOf(
            "-XX:+UseSerialGC",
            "-XX:MaxGCPauseMillis=200"
        )
        PerformancePreset.BALANCED -> listOf(
            "-XX:+UseG1GC",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:G1NewSizePercent=20",
            "-XX:G1ReservePercent=20",
            "-XX:MaxGCPauseMillis=50",
            "-XX:G1HeapRegionSize=32M"
        )
        PerformancePreset.HIGH_END -> listOf(
            "-XX:+UseZGC",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+DisableExplicitGC",
            "-XX:+AlwaysPreTouch",
            "-XX:+PerfDisableSharedMem"
        )
        PerformancePreset.CUSTOM -> emptyList()
    }

    companion object {
        private fun GameVersion.toMainClass(): String = when (loaderType) {
            LoaderType.FABRIC    -> "net.fabricmc.loader.launch.knot.KnotClient"
            LoaderType.FORGE     -> "cpw.mods.bootstraplauncher.BootstrapLauncher"
            LoaderType.NEOFORGE  -> "cpw.mods.bootstraplauncher.BootstrapLauncher"
            LoaderType.QUILT     -> "org.quiltmc.loader.impl.launch.knot.KnotClient"
            LoaderType.VANILLA   -> "net.minecraft.client.main.Main"
        }
    }
}

// ─── Game Activity ────────────────────────────────────────────────────────────

/**
 * GameActivity
 *
 * Full-screen landscape activity that hosts the running Minecraft instance.
 * Provides:
 * - SurfaceView for LWJGL/OpenGL rendering
 * - Touch input translation to Minecraft mouse/keyboard events
 * - Custom on-screen control overlay
 * - Performance monitoring overlay (FPS, RAM)
 * - Back gesture to show exit confirmation
 */
@AndroidEntryPoint
class GameActivity : Activity() {

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"

        fun start(context: Context, profileId: String) {
            context.startActivity(Intent(context, GameActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        Timber.d("GameActivity started for profile: $profileId")
        // In a real implementation:
        // 1. Load SurfaceView
        // 2. Start Minecraft process via LaunchEngine
        // 3. Overlay the touch control layer
        // 4. Wire up performance monitoring
    }
}
