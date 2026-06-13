package com.novacraft.launcher.util

/**
 * Application-wide constants.
 */
object Constants {

    // ── Microsoft OAuth ───────────────────────────────────────────────────────
    // Register your app at https://portal.azure.com to get a real client ID.
    const val MS_CLIENT_ID   = "YOUR_MICROSOFT_CLIENT_ID"
    const val MS_REDIRECT_URI = "https://login.microsoftonline.com/common/oauth2/nativeclient"

    // ── Launcher metadata ────────────────────────────────────────────────────
    const val LAUNCHER_NAME    = "NovaCraft Launcher"
    const val LAUNCHER_VERSION = "1.0.0"
    const val LAUNCHER_BRAND   = "NovaCraftLauncher"

    // ── Mojang endpoints ────────────────────────────────────────────────────
    const val MOJANG_VERSION_MANIFEST   = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
    const val MOJANG_LIBRARIES_BASE     = "https://libraries.minecraft.net/"
    const val MOJANG_ASSETS_BASE        = "https://resources.download.minecraft.net/"
    const val MOJANG_AUTH_BASE          = "https://authserver.mojang.com"

    // ── Adoptium JRE ────────────────────────────────────────────────────────
    const val ADOPTIUM_API_BASE         = "https://api.adoptium.net/"
    const val JRE_ARCH                  = "aarch64"   // Android ARM64
    const val JRE_OS                    = "linux"

    // ── Modrinth ────────────────────────────────────────────────────────────
    const val MODRINTH_API_BASE         = "https://api.modrinth.com/v2/"

    // ── Game directory names ─────────────────────────────────────────────────
    const val DIR_VERSIONS      = "versions"
    const val DIR_LIBRARIES     = "libraries"
    const val DIR_ASSETS        = "assets"
    const val DIR_NATIVES       = "natives"
    const val DIR_MODS          = "mods"
    const val DIR_RESOURCEPACKS = "resourcepacks"
    const val DIR_SHADERPACKS   = "shaderpacks"
    const val DIR_SAVES         = "saves"
    const val DIR_SCREENSHOTS   = "screenshots"
    const val DIR_LOGS          = "logs"
    const val DIR_CRASH_REPORTS = "crash-reports"
    const val DIR_JRE           = "jre"

    // ── Java version requirements ───────────────────────────────────────────
    const val JAVA_8_MC_MAX  = "1.17.1"
    const val JAVA_17_MC_MIN = "1.18.0"
    const val JAVA_17_MC_MAX = "1.20.4"
    const val JAVA_21_MC_MIN = "1.20.5"

    // ── Notification channel IDs ────────────────────────────────────────────
    const val NOTIF_CHANNEL_DOWNLOADS = "novacraft_downloads"
    const val NOTIF_CHANNEL_JAVA      = "novacraft_java_install"
    const val NOTIF_CHANNEL_GAME      = "novacraft_game"
    const val NOTIF_CHANNEL_UPDATES   = "novacraft_updates"

    // ── WorkManager tags ────────────────────────────────────────────────────
    const val WORK_TAG_DOWNLOAD = "download_work"
    const val WORK_TAG_JAVA     = "java_install_work"
    const val WORK_TAG_UPDATE   = "update_check_work"

    // ── DataStore keys ───────────────────────────────────────────────────────
    const val DATASTORE_SETTINGS = "novacraft_settings"

    // ── Encrypted prefs ──────────────────────────────────────────────────────
    const val ENCRYPTED_PREFS_NAME = "novacraft_secure_prefs"

    // ── Defaults ────────────────────────────────────────────────────────────
    const val DEFAULT_RAM_MB         = 2048
    const val DEFAULT_DOWNLOAD_THREADS = 4
    const val DEFAULT_CONTROL_LAYOUT = "DEFAULT"

    // ── JVM optimisation flags ──────────────────────────────────────────────
    val JVM_ARGS_G1GC = listOf(
        "-XX:+UseG1GC",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:G1NewSizePercent=20",
        "-XX:G1ReservePercent=20",
        "-XX:MaxGCPauseMillis=50",
        "-XX:G1HeapRegionSize=32M"
    )

    val JVM_ARGS_ZGC = listOf(
        "-XX:+UseZGC",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+DisableExplicitGC",
        "-XX:+AlwaysPreTouch"
    )

    // ── Misc ────────────────────────────────────────────────────────────────
    const val MAX_CRASH_REPORTS_STORED = 20
    const val NEWS_CACHE_TTL_MS        = 1_800_000L  // 30 min
    const val TOKEN_REFRESH_MARGIN_MS  = 300_000L    // 5 min before expiry
}
