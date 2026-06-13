package com.novacraft.launcher.ui.screens.mods

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.ModsViewModel

@Composable
fun ModsScreen(
    navController: NavController,
    viewModel: ModsViewModel = hiltViewModel()
) {
    val profiles        by viewModel.profiles.collectAsState()
    val installedMods   by viewModel.installedMods.collectAsState()
    val searchResults   by viewModel.searchResults.collectAsState()
    val isSearching     by viewModel.isSearching.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val selectedProfile by viewModel.selectedProfileId.collectAsState()
    val error           by viewModel.error.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }  // 0 = Installed, 1 = Browse
    var localQuery by remember { mutableStateOf("") }
    var selectedLoader by remember { mutableStateOf(LoaderType.FABRIC) }

    // Auto-select first profile
    LaunchedEffect(profiles) {
        if (selectedProfile == null && profiles.isNotEmpty()) {
            viewModel.selectProfile(profiles.first().id)
        }
    }

    Scaffold(
        bottomBar      = { NovaCraftBottomBar(navController) },
        topBar         = {
            NovaCraftTopBar(
                title    = "Mods",
                subtitle = "${installedMods.size} installed"
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
                // ── Tab Row ──────────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor   = Color.Transparent,
                    contentColor     = NovaCyan,
                    indicator        = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[activeTab])
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(NovaCyan, NovaPurple)),
                                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                )
                        )
                    }
                ) {
                    listOf("Installed", "Browse Modrinth").forEachIndexed { i, title ->
                        Tab(
                            selected = activeTab == i,
                            onClick  = { activeTab = i },
                            text     = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (activeTab == i) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // ── Profile selector ─────────────────────────────────────────
                if (profiles.isNotEmpty()) {
                    ProfileSelectorChips(
                        profiles       = profiles,
                        selectedId     = selectedProfile,
                        onSelectProfile = viewModel::selectProfile
                    )
                }

                // ── Tab content ───────────────────────────────────────────────
                when (activeTab) {
                    0 -> InstalledModsTab(
                        mods            = installedMods,
                        onToggle        = { id, enabled -> viewModel.toggleMod(id, enabled) },
                        onUninstall     = viewModel::uninstallMod
                    )
                    1 -> BrowseModsTab(
                        results         = searchResults,
                        isSearching     = isSearching,
                        installProgress = installProgress,
                        query           = localQuery,
                        onQueryChange   = { localQuery = it },
                        selectedLoader  = selectedLoader,
                        onLoaderChange  = { selectedLoader = it },
                        onSearch        = { viewModel.searchMods(localQuery, selectedLoader) },
                        onInstall       = viewModel::installMod
                    )
                }
            }
        }
    }
}

// ─── Profile Selector ────────────────────────────────────────────────────────

@Composable
fun ProfileSelectorChips(
    profiles: List<GameProfile>,
    selectedId: String?,
    onSelectProfile: (String) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(profiles) { profile ->
            FilterChip(
                selected = profile.id == selectedId,
                onClick  = { onSelectProfile(profile.id) },
                label    = { Text(profile.name, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Text(profile.iconEmoji, style = MaterialTheme.typography.bodySmall) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NovaPurple.copy(0.2f),
                    selectedLabelColor     = NovaPurple
                )
            )
        }
    }
}

// ─── Installed Mods ──────────────────────────────────────────────────────────

@Composable
fun InstalledModsTab(
    mods: List<Mod>,
    onToggle: (String, Boolean) -> Unit,
    onUninstall: (String) -> Unit
) {
    if (mods.isEmpty()) {
        EmptyState(
            icon     = Icons.Outlined.Extension,
            title    = "No mods installed",
            subtitle = "Browse Modrinth to find and install mods",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mods, key = { it.id }) { mod ->
                InstalledModCard(mod = mod, onToggle = { onToggle(mod.id, it) }, onUninstall = { onUninstall(mod.id) })
            }
        }
    }
}

@Composable
fun InstalledModCard(mod: Mod, onToggle: (Boolean) -> Unit, onUninstall: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (mod.iconUrl != null) {
                    AsyncImage(
                        model              = mod.iconUrl,
                        contentDescription = mod.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(Icons.Filled.Extension, null, tint = NovaPurple, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mod.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Text(mod.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }
            Switch(
                checked         = mod.isEnabled,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(checkedThumbColor = NovaCyan, checkedTrackColor = NovaCyan.copy(0.3f))
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onUninstall, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = ColorDanger.copy(0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Browse Mods ─────────────────────────────────────────────────────────────

@Composable
fun BrowseModsTab(
    results: List<Mod>,
    isSearching: Boolean,
    installProgress: Map<String, Float>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedLoader: LoaderType,
    onLoaderChange: (LoaderType) -> Unit,
    onSearch: () -> Unit,
    onInstall: (Mod) -> Unit
) {
    Column {
        // Search bar + loader row
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaSearchBar(
                    query         = query,
                    onQueryChange = onQueryChange,
                    placeholder   = "Search Modrinth...",
                    modifier      = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = onSearch,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(NovaBlue, NovaPurple)))
                ) {
                    Icon(Icons.Filled.Search, "Search", tint = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(LoaderType.FABRIC, LoaderType.FORGE, LoaderType.NEOFORGE, LoaderType.QUILT).forEach { loader ->
                    LoaderChip(
                        loaderType = loader,
                        modifier   = Modifier.clickable { onLoaderChange(loader) }
                            .then(if (loader == selectedLoader) Modifier.border(1.5.dp, when(loader) {
                                LoaderType.FABRIC   -> ColorFabric
                                LoaderType.FORGE    -> ColorForge
                                LoaderType.NEOFORGE -> ColorNeoForge
                                LoaderType.QUILT    -> ColorQuilt
                                else -> NovaCyan
                            }, RoundedCornerShape(6.dp)) else Modifier)
                    )
                }
            }
        }

        if (isSearching) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = NovaCyan)
            }
        } else if (results.isEmpty()) {
            EmptyState(
                icon     = Icons.Outlined.Search,
                title    = "Search Modrinth",
                subtitle = "Find mods by name, author, or category",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { mod ->
                    BrowseModCard(
                        mod      = mod,
                        progress = installProgress[mod.id],
                        onInstall = { onInstall(mod) }
                    )
                }
            }
        }
    }
}

@Composable
fun BrowseModCard(mod: Mod, progress: Float?, onInstall: () -> Unit) {
    val isInstalling = progress != null && progress < 1f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (mod.iconUrl != null) {
                    AsyncImage(
                        model              = mod.iconUrl,
                        contentDescription = mod.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Extension, null, tint = NovaPurple, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("by ${mod.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Spacer(Modifier.height(4.dp))
                Text(mod.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(formatDownloads(mod.downloads), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(formatDownloads(mod.follows), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))

            when {
                isInstalling -> CircularProgressIndicator(progress = { progress!! }, modifier = Modifier.size(36.dp), color = NovaCyan, strokeWidth = 3.dp)
                mod.isInstalled -> Icon(Icons.Filled.CheckCircle, "Installed", tint = ColorSuccess, modifier = Modifier.size(28.dp).align(Alignment.CenterVertically))
                else -> IconButton(
                    onClick  = onInstall,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(NovaBlue, NovaPurple))).align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Filled.Add, "Install", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun formatDownloads(n: Long): String = when {
    n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
    n >= 1_000     -> "${"%.1f".format(n / 1_000.0)}K"
    else           -> n.toString()
}
