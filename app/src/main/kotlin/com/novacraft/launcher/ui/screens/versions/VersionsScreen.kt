package com.novacraft.launcher.ui.screens.versions

import androidx.compose.animation.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.VersionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VersionsScreen(
    navController: NavController,
    viewModel: VersionsViewModel = hiltViewModel()
) {
    val filteredVersions  by viewModel.filteredVersions.collectAsState()
    val installedVersions by viewModel.installedVersions.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val installProgress   by viewModel.installProgress.collectAsState()
    val error             by viewModel.error.collectAsState()
    val selectedLoader    by viewModel.selectedLoader.collectAsState()
    val selectedRelease   by viewModel.selectedReleaseType.collectAsState()
    val searchQuery       by viewModel.searchQuery.collectAsState()

    var showInstalledOnly by remember { mutableStateOf(false) }

    val displayVersions = if (showInstalledOnly) installedVersions else filteredVersions

    Scaffold(
        bottomBar  = { NovaCraftBottomBar(navController) },
        topBar     = {
            NovaCraftTopBar(
                title    = "Versions",
                subtitle = "${installedVersions.size} installed",
                actions  = {
                    IconButton(onClick = { viewModel.fetchVersions() }) {
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
                // ── Search + Filters ─────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    NovaSearchBar(
                        query         = searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                        placeholder   = "Search versions..."
                    )
                    Spacer(Modifier.height(10.dp))
                    LoaderFilterRow(
                        selectedLoader  = selectedLoader,
                        onSelectLoader  = viewModel::setLoader
                    )
                    Spacer(Modifier.height(8.dp))
                    ReleaseTypeRow(
                        selectedType  = selectedRelease,
                        onSelectType  = viewModel::setReleaseType,
                        showInstalled = showInstalledOnly,
                        onToggleInstalled = { showInstalledOnly = !showInstalledOnly }
                    )
                }

                // ── Error banner ────────────────────────────────────────────
                AnimatedVisibility(error != null) {
                    error?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorDanger.copy(0.12f))
                                .border(1.dp, ColorDanger.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = ColorDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = ColorDanger, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp), tint = ColorDanger)
                            }
                        }
                    }
                }

                // ── Content ──────────────────────────────────────────────────
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NovaCyan)
                            Spacer(Modifier.height(12.dp))
                            Text("Fetching version list...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else if (displayVersions.isEmpty()) {
                    EmptyState(
                        icon       = Icons.Outlined.Layers,
                        title      = if (showInstalledOnly) "No versions installed" else "No versions found",
                        subtitle   = if (showInstalledOnly) "Install a version to start playing" else "Try adjusting your filters",
                        modifier   = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayVersions, key = { it.id }) { version ->
                            VersionCard(
                                version         = version,
                                installProgress = installProgress[version.id],
                                onInstall       = { viewModel.installVersion(version) },
                                onUninstall     = { viewModel.uninstallVersion(version.id) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ─── Loader Filter Row ────────────────────────────────────────────────────────

@Composable
fun LoaderFilterRow(selectedLoader: LoaderType, onSelectLoader: (LoaderType) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex    = LoaderType.entries.indexOf(selectedLoader),
        containerColor      = Color.Transparent,
        contentColor        = NovaCyan,
        edgePadding         = 0.dp,
        divider             = {}
    ) {
        LoaderType.entries.forEach { loader ->
            val isSelected = loader == selectedLoader
            val (color, label) = when (loader) {
                LoaderType.VANILLA  -> Pair(ColorVanilla, "Vanilla")
                LoaderType.FABRIC   -> Pair(ColorFabric, "Fabric")
                LoaderType.FORGE    -> Pair(ColorForge, "Forge")
                LoaderType.NEOFORGE -> Pair(ColorNeoForge, "NeoForge")
                LoaderType.QUILT    -> Pair(ColorQuilt, "Quilt")
            }
            Tab(
                selected = isSelected,
                onClick  = { onSelectLoader(loader) },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) color.copy(0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    text     = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─── Release Type Row ────────────────────────────────────────────────────────

@Composable
fun ReleaseTypeRow(
    selectedType: ReleaseType?,
    onSelectType: (ReleaseType?) -> Unit,
    showInstalled: Boolean,
    onToggleInstalled: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        listOf<Pair<ReleaseType?, String>>(
            null to "All",
            ReleaseType.RELEASE  to "Release",
            ReleaseType.SNAPSHOT to "Snapshot",
            ReleaseType.BETA     to "Beta"
        ).forEach { (type, label) ->
            val isSelected = selectedType == type
            FilterChip(
                selected = isSelected,
                onClick  = { onSelectType(type) },
                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NovaPurple.copy(0.2f),
                    selectedLabelColor     = NovaPurple
                )
            )
        }

        Spacer(Modifier.weight(1f))

        FilterChip(
            selected = showInstalled,
            onClick  = onToggleInstalled,
            label    = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Installed", style = MaterialTheme.typography.labelSmall)
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ColorSuccess.copy(0.15f),
                selectedLabelColor     = ColorSuccess
            )
        )
    }
}

// ─── Version Card ────────────────────────────────────────────────────────────

@Composable
fun VersionCard(
    version: GameVersion,
    installProgress: Float?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val isInstalling = installProgress != null && installProgress < 1f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Version icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            when (version.loaderType) {
                                LoaderType.VANILLA  -> Brush.linearGradient(listOf(ColorVanilla.copy(0.2f), ColorVanilla.copy(0.1f)))
                                LoaderType.FABRIC   -> Brush.linearGradient(listOf(ColorFabric.copy(0.2f), ColorFabric.copy(0.1f)))
                                LoaderType.FORGE    -> Brush.linearGradient(listOf(ColorForge.copy(0.2f), ColorForge.copy(0.1f)))
                                LoaderType.NEOFORGE -> Brush.linearGradient(listOf(ColorNeoForge.copy(0.2f), ColorNeoForge.copy(0.1f)))
                                LoaderType.QUILT    -> Brush.linearGradient(listOf(ColorQuilt.copy(0.2f), ColorQuilt.copy(0.1f)))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (version.loaderType) {
                            LoaderType.VANILLA  -> "🎮"
                            LoaderType.FABRIC   -> "🧵"
                            LoaderType.FORGE    -> "⚒️"
                            LoaderType.NEOFORGE -> "🔥"
                            LoaderType.QUILT    -> "🪡"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text  = version.minecraftVersion,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (version.releaseType == ReleaseType.SNAPSHOT) {
                            Surface(shape = RoundedCornerShape(4.dp), color = ColorWarning.copy(0.15f)) {
                                Text("SNAP", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = ColorWarning)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LoaderChip(loaderType = version.loaderType)
                        version.loaderVersion?.let { lv ->
                            Text(lv, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                        }
                    }
                    Text(
                        text  = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(version.releaseDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    )
                }

                // Action
                when {
                    isInstalling -> {
                        CircularProgressIndicator(
                            progress = { installProgress!! },
                            modifier = Modifier.size(36.dp),
                            color    = NovaCyan,
                            strokeWidth = 3.dp
                        )
                    }
                    version.isInstalled -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ColorSuccess.copy(0.15f),
                                border = BorderStroke(1.dp, ColorSuccess.copy(0.3f))
                            ) {
                                Text(
                                    "Installed",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorSuccess
                                )
                            }
                            IconButton(onClick = onUninstall, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.DeleteOutline, null, tint = ColorDanger.copy(0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    else -> {
                        IconButton(
                            onClick  = onInstall,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NovaBlue, NovaPurple)))
                        ) {
                            Icon(Icons.Filled.Download, "Install", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Progress bar during install
            if (isInstalling) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { installProgress!! },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color    = NovaCyan,
                    trackColor = NovaCyan.copy(0.2f)
                )
                Text(
                    "Installing... ${(installProgress!! * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaCyan.copy(0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
