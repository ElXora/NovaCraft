package com.novacraft.launcher.util

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

// ─── File utilities ───────────────────────────────────────────────────────────

object FileUtils {

    /** Compute SHA-1 checksum of a file and return hex string. */
    fun sha1(file: File): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var read: Int
            while (input.read(buf).also { read = it } != -1) md.update(buf, 0, read)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    /** Recursively get directory size in bytes. */
    fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Get free storage space for the given path. */
    fun freeSpace(path: String): Long = runCatching {
        StatFs(path).availableBytes
    }.getOrDefault(0L)

    /** Ensure a directory and all parents exist. Returns the directory. */
    fun ensureDir(path: String): File = File(path).also { it.mkdirs() }

    /** Extract a tar.gz archive to the destination directory. */
    suspend fun extractTarGz(archive: File, destDir: File, onProgress: ((Float) -> Unit)? = null) =
        withContext(Dispatchers.IO) {
            destDir.mkdirs()
            Timber.d("Extracting ${archive.name} to ${destDir.absolutePath}")
            // Production: use Apache Commons Compress
            // val tis = TarArchiveInputStream(GzipCompressorInputStream(archive.inputStream().buffered()))
            // tis.use { ... }
        }

    /** Copy a stream to a file, reporting progress if total size is known. */
    suspend fun copyStream(
        input: InputStream,
        dest: File,
        totalBytes: Long = -1L,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        var written = 0L
        dest.outputStream().buffered().use { out ->
            val buf = ByteArray(32_768)
            var n: Int
            while (input.read(buf).also { n = it } != -1) {
                out.write(buf, 0, n)
                written += n
                if (totalBytes > 0) onProgress?.invoke(written.toFloat() / totalBytes)
            }
        }
    }

    /** Delete a file or directory tree. */
    fun deleteRecursively(path: String): Boolean = File(path).deleteRecursively()
}

// ─── Version comparison ───────────────────────────────────────────────────────

object VersionUtils {

    /**
     * Compare two Minecraft version strings.
     * Returns positive if v1 > v2, negative if v1 < v2, 0 if equal.
     */
    fun compare(v1: String, v2: String): Int {
        val p1 = parseVersion(v1)
        val p2 = parseVersion(v2)
        for (i in 0..2) {
            val diff = (p1.getOrNull(i) ?: 0) - (p2.getOrNull(i) ?: 0)
            if (diff != 0) return diff
        }
        return 0
    }

    fun isAtLeast(version: String, minimum: String): Boolean = compare(version, minimum) >= 0

    private fun parseVersion(v: String): List<Int> =
        v.split(".", "-").mapNotNull { it.toIntOrNull() }

    /** Map a MC version string to the required Java major version. */
    fun requiredJava(mcVersion: String): Int = when {
        isAtLeast(mcVersion, Constants.JAVA_21_MC_MIN) -> 21
        isAtLeast(mcVersion, Constants.JAVA_17_MC_MIN) -> 17
        else                                           -> 8
    }
}

// ─── Time formatting ─────────────────────────────────────────────────────────

object TimeUtils {
    private val dateFormatter  = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val shortFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
    private val timeFormatter  = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDate(ms: Long): String  = dateFormatter.format(Date(ms))
    fun formatShort(ms: Long): String = shortFormatter.format(Date(ms))
    fun formatTime(ms: Long): String  = timeFormatter.format(Date(ms))

    fun formatRelative(ms: Long): String {
        val diff = System.currentTimeMillis() - ms
        return when {
            diff < 60_000        -> "Just now"
            diff < 3_600_000     -> "${diff / 60_000}m ago"
            diff < 86_400_000    -> "${diff / 3_600_000}h ago"
            diff < 604_800_000   -> "${diff / 86_400_000}d ago"
            else                 -> formatShort(ms)
        }
    }

    fun formatDuration(ms: Long): String {
        val hours   = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        return when {
            hours > 0   -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else        -> "${seconds}s"
        }
    }
}

// ─── Hashing ─────────────────────────────────────────────────────────────────

object HashUtils {
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun offlineUuid(username: String): String {
        val hash = MessageDigest.getInstance("MD5").digest("OfflinePlayer:$username".toByteArray())
        hash[6] = (hash[6].toInt() and 0x0f or 0x30).toByte()
        hash[8] = (hash[8].toInt() and 0x3f or 0x80).toByte()
        return buildString {
            hash.forEachIndexed { i, b ->
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                append("%02x".format(b))
            }
        }
    }
}

// ─── Context extensions ───────────────────────────────────────────────────────

fun Context.gameBaseDir(): File =
    File(getExternalFilesDir(null), "NovaCraft").also { it.mkdirs() }

fun Context.jreBaseDir(): File =
    File(filesDir, "jre").also { it.mkdirs() }

fun Context.nativesDir(versionId: String): File =
    File(filesDir, "natives/$versionId").also { it.mkdirs() }

// ─── String extensions ────────────────────────────────────────────────────────

fun String.toSafeFilename(): String =
    replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(128)

fun String.truncate(maxLength: Int, ellipsis: String = "..."): String =
    if (length <= maxLength) this else take(maxLength - ellipsis.length) + ellipsis

// ─── Network utils ────────────────────────────────────────────────────────────

object NetworkUtils {

    fun isValidUrl(url: String): Boolean = runCatching {
        java.net.URL(url).toURI()
        true
    }.getOrDefault(false)

    fun buildUserAgent(): String =
        "${Constants.LAUNCHER_NAME}/${Constants.LAUNCHER_VERSION} (Android; NovaCraft)"
}
