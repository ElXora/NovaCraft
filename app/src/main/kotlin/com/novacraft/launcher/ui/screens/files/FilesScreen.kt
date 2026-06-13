package com.novacraft.launcher.ui.screens.files

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilesScreen(navController: NavController) {
    // Start at the NovaCraft game directory
    var currentPath by remember { mutableStateOf("/storage/emulated/0/NovaCraft") }
    var entries     by remember(currentPath) { mutableStateOf(listFiles(currentPath)) }
    var selectedFile by remember { mutableStateOf<FileEntry?>(null) }

    val breadcrumbs = currentPath.split("/").filter { it.isNotBlank() }

    Scaffold(
        bottomBar = { NovaCraftBottomBar(navController) },
        topBar    = {
            NovaCraftTopBar(
                title    = "Files",
                subtitle = formatPath(currentPath),
                actions  = {
                    IconButton(onClick = { entries = listFiles(currentPath) }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = NovaCyan)
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1A2E))))
                .padding(padding)
        ) {
            Column {
                // ── Breadcrumb bar ───────────────────────────────────────────
                BreadcrumbBar(
                    parts  = breadcrumbs,
                    onNavigate = { index ->
                        currentPath = "/" + breadcrumbs.take(index + 1).joinToString("/")
                        entries = listFiles(currentPath)
                    },
                    onHome = {
                        currentPath = "/storage/emulated/0/NovaCraft"
                        entries = listFiles(currentPath)
                    }
                )

                // ── Quick shortcuts ──────────────────────────────────────────
                QuickAccessRow(onNavigate = { path ->
                    currentPath = path
                    entries     = listFiles(path)
                })

                // ── File list ────────────────────────────────────────────────
                if (entries.isEmpty()) {
                    EmptyState(
                        icon     = Icons.Outlined.FolderOpen,
                        title    = "Empty folder",
                        subtitle = "Nothing here yet",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back entry
                        if (currentPath != "/storage/emulated/0/NovaCraft") {
                            item {
                                FileEntryRow(
                                    entry = FileEntry("../", "", true, 0L, 0L, ".."),
                                    onClick = {
                                        currentPath = currentPath.substringBeforeLast("/").ifEmpty { "/" }
                                        entries = listFiles(currentPath)
                                    }
                                )
                            }
                        }

                        items(entries, key = { it.path }) { entry ->
                            FileEntryRow(
                                entry   = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        currentPath = entry.path
                                        entries = listFiles(currentPath)
                                    } else {
                                        selectedFile = entry
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── File action bottom sheet ───────────────────────────────────────────────
    selectedFile?.let { file ->
        FileActionSheet(
            file      = file,
            onDismiss = { selectedFile = null },
            onDelete  = {
                // File.delete() in production
                selectedFile = null
                entries = listFiles(currentPath)
            }
        )
    }
}

// ─── Data ─────────────────────────────────────────────────────────────────────

data class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val displayName: String = name
)

fun listFiles(path: String): List<FileEntry> {
    // In production: call File(path).listFiles() with proper permission check
    // Returning mock data for design scaffold
    return if (path.contains("NovaCraft")) {
        listOf(
            FileEntry("$path/versions",       "versions",        true,  0L, System.currentTimeMillis(), "versions"),
            FileEntry("$path/mods",            "mods",            true,  0L, System.currentTimeMillis(), "mods"),
            FileEntry("$path/resourcepacks",   "resourcepacks",   true,  0L, System.currentTimeMillis(), "resourcepacks"),
            FileEntry("$path/shaderpacks",     "shaderpacks",     true,  0L, System.currentTimeMillis(), "shaderpacks"),
            FileEntry("$path/saves",           "saves",           true,  0L, System.currentTimeMillis(), "saves"),
            FileEntry("$path/screenshots",     "screenshots",     true,  0L, System.currentTimeMillis(), "screenshots"),
            FileEntry("$path/logs",            "logs",            true,  0L, System.currentTimeMillis(), "logs"),
            FileEntry("$path/options.txt",     "options.txt",     false, 4096L,  System.currentTimeMillis(), "options.txt"),
        )
    } else emptyList()
}

fun formatPath(path: String): String {
    val parts = path.split("/").filter { it.isNotBlank() }
    return if (parts.size <= 3) path else ".../" + parts.takeLast(2).joinToString("/")
}

// ─── Breadcrumb Bar ──────────────────────────────────────────────────────────

@Composable
fun BreadcrumbBar(
    parts: List<String>,
    onNavigate: (Int) -> Unit,
    onHome: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHome, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Home, "Home", tint = NovaCyan, modifier = Modifier.size(16.dp))
        }
        parts.forEachIndexed { i, part ->
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
            Text(
                text = part,
                style = MaterialTheme.typography.labelSmall,
                color = if (i == parts.lastIndex) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                fontWeight = if (i == parts.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable { onNavigate(i) }
            )
        }
    }
}

// ─── Quick Access ─────────────────────────────────────────────────────────────

@Composable
fun QuickAccessRow(onNavigate: (String) -> Unit) {
    val base = "/storage/emulated/0/NovaCraft"
    val shortcuts = listOf(
        Triple(Icons.Filled.Archive, "Saves", "$base/saves"),
        Triple(Icons.Filled.Image,   "Screenshots", "$base/screenshots"),
        Triple(Icons.Filled.Extension, "Mods", "$base/mods"),
        Triple(Icons.Filled.BrokenImage, "Resource Packs", "$base/resourcepacks"),
        Triple(Icons.Filled.Tune, "Shaders", "$base/shaderpacks"),
        Triple(Icons.Filled.BugReport, "Logs", "$base/logs")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shortcuts) { (icon, label, path) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(0.5f))
                    .border(1.dp, NovaGlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onNavigate(path) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(icon, label, tint = NovaCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── File Entry Row ───────────────────────────────────────────────────────────

@Composable
fun FileEntryRow(entry: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        val (icon, tint) = when {
            entry.displayName == ".." -> Pair(Icons.Filled.ArrowBack, MaterialTheme.colorScheme.onSurface.copy(0.5f))
            entry.isDirectory -> Pair(Icons.Filled.Folder, Color(0xFFFFAB00))
            entry.name.endsWith(".jar") -> Pair(Icons.Filled.Archive, NovaPurple)
            entry.name.endsWith(".json") -> Pair(Icons.Filled.DataObject, NovaCyan)
            entry.name.endsWith(".png") || entry.name.endsWith(".jpg") -> Pair(Icons.Filled.Image, Color(0xFF69F0AE))
            entry.name.endsWith(".log") || entry.name.endsWith(".txt") -> Pair(Icons.Filled.Article, MaterialTheme.colorScheme.onSurface.copy(0.5f))
            entry.name.endsWith(".zip") -> Pair(Icons.Filled.FolderZip, Color(0xFFFF9800))
            else -> Pair(Icons.Filled.InsertDriveFile, MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!entry.isDirectory && entry.sizeBytes > 0) {
                Text(formatBytes(entry.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        }
        if (entry.lastModified > 0 && entry.displayName != "..") {
            Text(
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(entry.lastModified)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.3f)
            )
        }
        if (entry.isDirectory) {
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
        }
    }
}

// ─── File Action Bottom Sheet ─────────────────────────────────────────────────

@Composable
fun FileActionSheet(file: FileEntry, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = { Text(file.name, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Path: ${file.path}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                if (file.sizeBytes > 0) Text("Size: ${formatBytes(file.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        },
        confirmButton = {
            NovaButton(text = "Delete", onClick = onDelete, variant = ButtonVariant.DANGER)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
