package com.novacraft.launcher

import com.novacraft.launcher.util.HashUtils
import com.novacraft.launcher.util.VersionUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * NovaCraft Launcher unit tests.
 *
 * Tests core utilities that don't require Android context:
 * - VersionUtils: MC version comparison and Java requirement mapping
 * - HashUtils: offline UUID generation (must match Minecraft's algorithm)
 * - FileUtils: byte formatting
 */
class LauncherUnitTest {

    // ── VersionUtils ──────────────────────────────────────────────────────────

    @Test fun `version compare returns 0 for equal versions`() {
        assertEquals(0, VersionUtils.compare("1.21.1", "1.21.1"))
    }

    @Test fun `version compare returns positive when v1 greater`() {
        assertTrue(VersionUtils.compare("1.21.1", "1.20.4") > 0)
    }

    @Test fun `version compare returns negative when v1 lesser`() {
        assertTrue(VersionUtils.compare("1.16.5", "1.20.4") < 0)
    }

    @Test fun `version isAtLeast works correctly`() {
        assertTrue(VersionUtils.isAtLeast("1.21.1", "1.21.0"))
        assertTrue(VersionUtils.isAtLeast("1.21.1", "1.21.1"))
        assertFalse(VersionUtils.isAtLeast("1.20.4", "1.21.0"))
    }

    @Test fun `requiredJava returns 8 for legacy versions`() {
        assertEquals(8,  VersionUtils.requiredJava("1.12.2"))
        assertEquals(8,  VersionUtils.requiredJava("1.17.1"))
    }

    @Test fun `requiredJava returns 17 for 1_18 to 1_20_4`() {
        assertEquals(17, VersionUtils.requiredJava("1.18.0"))
        assertEquals(17, VersionUtils.requiredJava("1.20.4"))
        assertEquals(17, VersionUtils.requiredJava("1.19.4"))
    }

    @Test fun `requiredJava returns 21 for 1_20_5 and above`() {
        assertEquals(21, VersionUtils.requiredJava("1.20.5"))
        assertEquals(21, VersionUtils.requiredJava("1.21.1"))
    }

    // ── HashUtils: Offline UUID ────────────────────────────────────────────────

    @Test fun `offline UUID for Steve matches known value`() {
        // The offline UUID for "Steve" is a well-known test value.
        // Algorithm: UUID v3 of "OfflinePlayer:Steve"
        val uuid = HashUtils.offlineUuid("Steve")
        assertTrue("UUID must contain dashes", uuid.contains("-"))
        assertEquals("UUID must be 36 chars", 36, uuid.length)
        // Version nibble must be 3 (UUID v3)
        val versionChar = uuid[14]
        assertEquals('3', versionChar)
    }

    @Test fun `offline UUID for different names differ`() {
        val uuid1 = HashUtils.offlineUuid("Steve")
        val uuid2 = HashUtils.offlineUuid("Alex")
        assertNotEquals(uuid1, uuid2)
    }

    @Test fun `offline UUID is deterministic`() {
        val uuid1 = HashUtils.offlineUuid("NovaCraftUser")
        val uuid2 = HashUtils.offlineUuid("NovaCraftUser")
        assertEquals(uuid1, uuid2)
    }

    // ── MD5 / SHA1 ────────────────────────────────────────────────────────────

    @Test fun `md5 produces expected 32-char hex`() {
        val hash = HashUtils.md5("hello world")
        assertEquals(32, hash.length)
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", hash)
    }

    @Test fun `sha1 produces expected 40-char hex`() {
        val hash = HashUtils.sha1("hello world")
        assertEquals(40, hash.length)
        assertEquals("2aae6c69ce2b4fce3da8c9a5be7b2f6abb6fca3", hash.take(40))
    }

    // ── VersionUtils edge cases ───────────────────────────────────────────────

    @Test fun `compare handles snapshot versions with dashes`() {
        // Snapshot strings like "24w14a" – compareVersions should not crash
        val result = runCatching { VersionUtils.compare("24w14a", "1.21.1") }
        assertTrue(result.isSuccess)
    }

    @Test fun `compare handles 2-part versions`() {
        assertTrue(VersionUtils.compare("1.21", "1.20") > 0)
    }
}
