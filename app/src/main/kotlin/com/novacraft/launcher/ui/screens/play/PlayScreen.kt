package com.novacraft.launcher.ui.screens.play

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.*

@Composable
fun PlayScreen(
    navController: NavController,
    viewModel: PlayViewModel = hiltViewModel()
) {
    val profiles         by viewModel.profiles.collectAsState()
    val installedVersions by viewModel.installedVersions.collectAsState()
    val selectedId       by viewModel.selectedProfileId.collectAsState()
    val launchState      by viewModel.launchState.collectAsState()
    val context          = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    // Select first profile by default
    LaunchedEffect(profiles) {
        if (selectedId == null && profiles.isNotEmpty()) {
            viewModel.selectProfile(profiles.first().id)
        }
    }

    Scaffold(
        bottomBar = { NovaCraftBottomBar(navController) },
        topBar = {
            NovaCraftTopBar(
                title    = "Play",
                subtitle = "${profiles.size} profiles",
                actions  = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, "New Profile", tint = NovaCyan)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedId != null) {
                NovaPlayFab(
                    onClick    = { viewModel.launch(context) },
                    isLaunching = launchState is LaunchState.Launching
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1A2E))))
                .padding(padding)
        ) {
            if (profiles.isEmpty()) {
                EmptyState(
                    icon       = Icons.Outlined.PlayCircle,
                    title      = "No profiles yet",
                    subtitle   = "Create a launch profile to start playing",
                    actionLabel = "Create Profile",
                    onAction   = { showCreateDialog = true },
                    modifier   = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Launch status banner
                    item {
                        AnimatedVisibility(launchState !is LaunchState.Idle) {
                            LaunchStatusBanner(state = launchState, onDismiss = viewModel::clearLaunchState)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile    = profile,
                            isSelected = profile.id == selectedId,
                            onSelect   = { viewModel.selectProfile(profile.id) },
                            onDelete   = { showDeleteConfirm = profile.id }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }

        // ── Create Profile Dialog ────────────────────────────────────────────
        if (showCreateDialog) {
            CreateProfileDialog(
                availableVersions = installedVersions,
                onConfirm = { name, versionId ->
                    viewModel.createProfile(name, versionId)
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false }
            )
        }

        // ── Delete Confirm Dialog ─────────────────────────────────────────────
        showDeleteConfirm?.let { id ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete Profile") },
                text  = { Text("Are you sure you want to delete this profile? Game files will not be removed.") },
                confirmButton = {
                    NovaButton(
                        text = "Delete",
                        onClick = {
                            viewModel.deleteProfile(id)
                            showDeleteConfirm = null
                        },
                        variant = ButtonVariant.DANGER
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// ─── Profile Card ─────────────────────────────────────────────────────────────

@Composable
fun ProfileCard(
    profile: GameProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) NovaCyan else NovaGlassBorder
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected)
                    Brush.linearGradient(listOf(NovaPurple.copy(0.12f), NovaBlue.copy(0.08f)))
                else
                    Brush.linearGradient(listOf(Color.White.copy(0.06f), Color.White.copy(0.02f)))
            )
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(NovaPurple.copy(0.25f), NovaBlue.copy(0.25f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.iconEmoji, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text  = profile.versionId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = NovaCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileStat(Icons.Outlined.Memory, "${profile.ramMb} MB RAM")
                ProfileStat(Icons.Outlined.Speed, profile.performancePreset.name.replace("_", " "))
                if (profile.isFavorite) {
                    ProfileStat(Icons.Filled.Star, "Favorite", tint = Color(0xFFFFD700))
                }
            }
        }
    }
}

@Composable
fun ProfileStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ─── Launch Status Banner ─────────────────────────────────────────────────────

@Composable
fun LaunchStatusBanner(state: LaunchState, onDismiss: () -> Unit) {
    val (color, icon, message) = when (state) {
        is LaunchState.Launching -> Triple(NovaCyan,    Icons.Filled.Rocket,     state.message)
        is LaunchState.Running   -> Triple(ColorSuccess, Icons.Filled.CheckCircle, "Game is running")
        is LaunchState.Error     -> Triple(ColorDanger,  Icons.Filled.Error,       state.message)
        else                     -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state is LaunchState.Launching) {
            CircularProgressIndicator(color = color, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.weight(1f))
        if (state is LaunchState.Error) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, "Dismiss", tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Create Profile Dialog ────────────────────────────────────────────────────

@Composable
fun CreateProfileDialog(
    availableVersions: List<GameVersion>,
    onConfirm: (name: String, versionId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var versionId by remember { mutableStateOf(availableVersions.firstOrNull()?.id ?: "") }
    var expanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text("Create Profile", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Profile Name") },
                    placeholder   = { Text("My Survival World") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaCyan,
                        focusedLabelColor  = NovaCyan
                    )
                )

                if (availableVersions.isEmpty()) {
                    Text(
                        "No versions installed. Go to the Versions tab to install one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded         = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value         = versionId.ifEmpty { "Select version" },
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Minecraft Version") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NovaCyan,
                                focusedLabelColor  = NovaCyan
                            )
                        )
                        ExposedDropdownMenu(
                            expanded         = expanded,
                            onDismissRequest = { expanded = false },
                            modifier         = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            availableVersions.forEach { v ->
                                DropdownMenuItem(
                                    text   = { Text(v.id) },
                                    onClick = { versionId = v.id; expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            NovaButton(
                text    = "Create",
                onClick = { if (name.isNotBlank() && versionId.isNotBlank()) onConfirm(name, versionId) },
                enabled = name.isNotBlank() && versionId.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
