package com.novacraft.launcher.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.navigation.Routes
import com.novacraft.launcher.ui.theme.*

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

data class BottomNavEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavEntry(Routes.HOME,     "Home",     Icons.Outlined.Home,        Icons.Filled.Home),
    BottomNavEntry(Routes.PLAY,     "Play",     Icons.Outlined.PlayCircle,  Icons.Filled.PlayCircle),
    BottomNavEntry(Routes.VERSIONS, "Versions", Icons.Outlined.Layers,      Icons.Filled.Layers),
    BottomNavEntry(Routes.MODS,     "Mods",     Icons.Outlined.Extension,   Icons.Filled.Extension),
    BottomNavEntry(Routes.FILES,    "Files",    Icons.Outlined.Folder,      Icons.Filled.Folder),
    BottomNavEntry(Routes.ACCOUNTS, "Accounts", Icons.Outlined.Person,      Icons.Filled.Person),
    BottomNavEntry(Routes.SETTINGS, "Settings", Icons.Outlined.Settings,    Icons.Filled.Settings),
)

@Composable
fun NovaCraftBottomBar(navController: NavController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = NovaGlassBorder,
                    start = Offset(0f, 0f),
                    end   = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick  = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text  = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = NovaCyan,
                    selectedTextColor   = NovaCyan,
                    indicatorColor      = NovaPurple.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            )
        }
    }
}

// ─── Glass Card ──────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = NovaPurple,
    showGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.03f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
        .then(
            if (showGlow) Modifier.drawBehind {
                drawCircle(
                    color  = glowColor.copy(alpha = 0.15f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width / 2, size.height)
                )
            } else Modifier
        )

    if (onClick != null) {
        Box(
            modifier = baseModifier.clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Box(modifier = baseModifier) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// ─── Gradient Button ─────────────────────────────────────────────────────────

@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    variant: ButtonVariant = ButtonVariant.PRIMARY
) {
    val gradientBrush = when (variant) {
        ButtonVariant.PRIMARY -> Brush.horizontalGradient(
            listOf(NovaBlue, NovaPurple)
        )
        ButtonVariant.DANGER -> Brush.horizontalGradient(
            listOf(Color(0xFFFF1744), Color(0xFFFF6B6B))
        )
        ButtonVariant.SUCCESS -> Brush.horizontalGradient(
            listOf(Color(0xFF00C853), Color(0xFF69F0AE))
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .height(52.dp)
            .then(
                if (enabled) Modifier.background(gradientBrush, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor   = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color    = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

enum class ButtonVariant { PRIMARY, DANGER, SUCCESS }

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text  = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = NovaCyan
                )
            }
        }
    }
}

// ─── Loader Type Chip ─────────────────────────────────────────────────────────

@Composable
fun LoaderChip(loaderType: LoaderType, modifier: Modifier = Modifier) {
    val (color, label) = when (loaderType) {
        LoaderType.VANILLA   -> Pair(ColorVanilla,  "Vanilla")
        LoaderType.FABRIC    -> Pair(ColorFabric,   "Fabric")
        LoaderType.FORGE     -> Pair(ColorForge,    "Forge")
        LoaderType.NEOFORGE  -> Pair(ColorNeoForge, "NeoForge")
        LoaderType.QUILT     -> Pair(ColorQuilt,    "Quilt")
    }
    Surface(
        modifier  = modifier,
        shape     = RoundedCornerShape(6.dp),
        color     = color.copy(alpha = 0.18f),
        border    = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Download Progress Item ───────────────────────────────────────────────────

@Composable
fun DownloadProgressItem(task: DownloadTask, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = NovaCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = task.name,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color         = NovaCyan,
                    trackColor    = NovaCyan.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${(task.progress * 100).toInt()}% · ${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text  = task.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (task.status) {
                    DownloadStatus.DOWNLOADING -> NovaCyan
                    DownloadStatus.COMPLETED   -> ColorSuccess
                    DownloadStatus.FAILED      -> ColorDanger
                    DownloadStatus.PAUSED      -> ColorWarning
                    else                       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        }
    }
}

// ─── Account Avatar ──────────────────────────────────────────────────────────

@Composable
fun AccountAvatar(
    account: Account,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(listOf(NovaPurple, NovaBlue))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = account.username.take(2).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier           = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            NovaButton(text = actionLabel, onClick = onAction)
        }
    }
}

// ─── Top App Bar ─────────────────────────────────────────────────────────────

@Composable
fun NovaCraftTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaCyan.copy(alpha = 0.8f)
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// ─── Stat Card ───────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = NovaCyan,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text  = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ─── Nova Search Bar ─────────────────────────────────────────────────────────

@Composable
fun NovaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value           = query,
        onValueChange   = onQueryChange,
        placeholder     = { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
        leadingIcon     = { Icon(Icons.Outlined.Search, null, tint = NovaCyan) },
        trailingIcon    = if (query.isNotEmpty()) {{
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Clear, null, modifier = Modifier.size(18.dp))
            }
        }} else null,
        singleLine      = true,
        shape           = RoundedCornerShape(14.dp),
        modifier        = modifier.fillMaxWidth(),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = NovaCyan,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor  = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ─── Animated Play FAB ────────────────────────────────────────────────────────

@Composable
fun NovaPlayFab(
    onClick: () -> Unit,
    isLaunching: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    FloatingActionButton(
        onClick      = onClick,
        modifier     = modifier.size(64.dp),
        containerColor = Color.Transparent,
        contentColor = Color.White,
        elevation    = FloatingActionButtonDefaults.elevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(NovaBlue, NovaPurple)),
                    CircleShape
                )
                .then(
                    if (!isLaunching) Modifier else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLaunching) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(28.dp),
                    color       = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// ─── Utils ────────────────────────────────────────────────────────────────────

fun formatBytes(bytes: Long): String = when {
    bytes < 1024       -> "$bytes B"
    bytes < 1048576    -> "${bytes / 1024} KB"
    bytes < 1073741824 -> "${"%.1f".format(bytes / 1048576.0)} MB"
    else               -> "${"%.2f".format(bytes / 1073741824.0)} GB"
}

fun formatPlaytime(ms: Long): String {
    val hours   = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
