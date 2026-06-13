package com.novacraft.launcher.ui.screens.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.novacraft.launcher.domain.model.AppSettings
import com.novacraft.launcher.domain.model.PerformancePreset
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var localSettings by remember(settings) { mutableStateOf(settings) }
    val isSaving by viewModel.isSaving.collectAsState()
    var hasChanges by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar      = { NovaCraftBottomBar(navController) },
        topBar         = {
            NovaCraftTopBar(
                title    = "Settings",
                subtitle = "NovaCraft Launcher v1.0.0",
                actions  = {
                    if (hasChanges) {
                        TextButton(
                            onClick = {
                                viewModel.updateSettings(localSettings)
                                hasChanges = false
                            }
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NovaCyan, strokeWidth = 2.dp)
                            } else {
                                Text("Save", color = NovaCyan, fontWeight = FontWeight.Bold)
                            }
                        }
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
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Java / Performance ──────────────────────────────────────
                item { SettingsSectionHeader("⚡ Performance") }

                item {
                    RamSliderSetting(
                        ramMb    = localSettings.defaultRamMb,
                        onChange = { localSettings = localSettings.copy(defaultRamMb = it); hasChanges = true }
                    )
                }

                item {
                    PerformancePresetSetting(
                        preset   = try { PerformancePreset.valueOf(localSettings.themeConfig.ifEmpty { "BALANCED" }) } catch(e: Exception) { PerformancePreset.BALANCED },
                        onSelect = { hasChanges = true }
                    )
                }

                item {
                    SettingsTextField(
                        label    = "Custom JVM Arguments",
                        value    = localSettings.defaultJvmArgs,
                        onValueChange = { localSettings = localSettings.copy(defaultJvmArgs = it); hasChanges = true },
                        placeholder  = "-XX:+UseG1GC -Xmx2G ...",
                        minLines = 3,
                        icon     = Icons.Outlined.Code
                    )
                }

                // ── Downloads ───────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("📥 Downloads") }

                item {
                    SliderSetting(
                        label    = "Download Threads",
                        value    = localSettings.downloadThreads.toFloat(),
                        range    = 1f..8f,
                        steps    = 6,
                        display  = { "${it.toInt()} threads" },
                        onValueChangeFinished = { localSettings = localSettings.copy(downloadThreads = it.toInt()); hasChanges = true },
                        icon     = Icons.Outlined.CloudDownload
                    )
                }

                item {
                    SettingsTextField(
                        label    = "Game Directory",
                        value    = localSettings.gameDir,
                        onValueChange = { localSettings = localSettings.copy(gameDir = it); hasChanges = true },
                        placeholder  = "/storage/emulated/0/NovaCraft/",
                        icon     = Icons.Outlined.Folder
                    )
                }

                // ── Appearance ──────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("🎨 Appearance") }

                item {
                    ThemeSetting(
                        currentTheme = localSettings.themeConfig,
                        onSelect     = { localSettings = localSettings.copy(themeConfig = it); hasChanges = true }
                    )
                }

                item {
                    ToggleSetting(
                        label    = "Show FPS Counter",
                        sublabel = "Display frames per second in-game",
                        checked  = localSettings.showFpsCounter,
                        onCheckedChange = { localSettings = localSettings.copy(showFpsCounter = it); hasChanges = true },
                        icon     = Icons.Outlined.Speed
                    )
                }

                item {
                    ToggleSetting(
                        label    = "Performance Overlay",
                        sublabel = "Show RAM, CPU, and ping in-game",
                        checked  = localSettings.showPerfOverlay,
                        onCheckedChange = { localSettings = localSettings.copy(showPerfOverlay = it); hasChanges = true },
                        icon     = Icons.Outlined.Monitor
                    )
                }

                // ── Controls ────────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("🎮 Controls") }

                item {
                    ToggleSetting(
                        label    = "Gyroscope Aiming",
                        sublabel = "Use device gyroscope for camera rotation",
                        checked  = localSettings.enableGyroAim,
                        onCheckedChange = { localSettings = localSettings.copy(enableGyroAim = it); hasChanges = true },
                        icon     = Icons.Outlined.Vibration
                    )
                }

                item {
                    ToggleSetting(
                        label    = "Haptic Feedback",
                        sublabel = "Vibrate on touch button presses",
                        checked  = localSettings.hapticFeedback,
                        onCheckedChange = { localSettings = localSettings.copy(hapticFeedback = it); hasChanges = true },
                        icon     = Icons.Outlined.TouchApp
                    )
                }

                // ── Launcher ────────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("🚀 Launcher") }

                item {
                    ToggleSetting(
                        label    = "Auto-Check Updates",
                        sublabel = "Check for launcher updates on start",
                        checked  = localSettings.autoCheckUpdates,
                        onCheckedChange = { localSettings = localSettings.copy(autoCheckUpdates = it); hasChanges = true },
                        icon     = Icons.Outlined.Update
                    )
                }

                item {
                    ToggleSetting(
                        label    = "Cloud Sync",
                        sublabel = "Sync settings and profiles across devices",
                        checked  = localSettings.cloudSyncEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(cloudSyncEnabled = it); hasChanges = true },
                        icon     = Icons.Outlined.Cloud
                    )
                }

                // ── Backup ──────────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("💾 Backup & Restore") }

                item {
                    ActionSetting(
                        label    = "Export Settings",
                        sublabel = "Save current settings to a file",
                        icon     = Icons.Outlined.Upload,
                        onClick  = { viewModel.exportSettings("/storage/emulated/0/NovaCraft/settings_backup.json") }
                    )
                }

                item {
                    ActionSetting(
                        label    = "Import Settings",
                        sublabel = "Load settings from a file",
                        icon     = Icons.Outlined.Download,
                        onClick  = { viewModel.importSettings("/storage/emulated/0/NovaCraft/settings_backup.json") }
                    )
                }

                item {
                    ActionSetting(
                        label    = "Reset to Defaults",
                        sublabel = "Restore all settings to their defaults",
                        icon     = Icons.Outlined.RestartAlt,
                        iconTint = ColorDanger,
                        onClick  = { viewModel.resetSettings() }
                    )
                }

                // ── About ────────────────────────────────────────────────────
                item { Spacer(Modifier.height(8.dp)); SettingsSectionHeader("ℹ️ About") }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("⛏️ NovaCraft Launcher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Java Minecraft. Anywhere.", style = MaterialTheme.typography.labelMedium, color = NovaCyan)
                            Spacer(Modifier.height(8.dp))
                            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            Text("Built with ❤️ for Android", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Settings Widgets ────────────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = NovaCyan,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun ToggleSetting(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor  = Color.White,
                    checkedTrackColor  = NovaCyan.copy(0.7f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(0.3f)
                )
            )
        }
    }
}

@Composable
fun RamSliderSetting(ramMb: Int, onChange: (Int) -> Unit) {
    var sliderValue by remember(ramMb) { mutableFloatStateOf(ramMb.toFloat()) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Memory, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("RAM Allocation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                Text(
                    "${sliderValue.toInt()} MB",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NovaCyan
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value    = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onChange(sliderValue.toInt()) },
                valueRange = 512f..8192f,
                steps  = 14,
                colors = SliderDefaults.colors(
                    thumbColor        = NovaCyan,
                    activeTrackColor  = NovaCyan,
                    inactiveTrackColor = NovaCyan.copy(0.2f)
                )
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("512 MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                Text("8 GB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }
        }
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
    icon: ImageVector
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                Text(display(sliderValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NovaCyan)
            }
            Slider(
                value    = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) },
                valueRange = range,
                steps  = steps,
                colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan, inactiveTrackColor = NovaCyan.copy(0.2f))
            )
        }
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 1,
    icon: ImageVector
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = { Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.35f)) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = minLines,
                textStyle     = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = NovaCyan,
                    unfocusedBorderColor = NovaGlassBorder,
                    focusedContainerColor  = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun ActionSetting(
    label: String,
    sublabel: String,
    icon: ImageVector,
    iconTint: Color = NovaCyan,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint.copy(0.8f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PerformancePresetSetting(preset: PerformancePreset, onSelect: (PerformancePreset) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Speed, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("Performance Preset", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformancePreset.entries.forEach { p ->
                    val isSelected = p == preset
                    val label = when (p) {
                        PerformancePreset.LOW_END  -> "Low"
                        PerformancePreset.BALANCED -> "Balanced"
                        PerformancePreset.HIGH_END -> "High"
                        PerformancePreset.CUSTOM   -> "Custom"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick  = { onSelect(p) },
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NovaCyan.copy(0.15f),
                            selectedLabelColor     = NovaCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSetting(currentTheme: String, onSelect: (String) -> Unit) {
    val themes = listOf(
        "DARK_NOVA" to "Dark Nova",
        "LIGHT_NOVA" to "Light Nova",
        "ABYSS"     to "Abyss"
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Palette, null, tint = NovaCyan.copy(0.8f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (id, label) ->
                    FilterChip(
                        selected = currentTheme == id,
                        onClick  = { onSelect(id) },
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NovaPurple.copy(0.2f),
                            selectedLabelColor     = NovaPurple
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
